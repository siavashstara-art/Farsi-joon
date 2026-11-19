package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LocalVoiceRecorder
import com.example.model.AlphabetLetter
import com.example.model.Lesson
import com.example.ui.components.FarsiText
import com.example.ui.components.PersianTtsFallbackBanner
import com.example.ui.components.ThreeFormWordCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.StarGold
import kotlinx.coroutines.delay

enum class LessonStep(val label: String, val index: Int) {
  LISTEN("1. Listen", 0),
  UNDERSTAND("2. Understand", 1),
  READ("3. Read", 2),
  SPEAK("4. Speak", 3),
  PRACTICE("5. Practice", 4),
  COMPLETE("Complete", 5)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
  lesson: Lesson,
  hideFinglishAfter12: Boolean,
  persianVoiceAvailable: Boolean = true,
  voiceRecorder: LocalVoiceRecorder,
  onSpeak: (String, String) -> Unit,
  onRecheckTts: () -> Unit = {},
  onToggleHideFinglish: (Boolean) -> Unit,
  onLessonCompleted: (score: Int, total: Int) -> Unit,
  onBack: () -> Unit
) {
  var currentStep by remember(lesson.id) { mutableStateOf(LessonStep.LISTEN) }
  var practiceScore by remember(lesson.id) { mutableIntStateOf(0) }

  BackHandler {
    if (currentStep != LessonStep.LISTEN && currentStep != LessonStep.COMPLETE) {
      val prev = LessonStep.entries.firstOrNull { it.index == currentStep.index - 1 }
      if (prev != null) currentStep = prev else onBack()
    } else {
      onBack()
    }
  }

  DisposableEffect(lesson.id) {
    onDispose {
      voiceRecorder.stopRecording()
      voiceRecorder.stopPlayback()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Lesson ${lesson.id} • ${currentStep.label}",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = lesson.title,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("lesson_back_button")
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Lessons")
          }
        }
      )
    },
    bottomBar = {
      if (currentStep != LessonStep.PRACTICE && currentStep != LessonStep.COMPLETE) {
        Surface(
          tonalElevation = 3.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (currentStep.index > 0) {
              OutlinedButton(
                onClick = {
                  currentStep = LessonStep.entries[currentStep.index - 1]
                },
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("lesson_prev_step_button")
              ) {
                Text("Back")
              }
            }
            Button(
              onClick = {
                currentStep = LessonStep.entries[currentStep.index + 1]
              },
              modifier = Modifier
                .weight(2f)
                .height(50.dp)
                .testTag("lesson_next_step_button"),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text("Next: ${LessonStep.entries[currentStep.index + 1].label}")
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      LinearProgressIndicator(
        progress = { (currentStep.index + 1) / 6f },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
      )

      when (currentStep) {
        LessonStep.LISTEN -> ListenStepContent(lesson, persianVoiceAvailable, onRecheckTts, onSpeak)
        LessonStep.UNDERSTAND -> UnderstandStepContent(
          lesson = lesson,
          hideFinglishAfter12 = hideFinglishAfter12,
          onToggleHideFinglish = onToggleHideFinglish,
          onSpeak = onSpeak
        )
        LessonStep.READ -> ReadStepContent(lesson, onSpeak)
        LessonStep.SPEAK -> SpeakStepContent(lesson, persianVoiceAvailable, voiceRecorder, onRecheckTts, onSpeak)
        LessonStep.PRACTICE -> PracticeStepContent(
          lesson = lesson,
          onQuizFinished = { score, total ->
            practiceScore = score
            onLessonCompleted(score, total)
            currentStep = LessonStep.COMPLETE
          }
        )
        LessonStep.COMPLETE -> LessonCompleteContent(
          lesson = lesson,
          score = practiceScore,
          total = lesson.quiz.size,
          onDone = onBack
        )
      }
    }
  }
}

@Composable
private fun ListenStepContent(
  lesson: Lesson,
  persianVoiceAvailable: Boolean,
  onRecheckTts: () -> Unit,
  onSpeak: (String, String) -> Unit
) {
  val revealedIds = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (!persianVoiceAvailable) {
      item {
        PersianTtsFallbackBanner(onRecheckVoice = onRecheckTts)
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Step 1 • Ear First",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap the speaker to hear each word first — Persian script stays tucked away until you're ready to peek.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }

    items(lesson.words, key = { it.id }) { word ->
      val revealed = revealedIds[word.id] == true
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = word.english,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = word.finglish,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary
            )
            if (revealed) {
              Spacer(modifier = Modifier.height(6.dp))
              FarsiText(
                text = word.farsi,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Tap eye to peek script",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .clickable { revealedIds[word.id] = true }
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
              onClick = { revealedIds[word.id] = !revealed },
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = if (revealed) "Hide Persian script for ${word.english}" else "Show Persian script for ${word.english}"
              )
            }
            FilledTonalIconButton(
              onClick = { onSpeak(word.farsi, "STANDARD") },
              modifier = Modifier
                .size(48.dp)
                .testTag("listen_speak_${word.id}")
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UnderstandStepContent(
  lesson: Lesson,
  hideFinglishAfter12: Boolean,
  onToggleHideFinglish: (Boolean) -> Unit,
  onSpeak: (String, String) -> Unit
) {
  val dimFinglish = lesson.id >= 13

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (lesson.colloquialNote.isNotBlank()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Spoken vs. Formal & Cultural Tip",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = lesson.colloquialNote,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
        }
      }
    }

    if (dimFinglish) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Lesson 13+ Reading Mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
          )
          OutlinedButton(
            onClick = { onToggleHideFinglish(!hideFinglishAfter12) },
            modifier = Modifier.testTag("lesson_toggle_finglish_all")
          ) {
            Text(if (hideFinglishAfter12) "Show Finglish" else "Hide Finglish")
          }
        }
      }
    }

    item {
      Text(
        text = "Vocabulary (${lesson.words.size} words)",
        style = MaterialTheme.typography.titleLarge
      )
    }

    items(lesson.words, key = { it.id }) { word ->
      ThreeFormWordCard(
        word = word,
        dimFinglish = dimFinglish,
        hideFinglishDefault = dimFinglish && hideFinglishAfter12,
        onSpeak = { onSpeak(word.farsi, "STANDARD") }
      )
    }

    if (lesson.phrases.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Everyday Phrases",
          style = MaterialTheme.typography.titleLarge
        )
      }
      items(lesson.phrases, key = { it.id }) { phrase ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              FarsiText(text = phrase.farsi, fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
              Text(text = phrase.finglish, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
              Text(text = phrase.english, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            FilledTonalIconButton(
              onClick = { onSpeak(phrase.farsi, "STANDARD") },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen to phrase")
            }
          }
        }
      }
    }

    if (lesson.sentences.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Real Sentences (${lesson.sentences.size})",
          style = MaterialTheme.typography.titleLarge
        )
      }
      items(lesson.sentences, key = { it.id }) { sentence ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              val speakerDisplay = when (sentence.speaker.uppercase()) {
                "GRANDMA" -> "Mâmânbozorg"
                "NIMA" -> "Nima"
                "B" -> "Speaker B"
                "A" -> if (lesson.sentences.any { it.speaker.equals("B", ignoreCase = true) }) "Speaker A" else null
                else -> null
              }
              if (speakerDisplay != null) {
                Text(
                  text = speakerDisplay,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              FarsiText(text = sentence.farsi, fontSize = 20.sp)
              if (!(dimFinglish && hideFinglishAfter12)) {
                Text(
                  text = sentence.finglish,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
              Text(text = sentence.english, style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalIconButton(
              onClick = { onSpeak(sentence.farsi, sentence.speaker) },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen to sentence")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReadStepContent(
  lesson: Lesson,
  onSpeak: (String, String) -> Unit
) {
  val revealedIds = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (lesson.isAlphabetLesson && lesson.letters.isNotEmpty()) {
      item {
        Text(
          text = "Letter Shapes (4 Positions)",
          style = MaterialTheme.typography.titleLarge
        )
        Text(
          text = "Tap any letter card to hear its example word. Persian letters change shape depending on where they connect.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      items(lesson.letters, key = { it.name }) { letter ->
        AlphabetFourFormCard(letter = letter, onSpeak = { onSpeak(letter.exampleFarsi, "STANDARD") })
      }
    }

    item {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Script-First Reading Practice",
        style = MaterialTheme.typography.titleLarge
      )
      Text(
        text = "Try reading the Persian script aloud, then tap to check Finglish & meaning.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    items(lesson.words, key = { "read_${it.id}" }) { word ->
      val revealed = revealedIds[word.id] == true
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { revealedIds[word.id] = !revealed }
          .testTag("read_card_${word.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            FarsiText(text = word.farsi, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            if (revealed) {
              Text(text = "${word.finglish} — ${word.english}", style = MaterialTheme.typography.bodyLarge)
            } else {
              Text(
                text = "Tap card to reveal pronunciation & meaning",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          FilledTonalIconButton(
            onClick = { onSpeak(word.farsi, "STANDARD") },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear word")
          }
        }
      }
    }
  }
}

@Composable
private fun AlphabetFourFormCard(
  letter: AlphabetLetter,
  onSpeak: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "${letter.name} (${letter.sound})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Example: ${letter.exampleFarsi} (${letter.exampleFinglish}) = ${letter.exampleEnglish}",
            style = MaterialTheme.typography.bodyMedium
          )
        }
        FilledTonalIconButton(onClick = onSpeak, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear ${letter.name}")
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          "Isolated" to letter.isolated,
          "Initial" to letter.initial,
          "Medial" to letter.medial,
          "Final" to letter.final
        ).forEach { (pos, glyph) ->
          Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              FarsiText(text = glyph, fontSize = 24.sp, textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = pos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SpeakStepContent(
  lesson: Lesson,
  persianVoiceAvailable: Boolean,
  voiceRecorder: LocalVoiceRecorder,
  onRecheckTts: () -> Unit,
  onSpeak: (String, String) -> Unit
) {
  val context = LocalContext.current
  val isRecording by voiceRecorder.isRecording.collectAsStateWithLifecycle()
  val isPlayingRecording by voiceRecorder.isPlayingRecording.collectAsStateWithLifecycle()
  val hasRecording by voiceRecorder.hasRecording.collectAsStateWithLifecycle()
  val recordingError by voiceRecorder.recordingError.collectAsStateWithLifecycle()
  var selectedIdx by remember(lesson.id) { mutableIntStateOf(0) }
  var permissionDenied by remember { mutableStateOf(false) }
  var recordingSeconds by remember { mutableIntStateOf(0) }

  LaunchedEffect(isRecording) {
    if (isRecording) {
      recordingSeconds = 0
      while (true) {
        delay(1000L)
        recordingSeconds++
      }
    } else {
      recordingSeconds = 0
    }
  }

  val targetSentence = lesson.sentences.getOrNull(selectedIdx)
  val targetFarsi = targetSentence?.farsi ?: lesson.words.firstOrNull()?.farsi.orEmpty()
  val targetFinglish = targetSentence?.finglish ?: lesson.words.firstOrNull()?.finglish.orEmpty()
  val targetEnglish = targetSentence?.english ?: lesson.words.firstOrNull()?.english.orEmpty()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      permissionDenied = false
      voiceRecorder.startRecording()
    } else {
      permissionDenied = true
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    if (!persianVoiceAvailable) {
      PersianTtsFallbackBanner(onRecheckVoice = onRecheckTts)
    }

    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "100% On-Device Privacy: Your voice stays on your phone and is never uploaded.",
          style = MaterialTheme.typography.labelMedium
        )
      }
    }

    if (lesson.sentences.size > 1) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        lesson.sentences.forEachIndexed { idx, _ ->
          FilledTonalButton(
            onClick = {
              selectedIdx = idx
              voiceRecorder.clearRecording()
            },
            colors = if (selectedIdx == idx) ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            ) else ButtonDefaults.filledTonalButtonColors()
          ) {
            Text("Line ${idx + 1}")
          }
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        FarsiText(
          text = targetFarsi,
          fontSize = 26.sp,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = targetFinglish,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = targetEnglish,
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
          onClick = { onSpeak(targetFarsi, targetSentence?.speaker ?: "STANDARD") },
          modifier = Modifier.height(48.dp)
        ) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("1. Hear Sample Voice")
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = {
            if (isRecording) {
              voiceRecorder.stopRecording()
            } else {
              val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
              ) == PackageManager.PERMISSION_GRANTED
              if (hasMic) {
                voiceRecorder.startRecording()
              } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          },
          colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
          else ButtonDefaults.buttonColors(),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("speak_record_button"),
          shape = RoundedCornerShape(16.dp)
        ) {
          Icon(
            imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (isRecording) "Stop Recording (00:${recordingSeconds.toString().padStart(2, '0')})"
            else "2. Record Yourself"
          )
        }

        if (permissionDenied) {
          Text(
            text = "Microphone permission wasn't granted. You can still skip ahead or tap Record again to allow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
          )
        }

        if (recordingError != null) {
          Text(
            text = recordingError.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
          )
        }

        if (hasRecording && !isRecording) {
          FilledTonalButton(
            onClick = {
              if (isPlayingRecording) voiceRecorder.stopPlayback()
              else voiceRecorder.playRecording()
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("speak_playback_button")
          ) {
            Icon(
              imageVector = if (isPlayingRecording) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
              contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPlayingRecording) "Stop Playback" else "3. Play Your Recording & Compare")
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Self-Check Guide: Compare your recording with the sample — check stress on the final syllable, long vowels (â, u, i), and natural spoken rhythm.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(12.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PracticeStepContent(
  lesson: Lesson,
  onQuizFinished: (score: Int, total: Int) -> Unit
) {
  var qIndex by remember(lesson.id) { mutableIntStateOf(0) }
  var selectedOption by remember(lesson.id, qIndex) { mutableStateOf<Int?>(null) }
  var score by remember(lesson.id) { mutableIntStateOf(0) }

  val questions = lesson.quiz
  LaunchedEffect(lesson.id, questions.isEmpty()) {
    if (questions.isEmpty()) {
      onQuizFinished(1, 1)
    }
  }
  if (questions.isEmpty()) return

  val q = questions[qIndex]
  val answered = selectedOption != null

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Question ${qIndex + 1} of ${questions.size}",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = q.prompt,
          style = MaterialTheme.typography.titleLarge
        )
        if (q.promptFarsi.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          FarsiText(
            text = q.promptFarsi,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    q.options.forEachIndexed { idx, optionText ->
      val isSelected = selectedOption == idx
      val isCorrect = idx == q.correctIndex
      val containerColor = when {
        !answered -> MaterialTheme.colorScheme.surface
        isCorrect -> EmeraldSuccessContainer
        isSelected -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
      }
      val borderColor = when {
        !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        isCorrect -> EmeraldSuccess
        isSelected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
      }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
          .clickable(enabled = !answered) {
            selectedOption = idx
            if (idx == q.correctIndex) score++
          }
          .testTag("quiz_option_$idx"),
        color = containerColor
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = optionText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
          )
          if (answered && isCorrect) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct", tint = EmeraldSuccess)
          }
        }
      }
    }

    if (answered) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = q.explanation,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(14.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = {
          if (qIndex < questions.lastIndex) {
            qIndex++
          } else {
            onQuizFinished(score, questions.size)
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("quiz_next_button"),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(if (qIndex < questions.lastIndex) "Next Question" else "Finish Lesson")
      }
    }
  }
}

@Composable
private fun LessonCompleteContent(
  lesson: Lesson,
  score: Int,
  total: Int,
  onDone: () -> Unit
) {
  val ratio = if (total > 0) score.toFloat() / total else 1f
  val stars = when {
    ratio >= 0.9f -> 3
    ratio >= 0.7f -> 2
    else -> 1
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    FarsiText(
      text = "آفرین!",
      fontSize = 36.sp,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Lesson ${lesson.id} Complete!",
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      repeat(3) { idx ->
        Icon(
          imageVector = Icons.Rounded.Star,
          contentDescription = null,
          tint = if (idx < stars) StarGold else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(44.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Practice Score: $score / $total\n${lesson.words.size} vocabulary cards added to your offline Leitner Review Box!",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (lesson.isCheckpointAfter) {
      Spacer(modifier = Modifier.height(14.dp))
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(
          text = "Checkpoint ${lesson.id / 5} Milestone Reached!",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(28.dp))
    Button(
      onClick = onDone,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("lesson_complete_done_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text("Back to Lesson Path")
    }
  }
}
