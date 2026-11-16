package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Offline-first Leitner spaced repetition engine & progress manager.
 * Intervals: Box 1 = 1 day, Box 2 = 2 days, Box 3 = 3 days, Box 4 = 7 days, Box 5 = 14 days.
 * Wrong answer resets card back to Box 1.
 */
class LearningProgressRepository(
  private val dao: FarsiDao,
  private val curriculumRepository: CurriculumRepository
) {
  val lessonProgressFlow: Flow<List<LessonProgressEntity>> = dao.observeLessonProgress()
  val leitnerCardsFlow: Flow<List<LeitnerCardEntity>> = dao.observeLeitnerCards()

  suspend fun ensureStarterWordsInLeitner(nowMillis: Long = System.currentTimeMillis()) {
    val existing = dao.getDueLeitnerCards(Long.MAX_VALUE)
    if (existing.isEmpty()) {
      val lessons = curriculumRepository.getLessons()
      val lesson1 = lessons.firstOrNull { it.id == 1 } ?: return
      val cards = lesson1.words.map { word ->
        LeitnerCardEntity(
          wordId = word.id,
          lessonId = lesson1.id,
          boxLevel = 1,
          lastReviewedAt = 0L,
          nextReviewAt = nowMillis,
          correctCount = 0,
          wrongCount = 0
        )
      }
      dao.upsertLeitnerCards(cards)
    }
  }

  suspend fun recordLessonCompletion(
    lessonId: Int,
    score: Int,
    totalQuestions: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): LessonProgressEntity {
    val existing = dao.getAllLessonProgress().firstOrNull { it.lessonId == lessonId }
    val ratio = if (totalQuestions > 0) score.toFloat() / totalQuestions else 1f
    val earnedStars = when {
      ratio >= 0.9f -> 3
      ratio >= 0.7f -> 2
      ratio >= 0.4f -> 1
      else -> 1
    }
    val updated = LessonProgressEntity(
      lessonId = lessonId,
      completed = true,
      stars = maxOf(existing?.stars ?: 0, earnedStars),
      bestScore = maxOf(existing?.bestScore ?: 0, score),
      totalQuestions = totalQuestions,
      lastCompletedAt = nowMillis
    )
    dao.upsertLessonProgress(updated)

    // Unlock lesson vocabulary into Leitner box 1 (ready for review) if not yet present
    val lesson = curriculumRepository.getLessons().firstOrNull { it.id == lessonId }
    if (lesson != null) {
      val newCards = lesson.words.mapNotNull { word ->
        if (dao.getLeitnerCard(word.id) == null) {
          LeitnerCardEntity(
            wordId = word.id,
            lessonId = lessonId,
            boxLevel = 1,
            lastReviewedAt = 0L,
            nextReviewAt = nowMillis,
            correctCount = 0,
            wrongCount = 0
          )
        } else {
          null
        }
      }
      if (newCards.isNotEmpty()) {
        dao.upsertLeitnerCards(newCards)
      }
    }
    return updated
  }

  suspend fun recordLeitnerReview(
    wordId: String,
    isCorrect: Boolean,
    nowMillis: Long = System.currentTimeMillis()
  ): LeitnerCardEntity? {
    val current = dao.getLeitnerCard(wordId) ?: return null
    val nextBox = if (isCorrect) (current.boxLevel + 1).coerceAtMost(5) else 1
    val intervalDays = intervalDaysForBox(nextBox)
    val nextReviewAt = nowMillis + TimeUnit.DAYS.toMillis(intervalDays.toLong())
    val updated = current.copy(
      boxLevel = nextBox,
      lastReviewedAt = nowMillis,
      nextReviewAt = nextReviewAt,
      correctCount = current.correctCount + if (isCorrect) 1 else 0,
      wrongCount = current.wrongCount + if (isCorrect) 0 else 1
    )
    dao.upsertLeitnerCard(updated)
    return updated
  }

  suspend fun clearAllLocalProgress() {
    dao.clearLessonProgress()
    dao.clearLeitnerCards()
  }

  companion object {
    fun intervalDaysForBox(boxLevel: Int): Int = when (boxLevel) {
      1 -> 1
      2 -> 2
      3 -> 3
      4 -> 7
      5 -> 14
      else -> 1
    }
  }
}
