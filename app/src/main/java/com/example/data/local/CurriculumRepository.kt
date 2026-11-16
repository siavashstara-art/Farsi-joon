package com.example.data.local

import android.content.Context
import com.example.model.Lesson
import com.example.model.LessonsResponse
import com.example.model.StoriesResponse
import com.example.model.Story
import com.example.model.WordItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurriculumRepository(private val context: Context) {

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  @Volatile
  private var cachedLessons: List<Lesson>? = null

  @Volatile
  private var cachedStories: List<Story>? = null

  suspend fun getLessons(): List<Lesson> = withContext(Dispatchers.IO) {
    cachedLessons?.let { return@withContext it }
    val json = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
    val adapter = moshi.adapter(LessonsResponse::class.java)
    val parsed = adapter.fromJson(json)?.lessons.orEmpty()
    cachedLessons = parsed
    parsed
  }

  suspend fun getStories(): List<Story> = withContext(Dispatchers.IO) {
    cachedStories?.let { return@withContext it }
    val json = context.assets.open("stories.json").bufferedReader().use { it.readText() }
    val adapter = moshi.adapter(StoriesResponse::class.java)
    val parsed = adapter.fromJson(json)?.stories.orEmpty()
    cachedStories = parsed
    parsed
  }

  suspend fun getAllWordsById(): Map<String, Pair<Int, WordItem>> {
    return getLessons().flatMap { lesson ->
      lesson.words.map { word -> word.id to (lesson.id to word) }
    }.toMap()
  }
}
