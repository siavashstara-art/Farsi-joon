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
  private val callbackLock = Any()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _persianVoiceAvailable = MutableStateFlow(true)
  val persianVoiceAvailable: StateFlow<Boolean> = _persianVoiceAvailable.asStateFlow()

  private val _ttsError = MutableStateFlow<String?>(null)
  val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

  private val _activeUtteranceId = MutableStateFlow<String?>(null)
  val activeUtteranceId: StateFlow<String?> = _activeUtteranceId.asStateFlow()

  private var expectedUtteranceId: String? = null
  private var onUtteranceDoneCallback: ((String) -> Unit)? = null
  private var onUtteranceErrorCallback: (() -> Unit)? = null

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val engine = tts ?: return@TextToSpeech
        evaluatePersianSupport(engine)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
            _activeUtteranceId.value = utteranceId
            _ttsError.value = null
          }

          override fun onDone(utteranceId: String?) {
            val callback = synchronized(callbackLock) {
              if (utteranceId != null && expectedUtteranceId != null && utteranceId != expectedUtteranceId) {
                null
              } else {
                val cb = onUtteranceDoneCallback
                expectedUtteranceId = null
                onUtteranceDoneCallback = null
                onUtteranceErrorCallback = null
                cb
              }
            }
            _isSpeaking.value = false
            _activeUtteranceId.value = null
            if (utteranceId != null && callback != null) {
              mainHandler.post {
                callback.invoke(utteranceId)
              }
            }
          }

          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            handleTtsError(utteranceId)
          }

          override fun onError(utteranceId: String?, errorCode: Int) {
            handleTtsError(utteranceId)
          }
        })
        isReady = true
      } else {
        isReady = false
        _persianVoiceAvailable.value = false
        _ttsError.value = "Device Text-to-Speech engine is unavailable."
      }
    }
  }

  private fun evaluatePersianSupport(engine: TextToSpeech) {
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
  }

  private fun handleTtsError(utteranceId: String?) {
    val errCallback = synchronized(callbackLock) {
      if (utteranceId != null && expectedUtteranceId != null && utteranceId != expectedUtteranceId) {
        null
      } else {
        val cb = onUtteranceErrorCallback
        expectedUtteranceId = null
        onUtteranceDoneCallback = null
        onUtteranceErrorCallback = null
        cb
      }
    }
    _isSpeaking.value = false
    _activeUtteranceId.value = null
    _ttsError.value = if (_persianVoiceAvailable.value) {
      "Could not play audio on this device."
    } else {
      "Persian voice data is not installed on this device."
    }
    if (errCallback != null) {
      mainHandler.post {
        errCallback.invoke()
      }
    }
  }

  fun recheckVoiceAvailability() {
    val engine = tts ?: return
    if (!isReady) return
    evaluatePersianSupport(engine)
    if (_persianVoiceAvailable.value) {
      _ttsError.value = null
    }
  }

  fun clearError() {
    _ttsError.value = null
  }

  fun speak(
    farsiText: String,
    voiceProfile: VoiceProfile = VoiceProfile.STANDARD,
    utteranceId: String = UUID.randomUUID().toString(),
    onDone: ((String) -> Unit)? = null,
    onError: (() -> Unit)? = null
  ) {
    val cleanText = farsiText.trim()
    if (cleanText.isEmpty()) {
      if (onError != null) {
        mainHandler.post { onError.invoke() }
      }
      return
    }
    val engine = tts
    if (engine == null || !isReady) {
      _ttsError.value = "Speech engine is not ready yet."
      if (onError != null) {
        mainHandler.post { onError.invoke() }
      }
      return
    }
    if (!_persianVoiceAvailable.value) {
      _ttsError.value = "Persian voice data is missing on this device. Install Persian TTS or use Finglish below."
      if (onError != null) {
        mainHandler.post { onError.invoke() }
      }
      return
    }

    synchronized(callbackLock) {
      expectedUtteranceId = utteranceId
      onUtteranceDoneCallback = onDone
      onUtteranceErrorCallback = onError
    }

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
    val safeText = cleanText.take(3500)
    val result = engine.speak(safeText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    if (result == TextToSpeech.ERROR) {
      handleTtsError(utteranceId)
    }
  }

  fun stop() {
    synchronized(callbackLock) {
      expectedUtteranceId = null
      onUtteranceDoneCallback = null
      onUtteranceErrorCallback = null
    }
    tts?.stop()
    _isSpeaking.value = false
    _activeUtteranceId.value = null
  }

  fun release() {
    stop()
    tts?.shutdown()
    tts = null
    isReady = false
  }
}
