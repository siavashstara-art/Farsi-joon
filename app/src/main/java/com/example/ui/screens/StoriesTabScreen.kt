package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.PersianAudioPlayer
import com.example.audio.VoiceProfile
import com.example.model.Story
import com.example.ui.components.FarsiText
import com.example.ui.components.PersianTtsFallbackBanner

private fun storyDrawable(name: String): Int = when (name) {
  "img_story_nowruz" -> R.drawable.img_story_nowruz
  "img_story_yalda" -> R.drawable.img_story_yalda
  "img_story_grandma_call" -> R.drawable.img_story_grandma_call
  else -> R.drawable.img_onboarding_welcome
}

@Composable
fun StoriesTabScreen(
  stories: List<Story>,
  onSelectStory: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    item {
      Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
        Text(
          text = "Bilingual Audio Stories",
          style = MaterialTheme.typography.headlineSmall
        )
        Text(
          text = "Listen sentence-by-sentence in spoken Persian, follow along in Finglish & English.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    items(stories, key = { it.id }) { story ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .clickable { onSelectStory(story.id) }
          .testTag("story_card_${story.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column {
          Image(
            painter = painterResource(id = storyDrawable(story.imageName)),
            contentDescription = story.title,
            modifier = Modifier
              .fillMaxWidth()
              .height(165.dp),
            contentScale = ContentScale.Crop
          )
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = story.level,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )
              if (story.hasTwoVoices) {
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.RecordVoiceOver,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                      text = "2 Voices",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = story.title,
              style = MaterialTheme.typography.titleLarge
            )
            FarsiText(
              text = story.farsiTitle,
              fontSize = 20.sp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${story.sentences.size} sentences • ${story.durationLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
  story: Story,
  audioPlayer: PersianAudioPlayer,
  onBack: () -> Unit
) {
  var activeSentenceIdx by remember(story.id) { mutableIntStateOf(0) }
  var isAutoPlaying by remember(story.id) { mutableStateOf(false) }
  var showFinglish by remember { mutableStateOf(true) }
  var showEnglish by remember { mutableStateOf(true) }
  val persianVoiceAvailable by audioPlayer.persianVoiceAvailable.collectAsStateWithLifecycle()

  BackHandler {
    audioPlayer.stop()
    onBack()
  }

  DisposableEffect(story.id) {
    onDispose {
      audioPlayer.stop()
    }
  }

  fun playSentenceAt(index: Int, autoAdvance: Boolean) {
    val sentence = story.sentences.getOrNull(index) ?: run {
      isAutoPlaying = false
      return
    }
    activeSentenceIdx = index
    val profile = when (sentence.speaker.uppercase()) {
      "GRANDMA" -> VoiceProfile.GRANDMA
      "NIMA" -> VoiceProfile.NIMA
      else -> VoiceProfile.STANDARD
    }
    audioPlayer.speak(
      farsiText = sentence.farsi,
      voiceProfile = profile,
      utteranceId = "${story.id}_$index",
      onDone = {
        if (autoAdvance && index + 1 < story.sentences.size) {
          playSentenceAt(index + 1, true)
        } else {
          isAutoPlaying = false
        }
      },
      onError = {
        isAutoPlaying = false
      }
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(story.title, style = MaterialTheme.typography.titleMedium)
            FarsiText(text = story.farsiTitle, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            audioPlayer.stop()
            onBack()
          }) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Stories")
          }
        }
      )
    },
    bottomBar = {
      Surface(tonalElevation = 4.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilledTonalIconButton(
            onClick = {
              val prev = (activeSentenceIdx - 1).coerceAtLeast(0)
              playSentenceAt(prev, isAutoPlaying)
            },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous sentence")
          }
          Spacer(modifier = Modifier.width(16.dp))
          Button(
            onClick = {
              if (isAutoPlaying) {
                isAutoPlaying = false
                audioPlayer.stop()
              } else {
                isAutoPlaying = true
                playSentenceAt(activeSentenceIdx, true)
              }
            },
            modifier = Modifier
              .height(52.dp)
              .testTag("story_play_all_button"),
            shape = RoundedCornerShape(16.dp)
          ) {
            Icon(
              imageVector = if (isAutoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isAutoPlaying) "Pause Story" else "Play Sentence-by-Sentence")
          }
          Spacer(modifier = Modifier.width(16.dp))
          FilledTonalIconButton(
            onClick = {
              val next = (activeSentenceIdx + 1).coerceAtMost(story.sentences.lastIndex)
              playSentenceAt(next, isAutoPlaying)
            },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next sentence")
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      item {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
          Image(
            painter = painterResource(id = storyDrawable(story.imageName)),
            contentDescription = story.title,
            modifier = Modifier
              .fillMaxWidth()
              .height(175.dp)
              .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
          )
          if (story.culturalNote.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = story.culturalNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = showFinglish,
              onClick = { showFinglish = !showFinglish },
              label = { Text("Finglish") }
            )
            FilterChip(
              selected = showEnglish,
              onClick = { showEnglish = !showEnglish },
              label = { Text("English") }
            )
          }
          if (!persianVoiceAvailable) {
            Spacer(modifier = Modifier.height(10.dp))
            PersianTtsFallbackBanner(onRecheckVoice = { audioPlayer.recheckVoiceAvailability() })
          }
        }
      }

      itemsIndexed(story.sentences, key = { _, s -> s.id }) { idx, sentence ->
        val isActive = idx == activeSentenceIdx
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(
              width = if (isActive) 2.dp else 0.dp,
              color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
              shape = RoundedCornerShape(18.dp)
            )
            .clickable {
              isAutoPlaying = false
              playSentenceAt(idx, false)
            },
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              if (story.hasTwoVoices) {
                Text(
                  text = sentence.speakerLabel,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (sentence.speaker == "GRANDMA") MaterialTheme.colorScheme.tertiary
                  else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
              }
              FarsiText(
                text = sentence.farsi,
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (showFinglish) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = sentence.finglish,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Medium
                )
              }
              if (showEnglish) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = sentence.english,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
              onClick = {
                isAutoPlaying = false
                playSentenceAt(idx, false)
              },
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
