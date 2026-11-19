package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 100% on-device voice recorder and playback for Step 4 (Speak & Compare).
 * Audio is stored temporarily in app cache and never transmitted to any server.
 */
class LocalVoiceRecorder(private val context: Context) {

  private var recorder: MediaRecorder? = null
  private var player: MediaPlayer? = null
  private val outputFile = File(context.cacheDir, "farsi_joon_speak_practice.m4a")

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _isPlayingRecording = MutableStateFlow(false)
  val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

  private val _hasRecording = MutableStateFlow(false)
  val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

  private val _recordingError = MutableStateFlow<String?>(null)
  val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

  fun clearError() {
    _recordingError.value = null
  }

  fun startRecording(): Boolean {
    stopPlayback()
    stopRecording()
    _recordingError.value = null
    var mediaRecorder: MediaRecorder? = null
    return try {
      mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context.applicationContext)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      mediaRecorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(96000)
        setAudioSamplingRate(44100)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
      }
      recorder = mediaRecorder
      _isRecording.value = true
      true
    } catch (e: Exception) {
      Log.e("LocalVoiceRecorder", "Failed to start on-device recording", e)
      runCatching { mediaRecorder?.release() }
      recorder = null
      _isRecording.value = false
      _recordingError.value = "Could not access microphone on this device."
      false
    }
  }

  fun stopRecording() {
    val activeRecorder = recorder
    if (activeRecorder == null) {
      _isRecording.value = false
      return
    }
    var stopSucceeded = false
    try {
      activeRecorder.stop()
      stopSucceeded = true
    } catch (e: Exception) {
      Log.w("LocalVoiceRecorder", "Stop recording error: ${e.message}")
      if (outputFile.exists()) {
        outputFile.delete()
      }
      _recordingError.value = "Recording was too short. Hold and speak for at least 1 second."
    } finally {
      runCatching { activeRecorder.release() }
      recorder = null
      _isRecording.value = false
      _hasRecording.value = stopSucceeded && outputFile.exists() && outputFile.length() > 0L
    }
  }

  fun playRecording() {
    if (!outputFile.exists() || outputFile.length() == 0L) return
    stopRecording()
    stopPlayback()
    _recordingError.value = null
    var mediaPlayer: MediaPlayer? = null
    try {
      mediaPlayer = MediaPlayer().apply {
        setDataSource(outputFile.absolutePath)
        setOnCompletionListener {
          _isPlayingRecording.value = false
          runCatching { release() }
          player = null
        }
        setOnErrorListener { mp, _, _ ->
          _isPlayingRecording.value = false
          _recordingError.value = "Could not play back recorded audio."
          runCatching { mp.release() }
          player = null
          true
        }
        prepare()
        start()
      }
      player = mediaPlayer
      _isPlayingRecording.value = true
    } catch (e: Exception) {
      Log.e("LocalVoiceRecorder", "Playback failed", e)
      runCatching { mediaPlayer?.release() }
      player = null
      _isPlayingRecording.value = false
      _recordingError.value = "Playback failed. Please record again."
    }
  }

  fun stopPlayback() {
    try {
      player?.apply {
        if (isPlaying) stop()
        release()
      }
    } catch (_: Exception) {
      runCatching { player?.release() }
    } finally {
      player = null
      _isPlayingRecording.value = false
    }
  }

  fun clearRecording() {
    stopRecording()
    stopPlayback()
    if (outputFile.exists()) {
      outputFile.delete()
    }
    _hasRecording.value = false
    _recordingError.value = null
  }
}
