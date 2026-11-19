package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LearningProgressRepository
import com.example.ui.components.FarsiText
import com.example.ui.theme.EmeraldSuccess
import com.example.viewmodel.LearningUiState
import com.example.viewmodel.LeitnerCardWithWord

@Composable
fun ReviewTabScreen(
  uiState: LearningUiState,
  onReviewCard: (wordId: String, isCorrect: Boolean) -> Unit,
  onSpeak: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inSession by remember { mutableStateOf(false) }
  var practiceAllMode by remember { mutableStateOf(false) }
  var sessionQueue by remember { mutableStateOf<List<LeitnerCardWithWord>>(emptyList()) }
  var sessionIndex by remember { mutableIntStateOf(0) }
  var isFlipped by remember { mutableStateOf(false) }

  val dueCards = remember(uiState.leitnerCards) {
    val now = System.currentTimeMillis()
    uiState.leitnerCards.filter { it.card.nextReviewAt <= now }
  }

  if (inSession && sessionQueue.isNotEmpty() && sessionIndex < sessionQueue.size) {
    val current = sessionQueue[sessionIndex]
    LeitnerSessionView(
      item = current,
      currentNumber = sessionIndex + 1,
      totalCount = sessionQueue.size,
      isFlipped = isFlipped,
      dimFinglish = current.card.lessonId >= 13,
      hideFinglish = uiState.preferences.hideFinglishAfter12 && current.card.lessonId >= 13,
      onFlip = { isFlipped = !isFlipped },
      onSpeak = { onSpeak(current.word.farsi, "STANDARD") },
      onAnswer = { isCorrect ->
        onReviewCard(current.card.wordId, isCorrect)
        isFlipped = false
        if (sessionIndex + 1 < sessionQueue.size) {
          sessionIndex++
        } else {
          inSession = false
          sessionIndex = 0
        }
      },
      onExit = {
        inSession = false
        isFlipped = false
      },
      modifier = modifier
    )
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.OfflineBolt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "Leitner Box • 100% Offline",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Spaced repetition across 5 boxes (1, 2, 3, 7, and 14 days). A missed card moves back to Box 1 so nothing slips away.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                practiceAllMode = false
                sessionQueue = dueCards
                sessionIndex = 0
                isFlipped = false
                inSession = dueCards.isNotEmpty()
              },
              enabled = dueCards.isNotEmpty(),
              modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("start_due_review_button"),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text(if (dueCards.isNotEmpty()) "Review Due (${dueCards.size})" else "All Caught Up!")
            }

            if (uiState.leitnerCards.isNotEmpty()) {
              OutlinedButton(
                onClick = {
                  practiceAllMode = true
                  sessionQueue = uiState.leitnerCards.shuffled()
                  sessionIndex = 0
                  isFlipped = false
                  inSession = true
                },
                modifier = Modifier
                  .height(50.dp)
                  .testTag("practice_all_cards_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Practice All")
              }
            }
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Your 5 Leitner Boxes (${uiState.leitnerCards.size} unlocked words)",
          style = MaterialTheme.typography.titleLarge
        )

        (1..5).forEach { box ->
          val count = uiState.boxCounts[box] ?: 0
          val days = LearningProgressRepository.intervalDaysForBox(box)
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "B$box",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }
                }
                Column {
                  Text(
                    text = "Box $box • Every $days ${if (days == 1) "day" else "days"}",
                    style = MaterialTheme.typography.titleMedium
                  )
                  Text(
                    text = if (box == 5) "Long-term Persian memory" else "Complete lessons to add words",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }

    if (uiState.leitnerCards.isEmpty()) {
      item {
        Column(
          modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 400.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Rounded.Style,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No flashcards yet — start Lesson 1 to unlock your first words!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun LeitnerSessionView(
  item: LeitnerCardWithWord,
  currentNumber: Int,
  totalCount: Int,
  isFlipped: Boolean,
  dimFinglish: Boolean,
  hideFinglish: Boolean,
  onFlip: () -> Unit,
  onSpeak: () -> Unit,
  onAnswer: (Boolean) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onExit)
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Card $currentNumber of $totalCount • Box ${item.card.boxLevel}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )
      OutlinedButton(onClick = onExit) {
        Text("Finish")
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp)
        .weight(1f)
        .clickable(onClick = onFlip)
        .testTag("leitner_flashcard"),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        AnimatedContent(targetState = isFlipped, label = "card_flip") { flipped ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            FarsiText(
              text = item.word.farsi,
              fontSize = 38.sp,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalIconButton(
              onClick = onSpeak,
              modifier = Modifier.size(56.dp)
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear pronunciation")
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!flipped) {
              Text(
                text = "Tap card to reveal Finglish & meaning",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            } else {
              if (!hideFinglish) {
                Text(
                  text = item.word.finglish,
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.secondary.copy(alpha = if (dimFinglish) 0.6f else 1f),
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
              Text(
                text = item.word.english,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )
              if (item.word.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = item.word.note,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Right: ${item.card.correctCount} • Missed: ${item.card.wrongCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = { onAnswer(false) },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .testTag("leitner_forgot_button"),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Rounded.Close, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Still Learning\n(Box 1)", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
      }
      Button(
        onClick = { onAnswer(true) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .testTag("leitner_knew_button"),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Rounded.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        val nextBox = (item.card.boxLevel + 1).coerceAtMost(5)
        Text("I Knew It\n(Box $nextBox)", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
