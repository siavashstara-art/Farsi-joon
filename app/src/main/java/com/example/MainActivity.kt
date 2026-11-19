package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.ui.screens.ChatTabScreen
import com.example.ui.screens.LessonPlayerScreen
import com.example.ui.screens.LessonsTabScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReviewTabScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.screens.StoriesTabScreen
import com.example.ui.screens.StoryReaderScreen
import com.example.ui.theme.FarsiJoonTheme
import com.example.viewmodel.LearningViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class MainTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  LESSONS("Lessons", Icons.Rounded.School, Icons.Outlined.School, "tab_lessons"),
  REVIEW("Review", Icons.Rounded.Style, Icons.Outlined.Style, "tab_review"),
  STORIES("Stories", Icons.Rounded.AutoStories, Icons.Outlined.AutoStories, "tab_stories"),
  CHAT("Chat", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "tab_chat")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before rendering Compose UI or creating ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    setContent {
      FarsiJoonApp()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarsiJoonApp() {
  val learningViewModel: LearningViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        LearningViewModel.create(app)
      }
    }
  )

  val uiState by learningViewModel.uiState.collectAsStateWithLifecycle()
  val persianVoiceAvailable by learningViewModel.audioPlayer.persianVoiceAvailable.collectAsStateWithLifecycle()
  val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
    initialValue = Firebase.auth.currentUser
  )

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  LaunchedEffect(Unit) {
    val credentialManager = CredentialManager.create(context)
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { learningViewModel.syncAllLocalProgressOnSignIn() },
      onUnauthenticated = {},
      scope = scope
    )
  }

  var activeTab by rememberSaveable { mutableStateOf(MainTab.LESSONS) }
  var activeLessonId by rememberSaveable { mutableStateOf<Int?>(null) }
  var activeStoryId by rememberSaveable { mutableStateOf<String?>(null) }
  var showSettingsSheet by remember { mutableStateOf(false) }

  FarsiJoonTheme(themeMode = uiState.preferences.themeMode) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      if (!uiState.isLoading && !uiState.preferences.onboardingCompleted) {
        OnboardingScreen(
          onFinish = { learningViewModel.completeOnboarding() }
        )
        return@Surface
      }

      val selectedLesson = activeLessonId?.let { id ->
        uiState.lessons.firstOrNull { it.id == id }
      }
      if (selectedLesson != null) {
        LessonPlayerScreen(
          lesson = selectedLesson,
          hideFinglishAfter12 = uiState.preferences.hideFinglishAfter12,
          persianVoiceAvailable = persianVoiceAvailable,
          voiceRecorder = learningViewModel.voiceRecorder,
          onSpeak = { text, speaker -> learningViewModel.speakFarsi(text, speaker) },
          onRecheckTts = { learningViewModel.recheckTtsVoice() },
          onToggleHideFinglish = { learningViewModel.setHideFinglishAfter12(it) },
          onLessonCompleted = { score, total ->
            learningViewModel.completeLesson(selectedLesson.id, score, total)
          },
          onBack = { activeLessonId = null }
        )
        return@Surface
      }

      val selectedStory = activeStoryId?.let { id ->
        uiState.stories.firstOrNull { it.id == id }
      }
      if (selectedStory != null) {
        StoryReaderScreen(
          story = selectedStory,
          audioPlayer = learningViewModel.audioPlayer,
          onBack = { activeStoryId = null }
        )
        return@Surface
      }

      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          topBar = {
            CenterAlignedTopAppBar(
              title = {
                Text(
                  text = when (activeTab) {
                    MainTab.LESSONS -> "Farsi-Joon"
                    MainTab.REVIEW -> "Leitner Review"
                    MainTab.STORIES -> "Stories"
                    MainTab.CHAT -> "Persian Chat"
                  },
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              },
              actions = {
                IconButton(
                  onClick = { showSettingsSheet = true },
                  modifier = Modifier.testTag("open_settings_button")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings and Account"
                  )
                }
              }
            )
          },
          bottomBar = {
            if (!useNavigationRail) {
              NavigationBar {
                MainTab.entries.forEach { tab ->
                  val selected = activeTab == tab
                  NavigationBarItem(
                    selected = selected,
                    onClick = { activeTab = tab },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                      )
                    },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag(tab.testTag)
                  )
                }
              }
            }
          }
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            if (useNavigationRail) {
              NavigationRail {
                MainTab.entries.forEach { tab ->
                  val selected = activeTab == tab
                  NavigationRailItem(
                    selected = selected,
                    onClick = { activeTab = tab },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                      )
                    },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag(tab.testTag)
                  )
                }
              }
            }

            when (activeTab) {
              MainTab.LESSONS -> LessonsTabScreen(
                uiState = uiState,
                onSelectLesson = { activeLessonId = it },
                modifier = Modifier.weight(1f)
              )
              MainTab.REVIEW -> ReviewTabScreen(
                uiState = uiState,
                onReviewCard = { wordId, isCorrect ->
                  learningViewModel.reviewLeitnerCard(wordId, isCorrect)
                },
                onSpeak = { text, speaker -> learningViewModel.speakFarsi(text, speaker) },
                modifier = Modifier.weight(1f)
              )
              MainTab.STORIES -> StoriesTabScreen(
                stories = uiState.stories,
                onSelectStory = { activeStoryId = it },
                modifier = Modifier.weight(1f)
              )
              MainTab.CHAT -> ChatTabScreen(
                currentUser = currentUser,
                onSpeak = { text, speaker -> learningViewModel.speakFarsi(text, speaker) },
                onSignedInSync = { learningViewModel.syncAllLocalProgressOnSignIn() },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      if (showSettingsSheet) {
        SettingsBottomSheet(
          currentUser = currentUser,
          preferences = uiState.preferences,
          onThemeChanged = { learningViewModel.setThemeMode(it) },
          onToggleHideFinglishAfter12 = { learningViewModel.setHideFinglishAfter12(it) },
          onToggleDailyReminder = { learningViewModel.setDailyReminder(it) },
          onSignedInSync = { learningViewModel.syncAllLocalProgressOnSignIn() },
          onClearLocalProgress = { learningViewModel.resetLocalProgressOnly() },
          onDismiss = { showSettingsSheet = false }
        )
      }
    }
  }
}
