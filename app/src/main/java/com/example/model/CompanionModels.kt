package com.example.model

data class CompanionLine(
  val fa: String,
  val fin: String,
  val en: String
)

data class CompanionLesson(
  val id: Int, // 1..20
  val block: String, // 'A', 'B', 'C', 'D'
  val title: String,
  val titleFa: String,
  val story: List<CompanionLine>,
  val newWords: List<CompanionLine>,
  val build: List<CompanionLine>
)

data class LessonFeedback(
  val key: String,
  val fa: String,
  val en: String,
  val animation: String
)

object CompanionScoring {
  const val LISTEN = 20
  const val STORY = 30
  const val BUILD = 30
  const val WORDS = 20

  fun feedbackFor(score: Int): LessonFeedback {
    return when {
      score >= 90 -> LessonFeedback("star", "عالی بود! 🌟", "Excellent!", "snap-confetti")
      score >= 75 -> LessonFeedback("great", "آفرین! خیلی خوب پیش میری.", "Great job!", "snap")
      score >= 60 -> LessonFeedback("good", "خوبه! یه بار دیگه مرور کن.", "Good! Review once more.", "thumbs-up")
      else -> LessonFeedback("retry", "اشکال نداره، دوباره امتحان کن.", "It's okay, try again.", "gentle-wave")
    }
  }
}
