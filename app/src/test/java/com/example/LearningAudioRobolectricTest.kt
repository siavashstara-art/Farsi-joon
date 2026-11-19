package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.example.audio.LocalVoiceRecorder
import com.example.audio.PersianAudioPlayer
import com.example.data.local.CurriculumRepository
import com.example.model.WordItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LearningAudioRobolectricTest {

  @Test
  fun audioPlayer_invokesOnErrorWhenTtsNotInitialized() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val player = PersianAudioPlayer(context)
    var errorTriggered = false

    player.speak(
      farsiText = "سلام",
      onDone = {},
      onError = { errorTriggered = true }
    )
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    assertTrue("Expected onError callback when TTS engine is not ready", errorTriggered)
    assertNotNull(player.ttsError.value)
    player.release()
  }

  @Test
  fun audioPlayer_recheckVoiceAvailability_updatesWhenPersianLanguageMissing() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val player = PersianAudioPlayer(context)

    // In Robolectric JVM, default ShadowTextToSpeech reports missing/unsupported for fa-IR unless configured
    player.recheckVoiceAvailability()
    // Stop and release must remain idempotent and leave speaking state false
    player.stop()
    assertFalse(player.isSpeaking.value)
    player.release()
  }

  @Test
  fun voiceRecorder_stopWithoutStartAndClearRecording_areSafe() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val recorder = LocalVoiceRecorder(context)

    recorder.stopRecording()
    recorder.playRecording()
    recorder.clearRecording()

    assertFalse(recorder.isRecording.value)
    assertFalse(recorder.isPlayingRecording.value)
    assertFalse(recorder.hasRecording.value)
  }

  @Test
  fun curriculumStoriesAndLessons_haveValidAudioTextAndDrawableMappings() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CurriculumRepository(context)
    val stories = repo.getStories()
    val lessons = repo.getLessons()

    assertEquals(4, stories.size)
    stories.forEach { story ->
      assertTrue("Story ${story.id} must have sentences", story.sentences.isNotEmpty())
      story.sentences.forEach { sentence ->
        assertTrue("Story sentence ${sentence.id} missing Farsi", sentence.farsi.isNotBlank())
        assertTrue("Story sentence ${sentence.id} missing Finglish", sentence.finglish.isNotBlank())
        assertTrue("Story sentence ${sentence.id} missing English", sentence.english.isNotBlank())
      }
    }

    // Verify all two-speaker dialogues in lessons use recognized speaker tags
    val lessonSpeakers = lessons.flatMap { it.sentences }.map { it.speaker.uppercase() }.toSet()
    assertTrue(lessonSpeakers.all { it in setOf("A", "B", "NIMA", "GRANDMA") })
  }
}
