package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LessonsResponse(
  val lessons: List<Lesson> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Lesson(
  val id: Int,
  val title: String,
  val farsiTitle: String,
  val subtitle: String,
  val isCheckpointAfter: Boolean = false,
  val isAlphabetLesson: Boolean = false,
  val colloquialNote: String = "",
  val words: List<WordItem> = emptyList(),
  val phrases: List<PhraseItem> = emptyList(),
  val sentences: List<SentenceItem> = emptyList(),
  val letters: List<AlphabetLetter> = emptyList(),
  val quiz: List<QuizQuestion> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WordItem(
  val id: String,
  val farsi: String,
  val finglish: String,
  val english: String,
  val note: String = ""
)

@JsonClass(generateAdapter = true)
data class PhraseItem(
  val id: String,
  val farsi: String,
  val finglish: String,
  val english: String
)

@JsonClass(generateAdapter = true)
data class SentenceItem(
  val id: String,
  val farsi: String,
  val finglish: String,
  val english: String,
  val speaker: String = "A"
)

@JsonClass(generateAdapter = true)
data class AlphabetLetter(
  val letter: String,
  val name: String,
  val sound: String,
  val isolated: String,
  val initial: String,
  val medial: String,
  val final: String,
  val exampleFarsi: String,
  val exampleFinglish: String,
  val exampleEnglish: String
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
  val id: String,
  val type: String,
  val prompt: String,
  val promptFarsi: String = "",
  val promptFinglish: String = "",
  val options: List<String> = emptyList(),
  val correctIndex: Int = 0,
  val explanation: String = ""
)

@JsonClass(generateAdapter = true)
data class StoriesResponse(
  val stories: List<Story> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Story(
  val id: String,
  val title: String,
  val farsiTitle: String,
  val level: String,
  val durationLabel: String,
  val imageName: String,
  val hasTwoVoices: Boolean = false,
  val culturalNote: String = "",
  val sentences: List<StorySentence> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StorySentence(
  val id: String,
  val speaker: String,
  val speakerLabel: String,
  val farsi: String,
  val finglish: String,
  val english: String
)

// Firestore Cloud Models

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val totalStars: Long = 0L,
  val completedLessonsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudLessonProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val lessonId: Long = 1L,
  val completed: Boolean = false,
  val stars: Long = 0L,
  val bestScore: Long = 0L,
  val totalQuestions: Long = 4L,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyChatUsage(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val messageCount: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatReport(
  @DocumentId val id: String = "",
  val userId: String = "",
  val promptText: String = "",
  val responseText: String = "",
  val reason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)
