package com.example.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class VoiceProfile {
  STANDARD,
  NIMA,
  GRANDMA
}

class PersianAudioPlayer(context: Context) {

  private var tts: TextToSpeech? = null
  private var isReady = false
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _persianVoiceAvailable = MutableStateFlow(true)
  val persianVoiceAvailable: StateFlow<Boolean> = _persianVoiceAvailable.asStateFlow()

  private val _activeUtteranceId = MutableStateFlow<String?>(null)
  val activeUtteranceId: StateFlow<String?> = _activeUtteranceId.asStateFlow()

  private var onUtteranceDoneCallback: ((String) -> Unit)? = null

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val engine = tts ?: return@TextToSpeech
        val faIr = Locale.forLanguageTag("fa-IR")
        val fa = Locale.forLanguageTag("fa")
        val resIr = engine.setLanguage(faIr)
        if (resIr == TextToSpeech.LANG_MISSING_DATA || resIr == TextToSpeech.LANG_NOT_SUPPORTED) {
          val resFa = engine.setLanguage(fa)
          _persianVoiceAvailable.value =
            resFa != TextToSpeech.LANG_MISSING_DATA && resFa != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
          _persianVoiceAvailable.value = true
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
            _activeUtteranceId.value = utteranceId
          }

          override fun onDone(utteranceId: String?) {
            _isSpeaking.value = false
            _activeUtteranceId.value = null
            if (utteranceId != null) {
              mainHandler.post {
                onUtteranceDoneCallback?.invoke(utteranceId)
              }
            }
          }

          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            _isSpeaking.value = false
            _activeUtteranceId.value = null
          }
        })
        isReady = true
      } else {
        isReady = false
        _persianVoiceAvailable.value = false
      }
    }
  }

  fun speak(
    farsiText: String,
    voiceProfile: VoiceProfile = VoiceProfile.STANDARD,
    utteranceId: String = UUID.randomUUID().toString(),
    onDone: ((String) -> Unit)? = null
  ) {
    val engine = tts ?: return
    if (!isReady) return
    onUtteranceDoneCallback = onDone
    when (voiceProfile) {
      VoiceProfile.STANDARD -> {
        engine.setPitch(1.0f)
        engine.setSpeechRate(0.90f)
      }
      VoiceProfile.NIMA -> {
        engine.setPitch(1.08f)
        engine.setSpeechRate(0.95f)
      }
      VoiceProfile.GRANDMA -> {
        engine.setPitch(0.84f)
        engine.setSpeechRate(0.80f)
      }
    }
    engine.speak(farsiText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  fun stop() {
    onUtteranceDoneCallback = null
    tts?.stop()
    _isSpeaking.value = false
    _activeUtteranceId.value = null
  }

  fun release() {
    stop()
    tts?.shutdown()
    tts = null
  }
}
