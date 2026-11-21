package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.CloudSyncRepository
import com.example.ui.components.FarsiText
import com.example.ui.components.GoogleSignInButton
import com.example.viewmodel.ChatMessageItem
import com.example.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ChatTabScreen(
  currentUser: FirebaseUser?,
  onSpeak: (String, String) -> Unit,
  onSignedInSync: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (currentUser == null) {
    ChatSignInGate(
      onAuthSuccess = onSignedInSync,
      modifier = modifier
    )
  } else {
    val uid = currentUser.uid
    val displayName = currentUser.displayName?.split(" ")?.firstOrNull().orEmpty().ifBlank { "Friend" }
    androidx.compose.runtime.key(uid) {
      val chatViewModel: ChatViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val dbId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            ChatViewModel(
              currentUserId = uid,
              displayName = displayName,
              cloudSyncRepo = CloudSyncRepository(db)
            )
          }
        }
      )
      ChatAuthenticatedScreen(
        viewModel = chatViewModel,
        onSpeak = onSpeak,
        modifier = modifier
      )
    }
  }
}

@Composable
private fun ChatSignInGate(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier.widthIn(max = 500.dp),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.size(64.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FarsiText(text = "چت فارسی با هوش مصنوعی", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Sign in to chat with Farsi Joon",
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Lessons, Leitner Review, and Stories are 100% offline without an account. Sign in with Google to unlock 10 free AI Persian practice chats per day and cloud progress backup.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        GoogleSignInButton(
          onAuthSuccess = {
            errorMsg = null
            onAuthSuccess()
          },
          onAuthError = { errorMsg = it },
          label = "Continue with Google / Gmail",
          modifier = Modifier.fillMaxWidth()
        )
        if (errorMsg != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = errorMsg.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun ChatAuthenticatedScreen(
  viewModel: ChatViewModel,
  onSpeak: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var inputText by remember { mutableStateOf("") }
  var reportTarget by remember { mutableStateOf<ChatMessageItem?>(null) }
  var reportReason by remember { mutableStateOf("Off-topic or inaccurate Persian") }
  val listState = rememberLazyListState()

  val starterPrompts = listOf(
    "سلام! چطوری؟" to "Salâm! Chetori?",
    "می‌خوام دربارهٔ خانواده‌ام حرف بزنم" to "Talk about family",
    "تعارف چطوری کار می‌کنه؟" to "How does ta'ârof work?"
  )

  val readyPhrasesAfterLimit = listOf(
    Triple("سلام عزیز دلم، حالتون چطوره؟", "salâm aziz-e delam, hâletun chetore?", "Hello dear, how are you?"),
    Triple("دستت درد نکنه، خیلی خوشمزه بود!", "dastet dard nakone, kheli khoshmaze bud!", "Thank you, it was delicious!"),
    Triple("دلم برات خیلی تنگ شده!", "delam barât kheli tang shode!", "I miss you so much!")
  )

  LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.lastIndex)
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Gemini Flash Tutor",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )
        Surface(
          color = if (uiState.isLimitReached) MaterialTheme.colorScheme.tertiaryContainer
          else MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "${uiState.remainingMessages} / ${uiState.dailyLimit} left today",
            style = MaterialTheme.typography.labelMedium,
            color = if (uiState.isLimitReached) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
    }

    if (uiState.errorMessage != null || uiState.statusBanner != null) {
      Surface(
        color = if (uiState.errorMessage != null) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = uiState.errorMessage ?: uiState.statusBanner.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = { viewModel.dismissError() }) {
            Text("OK")
          }
        }
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .widthIn(max = 600.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(uiState.messages, key = { it.id }) { msg ->
        ChatBubble(
          item = msg,
          onSpeak = { onSpeak(extractPersianForSpeech(msg.text), "STANDARD") },
          onReport = { reportTarget = msg }
        )
      }

      if (uiState.isLimitReached) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              FarsiText(
                text = "چت امروز تعطیل است!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Text(
                text = "You used all 10 daily messages. Practice these ready-made phrases while your quota resets tomorrow:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Spacer(modifier = Modifier.height(10.dp))
              readyPhrasesAfterLimit.forEach { (fa, fi, en) ->
                Surface(
                  onClick = { onSpeak(fa, "STANDARD") },
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      FarsiText(text = fa, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                      Text(text = "$fi — $en", style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen")
                  }
                }
              }
            }
          }
        }
      }
    }

    if (!uiState.isLimitReached) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(starterPrompts) { (faPrompt, label) ->
          SuggestionChip(
            enabled = !uiState.isSending && !uiState.isLoadingQuota,
            onClick = { viewModel.sendMessage(faPrompt) },
            label = { Text(label) }
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Type in Finglish, Persian, or English...") },
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field"),
          shape = RoundedCornerShape(20.dp),
          maxLines = 3
        )
        FilledIconButton(
          onClick = {
            val toSend = inputText
            inputText = ""
            viewModel.sendMessage(toSend)
          },
          enabled = inputText.isNotBlank() && !uiState.isSending && !uiState.isLoadingQuota,
          modifier = Modifier
            .size(52.dp)
            .testTag("chat_send_button")
        ) {
          if (uiState.isSending) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
          } else {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
          }
        }
      }
    }
  }

  if (reportTarget != null) {
    AlertDialog(
      onDismissRequest = { reportTarget = null },
      title = { Text("Report AI Response") },
      text = {
        Column {
          Text("Help us improve safety and Persian accuracy. Why are you reporting this message?")
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = reportReason,
            onValueChange = { reportReason = it },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          val target = reportTarget
          if (target != null) {
            viewModel.reportMessage(target.id, reportReason)
          }
          reportTarget = null
        }) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { reportTarget = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ChatBubble(
  item: ChatMessageItem,
  onSpeak: () -> Unit,
  onReport: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (item.isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = if (item.isUser) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(18.dp),
      tonalElevation = if (item.isUser) 0.dp else 2.dp,
      modifier = Modifier.widthIn(max = 320.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = item.text,
          style = MaterialTheme.typography.bodyLarge,
          color = if (item.isUser) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurface
        )
        if (!item.isUser && !item.isStreaming) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onSpeak, modifier = Modifier.size(48.dp)) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Listen to message",
                modifier = Modifier.size(20.dp)
              )
            }
            if (!item.isReported) {
              TextButton(
                onClick = onReport,
                modifier = Modifier.testTag("report_message_${item.id}")
              ) {
                Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Report", style = MaterialTheme.typography.labelMedium)
              }
            } else {
              Text(
                text = "Reported",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

private fun extractPersianForSpeech(text: String): String {
  val persianLines = text.lineSequence()
    .map { line ->
      line.replace(Regex("""\([A-Za-z0-9\s.,!?;:'"âêîôûÂÊÎÔÛ\-/]+\)"""), "").trim()
    }
    .filter { line -> line.any { ch -> ch in '\u0600'..'\u06FF' } }
    .joinToString(". ")
  return persianLines.ifBlank { text.lineSequence().firstOrNull().orEmpty() }
}
