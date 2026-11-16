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

  fun startRecording(): Boolean {
    stopPlayback()
    stopRecording()
    return try {
      val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
      _isRecording.value = false
      false
    }
  }

  fun stopRecording() {
    try {
      recorder?.apply {
        stop()
        release()
      }
    } catch (e: Exception) {
      Log.w("LocalVoiceRecorder", "Stop recording error: ${e.message}")
    } finally {
      recorder = null
      _isRecording.value = false
      _hasRecording.value = outputFile.exists() && outputFile.length() > 0L
    }
  }

  fun playRecording() {
    if (!outputFile.exists() || outputFile.length() == 0L) return
    stopRecording()
    stopPlayback()
    try {
      val mediaPlayer = MediaPlayer().apply {
        setDataSource(outputFile.absolutePath)
        prepare()
        setOnCompletionListener {
          _isPlayingRecording.value = false
          release()
          player = null
        }
        start()
      }
      player = mediaPlayer
      _isPlayingRecording.value = true
    } catch (e: Exception) {
      Log.e("LocalVoiceRecorder", "Playback failed", e)
      _isPlayingRecording.value = false
    }
  }

  fun stopPlayback() {
    try {
      player?.apply {
        if (isPlaying) stop()
        release()
      }
    } catch (_: Exception) {
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
  }
}
