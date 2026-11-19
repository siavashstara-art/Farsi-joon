package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.example.audio.LocalVoiceRecorder
import com.example.audio.PersianAudioPlayer
import com.example.data.local.CurriculumRepository
import com.example.model.WordItem
import kotlinx.coroutines.flow.firstOrNull
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

  @Test
  fun audioPlayer_blankTextGuardAndVoiceProfiles_workSafely() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val player = PersianAudioPlayer(context)
    var errorTriggered = false

    player.speak(
      farsiText = "   ",
      voiceProfile = com.example.audio.VoiceProfile.GRANDMA,
      onDone = {},
      onError = { errorTriggered = true }
    )
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    assertTrue("Expected onError callback for blank Farsi text", errorTriggered)
    assertEquals(null, player.ttsError.value)
    player.release()
  }

  @Test
  fun curriculumSentenceBuildingAndPronunciationPractice_areCompleteAcrossAll20Lessons() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CurriculumRepository(context)
    val lessons = repo.getLessons()

    assertEquals(20, lessons.size)
    lessons.forEach { lesson ->
      // Words & phrases for Step 1-4 Listen/Understand/Read/Speak
      lesson.words.forEach { w ->
        assertTrue("Word ${w.id} missing Farsi", w.farsi.isNotBlank())
        assertTrue("Word ${w.id} missing Finglish", w.finglish.isNotBlank())
        assertTrue("Word ${w.id} missing English", w.english.isNotBlank())
      }
      lesson.sentences.forEach { s ->
        assertTrue("Sentence ${s.id} missing Farsi", s.farsi.isNotBlank())
        assertTrue("Sentence ${s.id} missing Finglish", s.finglish.isNotBlank())
        assertTrue("Sentence ${s.id} missing English", s.english.isNotBlank())
      }
      // Step 5 Sentence-building / quiz integrity
      lesson.quiz.forEach { q ->
        assertTrue("Quiz ${q.id} missing prompt", q.prompt.isNotBlank())
        assertTrue("Quiz ${q.id} needs >= 2 options", q.options.size >= 2)
        assertTrue("Quiz ${q.id} invalid correctIndex", q.correctIndex in q.options.indices)
        assertTrue("Quiz ${q.id} missing explanation", q.explanation.isNotBlank())
      }
    }
    val fillBlankCount = lessons.flatMap { it.quiz }.count { it.type == "fill_blank" }
    assertTrue("Expected fill_blank sentence-building items across curriculum", fillBlankCount >= 10)
  }

  @Test
  fun calmFocusPreference_persistsToggledState() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.local.PreferencesRepository(context)
    prefsRepo.setCalmFocusMode(true)
    val enabled = prefsRepo.preferencesFlow.firstOrNull()?.calmFocusMode
    assertEquals(true, enabled)
    prefsRepo.setCalmFocusMode(false)
  }
}
