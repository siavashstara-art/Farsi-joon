package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FarsiText
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val farsiGreeting: String,
  val title: String,
  val body: String,
  val icon: ImageVector,
  val imageRes: Int
)

@Composable
fun OnboardingScreen(
  onFinish: () -> Unit
) {
  val pages = listOf(
    OnboardingPage(
      farsiGreeting = "خوش آمدید!",
      title = "Spoken Persian first, reading & writing step by step",
      body = "Built for heritage learners who hear Persian at home and want to speak naturally, read the script, and connect with family.",
      icon = Icons.Rounded.GraphicEq,
      imageRes = R.drawable.img_onboarding_welcome
    ),
    OnboardingPage(
      farsiGreeting = "۱۰ دقیقه در روز",
      title = "10 minutes a day • 20 offline lessons + Leitner box",
      body = "Every word comes in three forms: Persian script, clean Finglish, and English. Master vocabulary offline with smart spaced repetition.",
      icon = Icons.Rounded.Style,
      imageRes = R.drawable.img_story_nowruz
    ),
    OnboardingPage(
      farsiGreeting = "سلام مامان‌بزرگ!",
      title = "Bilingual stories & your friendly AI tutor",
      body = "Listen sentence-by-sentence to cultural stories like Nowruz and Yaldâ, compare your voice on-device, and optionally sign in for daily Persian chat.",
      icon = Icons.Rounded.AutoAwesome,
      imageRes = R.drawable.img_story_grandma_call
    )
  )

  val pagerState = rememberPagerState(pageCount = { pages.size })
  val scope = rememberCoroutineScope()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .align(Alignment.Center)
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Farsi-Joon",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        TextButton(
          onClick = onFinish,
          modifier = Modifier.testTag("onboarding_skip_button")
        ) {
          Text("Skip")
        }
      }

      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) { pageIndex ->
        val page = pages[pageIndex]
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = page.title,
            modifier = Modifier
              .fillMaxWidth()
              .height(230.dp)
              .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
          )
          Spacer(modifier = Modifier.height(24.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              FarsiText(
                text = page.farsiGreeting,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(pages.size) { idx ->
          val active = pagerState.currentPage == idx
          Box(
            modifier = Modifier
              .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
              .clip(CircleShape)
              .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          if (pagerState.currentPage < pages.lastIndex) {
            scope.launch {
              pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
          } else {
            onFinish()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("onboarding_next_button"),
        shape = RoundedCornerShape(18.dp)
      ) {
        Text(
          text = if (pagerState.currentPage < pages.lastIndex) "Continue" else "Start Lesson 1",
          style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
