package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LessonProgressEntity
import com.example.model.Lesson
import com.example.ui.components.FarsiText
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.StarGold
import com.example.viewmodel.LearningUiState

@Composable
fun LessonsTabScreen(
  uiState: LearningUiState,
  onSelectLesson: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (uiState.isLoading) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val nextLessonId = uiState.lessons.firstOrNull { uiState.progressMap[it.id]?.completed != true }?.id ?: 1

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    item {
      ProgressHeroCard(
        completedCount = uiState.completedCount,
        totalCount = uiState.lessons.size,
        totalStars = uiState.totalStars,
        completionPercent = uiState.completionPercent,
        dueReviewCount = uiState.dueCardsCount,
        nextLesson = uiState.lessons.firstOrNull { it.id == nextLessonId },
        onContinueNext = { onSelectLesson(nextLessonId) }
      )
    }

    items(uiState.lessons, key = { it.id }) { lesson ->
      val progress = uiState.progressMap[lesson.id]
      val isCurrent = lesson.id == nextLessonId
      Column(modifier = Modifier.widthIn(max = 600.dp)) {
        LessonPathCard(
          lesson = lesson,
          progress = progress,
          isCurrent = isCurrent,
          onClick = { onSelectLesson(lesson.id) }
        )
        if (lesson.isCheckpointAfter) {
          Spacer(modifier = Modifier.height(10.dp))
          CheckpointBanner(
            checkpointNumber = lesson.id / 5,
            unlocked = progress?.completed == true
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun ProgressHeroCard(
  completedCount: Int,
  totalCount: Int,
  totalStars: Int,
  completionPercent: Int,
  dueReviewCount: Int,
  nextLesson: Lesson?,
  onContinueNext: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
          )
        )
        .padding(20.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Your 20-Lesson Path",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
              text = "$completedCount of $totalCount lessons • $completionPercent% complete",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "Stars earned",
                tint = StarGold,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$totalStars / ${totalCount * 3}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
          progress = { (completionPercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
          color = StarGold,
          trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
        )

        if (nextLesson != null) {
          Spacer(modifier = Modifier.height(14.dp))
          Surface(
            onClick = onContinueNext,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("continue_next_lesson_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "UP NEXT • LESSON ${nextLesson.id}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = nextLesson.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Start Lesson ${nextLesson.id}",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
              }
            }
          }
        }

        if (dueReviewCount > 0) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "$dueReviewCount Leitner flashcards ready in Review tab (100% offline)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
          )
        }
      }
    }
  }
}

@Composable
private fun LessonPathCard(
  lesson: Lesson,
  progress: LessonProgressEntity?,
  isCurrent: Boolean,
  onClick: () -> Unit
) {
  val completed = progress?.completed == true
  val stars = progress?.stars ?: 0

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("lesson_card_${lesson.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      else MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            when {
              completed -> EmeraldSuccess
              isCurrent -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.surfaceVariant
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        if (completed) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Completed",
            tint = MaterialTheme.colorScheme.onPrimary
          )
        } else {
          Text(
            text = "${lesson.id}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = lesson.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (lesson.isAlphabetLesson) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "Alphabet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        FarsiText(
          text = lesson.farsiTitle,
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = lesson.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
          Icon(
            imageVector = if (index < stars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            contentDescription = null,
            tint = if (index < stars) StarGold else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun CheckpointBanner(
  checkpointNumber: Int,
  unlocked: Boolean
) {
  Surface(
    color = if (unlocked) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.AutoStories,
        contentDescription = null,
        tint = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = if (unlocked) "Checkpoint $checkpointNumber Passed! Milestone unlocked"
        else "Checkpoint $checkpointNumber • Complete Lesson ${checkpointNumber * 5} to unlock milestone",
        style = MaterialTheme.typography.labelLarge,
        color = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
