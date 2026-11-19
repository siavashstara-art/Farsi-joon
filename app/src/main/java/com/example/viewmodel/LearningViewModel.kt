package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LocalVoiceRecorder
import com.example.audio.PersianAudioPlayer
import com.example.audio.VoiceProfile
import com.example.data.firebase.CloudSyncRepository
import com.example.data.local.CurriculumRepository
import com.example.data.local.FarsiDatabase
import com.example.data.local.LearningProgressRepository
import com.example.data.local.LeitnerCardEntity
import com.example.data.local.LessonProgressEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.model.Lesson
import com.example.model.Story
import com.example.model.WordItem
import com.example.notifications.DailyReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LeitnerCardWithWord(
  val card: LeitnerCardEntity,
  val word: WordItem
)

data class LearningUiState(
  val isLoading: Boolean = true,
  val lessons: List<Lesson> = emptyList(),
  val stories: List<Story> = emptyList(),
  val progressMap: Map<Int, LessonProgressEntity> = emptyMap(),
  val leitnerCards: List<LeitnerCardWithWord> = emptyList(),
  val dueCardsCount: Int = 0,
  val boxCounts: Map<Int, Int> = (1..5).associateWith { 0 },
  val totalStars: Int = 0,
  val completedCount: Int = 0,
  val completionPercent: Int = 0,
  val preferences: UserPreferences = UserPreferences()
)

class LearningViewModel(
  private val appContext: Context,
  private val curriculumRepo: CurriculumRepository,
  private val progressRepo: LearningProgressRepository,
  private val prefsRepo: PreferencesRepository,
  val audioPlayer: PersianAudioPlayer,
  val voiceRecorder: LocalVoiceRecorder
) : ViewModel() {

  private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
  private val _stories = MutableStateFlow<List<Story>>(emptyList())
  private val _wordLookup = MutableStateFlow<Map<String, Pair<Int, WordItem>>>(emptyMap())
  private val _isLoaded = MutableStateFlow(false)

  val uiState: StateFlow<LearningUiState> = combine(
    _isLoaded,
    _lessons,
    _stories,
    progressRepo.lessonProgressFlow,
    progressRepo.leitnerCardsFlow,
    prefsRepo.preferencesFlow
  ) { values ->
    val loaded = values[0] as Boolean
    @Suppress("UNCHECKED_CAST")
    val lessons = values[1] as List<Lesson>
    @Suppress("UNCHECKED_CAST")
    val stories = values[2] as List<Story>
    @Suppress("UNCHECKED_CAST")
    val progressList = values[3] as List<LessonProgressEntity>
    @Suppress("UNCHECKED_CAST")
    val cards = values[4] as List<LeitnerCardEntity>
    val prefs = values[5] as UserPreferences

    val progressMap = progressList.associateBy { it.lessonId }
    val lookup = _wordLookup.value
    val now = System.currentTimeMillis()
    val enrichedCards = cards.mapNotNull { card ->
      val word = lookup[card.wordId]?.second ?: return@mapNotNull null
      LeitnerCardWithWord(card = card, word = word)
    }
    val dueCount = enrichedCards.count { it.card.nextReviewAt <= now }
    val boxMap = (1..5).associateWith { box -> enrichedCards.count { it.card.boxLevel == box } }
    val completedCount = progressList.count { it.completed }
    val totalStars = progressList.sumOf { it.stars }
    val percent = if (lessons.isNotEmpty()) ((completedCount * 100f) / lessons.size).toInt() else 0

    LearningUiState(
      isLoading = !loaded,
      lessons = lessons,
      stories = stories,
      progressMap = progressMap,
      leitnerCards = enrichedCards,
      dueCardsCount = dueCount,
      boxCounts = boxMap,
      totalStars = totalStars,
      completedCount = completedCount,
      completionPercent = percent,
      preferences = prefs
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = LearningUiState()
  )

  private val _accountActionMessage = MutableStateFlow<String?>(null)
  val accountActionMessage: StateFlow<String?> = _accountActionMessage.asStateFlow()

  init {
    viewModelScope.launch {
      val lessons = curriculumRepo.getLessons()
      val stories = curriculumRepo.getStories()
      _wordLookup.value = curriculumRepo.getAllWordsById()
      _lessons.value = lessons
      _stories.value = stories
      progressRepo.ensureStarterWordsInLeitner()
      _isLoaded.value = true
    }
  }

  fun completeOnboarding() {
    viewModelScope.launch {
      prefsRepo.setOnboardingCompleted(true)
    }
  }

  fun setHideFinglishAfter12(hide: Boolean) {
    viewModelScope.launch {
      prefsRepo.setHideFinglishAfter12(hide)
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      prefsRepo.setThemeMode(mode)
    }
  }

  fun setDailyReminder(enabled: Boolean) {
    viewModelScope.launch {
      prefsRepo.setDailyReminder(enabled, 19, 0)
      if (enabled) {
        DailyReminderScheduler.scheduleDaily(appContext, 19, 0)
      } else {
        DailyReminderScheduler.cancel(appContext)
      }
    }
  }

  fun completeLesson(lessonId: Int, score: Int, totalQuestions: Int) {
    viewModelScope.launch {
      val updated = progressRepo.recordLessonCompletion(lessonId, score, totalQuestions)
      // Sync to Firestore ONLY if already signed in with Google
      val currentUser = Firebase.auth.currentUser
      if (currentUser != null) {
        runCatching {
          val syncRepo = CloudSyncRepository(appContext)
          val currentState = uiState.value
          val allProgress = currentState.progressMap.toMutableMap().apply {
            put(lessonId, updated)
          }
          val totalStars = allProgress.values.sumOf { it.stars }
          val completedCount = allProgress.values.count { it.completed }
          syncRepo.ensureUserProfile(
            displayName = currentUser.displayName ?: "Farsi Learner",
            totalStars = totalStars,
            completedLessonsCount = completedCount
          )
          syncRepo.syncLessonProgress(updated, totalStars, completedCount)
        }.onFailure { err ->
          Log.w("LearningVM", "Cloud sync skipped/failed: ${err.message}")
        }
      }
    }
  }

  fun syncAllLocalProgressOnSignIn() {
    val currentUser = Firebase.auth.currentUser ?: return
    viewModelScope.launch {
      runCatching {
        val syncRepo = CloudSyncRepository(appContext)
        val state = uiState.value
        syncRepo.ensureUserProfile(
          displayName = currentUser.displayName ?: "Farsi Learner",
          totalStars = state.totalStars,
          completedLessonsCount = state.completedCount
        )
        state.progressMap.values.forEach { entity ->
          syncRepo.syncLessonProgress(entity, state.totalStars, state.completedCount)
        }
      }.onFailure { err ->
        Log.w("LearningVM", "Initial cloud sync failed: ${err.message}")
      }
    }
  }

  fun reviewLeitnerCard(wordId: String, isCorrect: Boolean) {
    viewModelScope.launch {
      progressRepo.recordLeitnerReview(wordId, isCorrect)
    }
  }

  fun speakFarsi(text: String, speaker: String = "STANDARD") {
    val profile = when (speaker.uppercase()) {
      "GRANDMA" -> VoiceProfile.GRANDMA
      "NIMA", "B" -> VoiceProfile.NIMA
      else -> VoiceProfile.STANDARD
    }
    audioPlayer.speak(text, profile)
  }

  fun recheckTtsVoice() {
    audioPlayer.recheckVoiceAvailability()
  }

  fun clearAccountMessage() {
    _accountActionMessage.value = null
  }

  fun resetLocalProgressOnly() {
    viewModelScope.launch {
      progressRepo.clearAllLocalProgress()
      progressRepo.ensureStarterWordsInLeitner()
      _accountActionMessage.value = "Local lesson & Leitner progress cleared."
    }
  }

  fun clearLocalDataForAccountDeletion() {
    viewModelScope.launch {
      progressRepo.clearAllLocalProgress()
      _accountActionMessage.value = null
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayer.release()
    voiceRecorder.clearRecording()
  }

  companion object {
    fun create(context: Context): LearningViewModel {
      val appCtx = context.applicationContext
      val db = FarsiDatabase.getInstance(appCtx)
      val curriculum = CurriculumRepository(appCtx)
      val progress = LearningProgressRepository(db.farsiDao(), curriculum)
      val prefs = PreferencesRepository(appCtx)
      return LearningViewModel(
        appContext = appCtx,
        curriculumRepo = curriculum,
        progressRepo = progress,
        prefsRepo = prefs,
        audioPlayer = PersianAudioPlayer(appCtx),
        voiceRecorder = LocalVoiceRecorder(appCtx)
      )
    }
  }
}
