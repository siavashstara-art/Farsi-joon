package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudSyncRepository
import com.example.data.firebase.PersianChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ChatMessageItem(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
  val isReported: Boolean = false,
  val userPromptForReport: String = ""
)

sealed interface ChatUiError {
  data class Notice(val message: String) : ChatUiError
}

data class ChatUiState(
  val isLoadingQuota: Boolean = true,
  val dailyUsed: Int = 0,
  val dailyLimit: Int = PersianChatRepository.DAILY_LIMIT,
  val messages: List<ChatMessageItem> = emptyList(),
  val isSending: Boolean = false,
  val errorMessage: String? = null,
  val statusBanner: String? = null
) {
  val remainingMessages: Int get() = (dailyLimit - dailyUsed).coerceAtLeast(0)
  val isLimitReached: Boolean get() = dailyUsed >= dailyLimit
}

class ChatViewModel(
  private val currentUserId: String,
  private val displayName: String,
  private val cloudSyncRepo: CloudSyncRepository,
  private val chatRepo: PersianChatRepository = PersianChatRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    ChatUiState(
      messages = listOf(
        ChatMessageItem(
          isUser = false,
          text = "سلام $displayName جان! (Salâm $displayName jân!)\nمن فارسیجون هستم. امروز با هم فارسی تمرین کنیم؟ چطوری؟\nI'm Farsi Joon. Shall we practice Persian together today? How are you?"
        )
      )
    )
  )
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private val todayDateKey: String
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

  init {
    loadDailyQuota()
  }

  fun loadDailyQuota() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoadingQuota = true, errorMessage = null)
      try {
        val count = cloudSyncRepo.getDailyChatCount(todayDateKey)
        _uiState.value = _uiState.value.copy(
          isLoadingQuota = false,
          dailyUsed = count
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("ChatVM", "Failed to load daily quota", e)
        _uiState.value = _uiState.value.copy(
          isLoadingQuota = false,
          errorMessage = e.localizedMessage ?: "Could not load daily chat quota."
        )
      }
    }
  }

  fun sendMessage(rawText: String) {
    val userText = rawText.trim()
    val snapshot = _uiState.value
    if (userText.isEmpty() || snapshot.isSending || snapshot.isLoadingQuota) return

    if (snapshot.isLimitReached) {
      _uiState.value = snapshot.copy(
        errorMessage = "چت امروز تعطیل است — Daily limit of 10 messages reached."
      )
      return
    }

    if (!chatRepo.checkMinuteRateLimit()) {
      _uiState.value = snapshot.copy(
        errorMessage = "Please wait a few seconds before sending another message (minute rate limit)."
      )
      return
    }

    val historyPairs = snapshot.messages
      .filter { !it.isStreaming }
      .map { (if (it.isUser) "user" else "model") to it.text }

    val userMsg = ChatMessageItem(isUser = true, text = userText)
    val aiPlaceholderId = UUID.randomUUID().toString()
    val aiPlaceholder = ChatMessageItem(
      id = aiPlaceholderId,
      isUser = false,
      text = "...",
      isStreaming = true,
      userPromptForReport = userText
    )

    _uiState.value = snapshot.copy(
      messages = snapshot.messages + userMsg + aiPlaceholder,
      isSending = true,
      errorMessage = null,
      statusBanner = null
    )

    viewModelScope.launch {
      try {
        val updatedCount = cloudSyncRepo.incrementDailyChatCount(todayDateKey)
        chatRepo.recordMinuteSend()
        _uiState.value = _uiState.value.copy(dailyUsed = updatedCount)

        var latestText = ""
        chatRepo.streamReply(historyPairs, userText).collect { partial ->
          latestText = partial
          _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.map { item ->
              if (item.id == aiPlaceholderId) item.copy(text = partial, isStreaming = true) else item
            }
          )
        }

        _uiState.value = _uiState.value.copy(
          isSending = false,
          messages = _uiState.value.messages.map { item ->
            if (item.id == aiPlaceholderId) item.copy(text = latestText, isStreaming = false) else item
          }
        )
      } catch (e: CloudSyncRepository.DailyQuotaExceededException) {
        _uiState.value = _uiState.value.copy(
          isSending = false,
          dailyUsed = _uiState.value.dailyLimit,
          messages = _uiState.value.messages.filterNot { it.id == aiPlaceholderId || it.id == userMsg.id },
          errorMessage = e.message
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("ChatVM", "Chat send failed", e)
        _uiState.value = _uiState.value.copy(
          isSending = false,
          messages = _uiState.value.messages.filterNot { it.id == aiPlaceholderId },
          errorMessage = e.localizedMessage ?: "Failed to reach AI tutor. Please try again."
        )
      }
    }
  }

  fun reportMessage(messageId: String, reason: String) {
    val target = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
    viewModelScope.launch {
      try {
        cloudSyncRepo.submitChatReport(
          promptText = target.userPromptForReport,
          responseText = target.text,
          reason = reason
        )
        _uiState.value = _uiState.value.copy(
          messages = _uiState.value.messages.map {
            if (it.id == messageId) it.copy(isReported = true) else it
          },
          statusBanner = "Report saved. Thank you for helping keep Farsi Joon safe."
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          errorMessage = e.localizedMessage ?: "Could not submit report."
        )
      }
    }
  }

  fun dismissError() {
    _uiState.value = _uiState.value.copy(errorMessage = null, statusBanner = null)
  }
}
