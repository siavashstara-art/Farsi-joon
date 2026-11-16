package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class PersianChatRepository {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content {
        text(
          """
          You are 'Farsi-Joon' (فارسی‌جون), a warm, encouraging Persian conversation tutor for 2nd and 3rd generation Iranian diaspora learners (ages 13+).
          Rules:
          1. Keep replies short, natural, and conversational (2-4 sentences max).
          2. Prioritize friendly spoken Tehran/colloquial Persian first (e.g., خوبم، چطوری، میرم).
          3. Format EVERY Persian reply cleanly so the learner sees all three forms:
             - Persian script first
             - Finglish pronunciation in parentheses using clean phonetic spelling (â, kh, sh, gh, ch)
             - Clear English translation on the next line
          4. Gently correct mistakes with warmth ("آفرین! (âfarin!) Great job! You can also say...").
          5. Stay PG-13, family-friendly, culturally respectful, and focused on Persian language and culture.
          """.trimIndent()
        )
      }
    )
  }

  private val recentTimestamps = ArrayDeque<Long>()

  fun checkMinuteRateLimit(nowMillis: Long = System.currentTimeMillis()): Boolean {
    while (recentTimestamps.isNotEmpty() && nowMillis - recentTimestamps.first() > 60_000L) {
      recentTimestamps.removeFirst()
    }
    return recentTimestamps.size < MAX_PER_MINUTE
  }

  fun recordMinuteSend(nowMillis: Long = System.currentTimeMillis()) {
    recentTimestamps.addLast(nowMillis)
  }

  fun streamReply(
    historyMessages: List<Pair<String, String>>,
    userMessage: String
  ): Flow<String> = flow {
    val chat = model.startChat(
      history = historyMessages.takeLast(10).map { (role, text) ->
        content(role) { text(text) }
      }
    )
    val accumulated = StringBuilder()
    try {
      withTimeout(60_000L) {
        chat.sendMessageStream(userMessage).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            accumulated.append(part)
            emit(accumulated.toString())
          }
        }
      }
      if (accumulated.isBlank()) {
        throw IllegalStateException("AI tutor returned an empty response.")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Chat streaming timed out", e)
      throw IllegalStateException("Response timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Chat streaming failed", e)
      throw e
    }
  }

  companion object {
    private const val TAG = "PersianChatRepo"
    const val DAILY_LIMIT = 10
    const val MAX_PER_MINUTE = 4
  }
}
