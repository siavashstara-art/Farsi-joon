package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CurriculumRepository
import com.example.data.local.FarsiDatabase
import com.example.data.local.LearningProgressRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LearningCoreRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: FarsiDatabase
  private lateinit var curriculumRepo: CurriculumRepository
  private lateinit var progressRepo: LearningProgressRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, FarsiDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    curriculumRepo = CurriculumRepository(context)
    progressRepo = LearningProgressRepository(db.farsiDao(), curriculumRepo)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun curriculumLoads20LessonsAndStories() = runBlocking {
    val lessons = curriculumRepo.getLessons()
    assertEquals(20, lessons.size)
    assertEquals((1..20).toList(), lessons.map { it.id })

    // Checkpoints after 5, 10, 15, 20
    listOf(5, 10, 15, 20).forEach { id ->
      assertTrue("Lesson $id should have checkpoint flag", lessons.first { it.id == id }.isCheckpointAfter)
    }

    // Alphabet lessons 3, 7, 12 with 4 forms
    listOf(3, 7, 12).forEach { id ->
      val lesson = lessons.first { it.id == id }
      assertTrue("Lesson $id should be alphabet lesson", lesson.isAlphabetLesson)
      assertTrue("Lesson $id should have letters", lesson.letters.isNotEmpty())
      lesson.letters.forEach { letter ->
        assertTrue(letter.isolated.isNotBlank())
        assertTrue(letter.initial.isNotBlank())
        assertTrue(letter.medial.isNotBlank())
        assertTrue(letter.final.isNotBlank())
      }
    }

    // Lesson 11 includes "to chi?"
    val lesson11 = lessons.first { it.id == 11 }
    val hasToChi = lesson11.words.any { it.finglish.contains("to chi", ignoreCase = true) } ||
      lesson11.phrases.any { it.finglish.contains("to chi", ignoreCase = true) } ||
      lesson11.sentences.any { it.finglish.contains("to chi", ignoreCase = true) }
    assertTrue("Lesson 11 must include 'to chi?'", hasToChi)

    // Every lesson has words and quiz items
    lessons.forEach { lesson ->
      assertTrue("Lesson ${lesson.id} needs words", lesson.words.size in 4..10)
      assertTrue("Lesson ${lesson.id} needs quiz questions", lesson.quiz.size >= 3)
    }

    val stories = curriculumRepo.getStories()
    assertTrue("Should have curated bilingual stories", stories.size >= 3)
    assertTrue(
      "Grandma call story should support two voices",
      stories.any { it.hasTwoVoices && it.sentences.map { s -> s.speaker }.distinct().size >= 2 }
    )
  }

  @Test
  fun leitnerAdvancesBoxesAndResetsToBox1OnWrongAnswer() = runBlocking {
    val baseTime = 1_700_000_000_000L
    progressRepo.recordLessonCompletion(lessonId = 1, score = 4, totalQuestions = 4, nowMillis = baseTime)

    val allProgress = db.farsiDao().getAllLessonProgress()
    assertEquals(1, allProgress.size)
    assertEquals(3, allProgress.first().stars)

    val wordId = curriculumRepo.getLessons().first().words.first().id

    // Box 1 -> Box 2 (2 days)
    val box2 = progressRepo.recordLeitnerReview(wordId, isCorrect = true, nowMillis = baseTime)!!
    assertEquals(2, box2.boxLevel)
    assertEquals(baseTime + TimeUnit.DAYS.toMillis(2), box2.nextReviewAt)

    // Box 2 -> Box 3 (3 days)
    val box3 = progressRepo.recordLeitnerReview(wordId, isCorrect = true, nowMillis = baseTime)!!
    assertEquals(3, box3.boxLevel)
    assertEquals(baseTime + TimeUnit.DAYS.toMillis(3), box3.nextReviewAt)

    // Box 3 -> Box 4 (7 days)
    val box4 = progressRepo.recordLeitnerReview(wordId, isCorrect = true, nowMillis = baseTime)!!
    assertEquals(4, box4.boxLevel)
    assertEquals(baseTime + TimeUnit.DAYS.toMillis(7), box4.nextReviewAt)

    // Box 4 -> Box 5 (14 days)
    val box5 = progressRepo.recordLeitnerReview(wordId, isCorrect = true, nowMillis = baseTime)!!
    assertEquals(5, box5.boxLevel)
    assertEquals(baseTime + TimeUnit.DAYS.toMillis(14), box5.nextReviewAt)

    // Wrong answer resets back to Box 1 (1 day)
    val resetBox1 = progressRepo.recordLeitnerReview(wordId, isCorrect = false, nowMillis = baseTime)!!
    assertEquals(1, resetBox1.boxLevel)
    assertEquals(baseTime + TimeUnit.DAYS.toMillis(1), resetBox1.nextReviewAt)
    assertEquals(4, resetBox1.correctCount)
    assertEquals(1, resetBox1.wrongCount)
  }
}
