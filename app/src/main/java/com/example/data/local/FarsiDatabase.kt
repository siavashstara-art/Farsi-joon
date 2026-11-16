package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
  @PrimaryKey val lessonId: Int,
  val completed: Boolean = false,
  val stars: Int = 0,
  val bestScore: Int = 0,
  val totalQuestions: Int = 4,
  val lastCompletedAt: Long = 0L
)

@Entity(tableName = "leitner_cards")
data class LeitnerCardEntity(
  @PrimaryKey val wordId: String,
  val lessonId: Int,
  val boxLevel: Int = 1,
  val lastReviewedAt: Long = 0L,
  val nextReviewAt: Long = 0L,
  val correctCount: Int = 0,
  val wrongCount: Int = 0
)

@Dao
interface FarsiDao {
  @Query("SELECT * FROM lesson_progress ORDER BY lessonId ASC")
  fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

  @Query("SELECT * FROM lesson_progress ORDER BY lessonId ASC")
  suspend fun getAllLessonProgress(): List<LessonProgressEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLessonProgress(progress: LessonProgressEntity)

  @Query("DELETE FROM lesson_progress")
  suspend fun clearLessonProgress()

  @Query("SELECT * FROM leitner_cards ORDER BY nextReviewAt ASC")
  fun observeLeitnerCards(): Flow<List<LeitnerCardEntity>>

  @Query("SELECT * FROM leitner_cards WHERE nextReviewAt <= :nowMillis ORDER BY boxLevel ASC, nextReviewAt ASC")
  suspend fun getDueLeitnerCards(nowMillis: Long): List<LeitnerCardEntity>

  @Query("SELECT * FROM leitner_cards WHERE wordId = :wordId LIMIT 1")
  suspend fun getLeitnerCard(wordId: String): LeitnerCardEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLeitnerCards(cards: List<LeitnerCardEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLeitnerCard(card: LeitnerCardEntity)

  @Query("DELETE FROM leitner_cards")
  suspend fun clearLeitnerCards()
}

@Database(
  entities = [LessonProgressEntity::class, LeitnerCardEntity::class],
  version = 1,
  exportSchema = false
)
abstract class FarsiDatabase : RoomDatabase() {
  abstract fun farsiDao(): FarsiDao

  companion object {
    @Volatile
    private var INSTANCE: FarsiDatabase? = null

    fun getInstance(context: Context): FarsiDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          FarsiDatabase::class.java,
          "farsi_joon_local.db"
        ).build().also { INSTANCE = it }
      }
    }
  }
}
