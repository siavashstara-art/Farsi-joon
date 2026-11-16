package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.firebase.onGoogleSignInClicked
import com.example.model.WordItem
import com.example.ui.theme.VazirmatnFontFamily

/**
 * Renders Persian script with true RTL layout direction and the Vazirmatn font family.
 */
@Composable
fun FarsiText(
  text: String,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 24.sp,
  fontWeight: FontWeight = FontWeight.Bold,
  color: Color = MaterialTheme.colorScheme.onSurface,
  textAlign: TextAlign = TextAlign.Start
) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Text(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      style = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = fontSize * 1.45f,
        textDirection = TextDirection.Rtl
      )
    )
  }
}

@Composable
fun ThreeFormWordCard(
  word: WordItem,
  dimFinglish: Boolean,
  hideFinglishDefault: Boolean,
  onSpeak: () -> Unit,
  modifier: Modifier = Modifier
) {
  var localRevealFinglish by remember(word.id, hideFinglishDefault) {
    mutableStateOf(!hideFinglishDefault)
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        FarsiText(
          text = word.farsi,
          fontSize = 26.sp,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (localRevealFinglish) {
            Text(
              text = word.finglish,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.alpha(if (dimFinglish) 0.58f else 1f)
            )
          } else {
            Text(
              text = "Finglish hidden",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
          }
          if (dimFinglish) {
            IconButton(
              onClick = { localRevealFinglish = !localRevealFinglish },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("toggle_finglish_${word.id}")
            ) {
              Icon(
                imageVector = if (localRevealFinglish) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (localRevealFinglish) "Hide Finglish" else "Show Finglish",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        Text(
          text = word.english,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (word.note.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = word.note,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      FilledTonalIconButton(
        onClick = onSpeak,
        modifier = Modifier
          .size(52.dp)
          .testTag("speak_word_${word.id}")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
          contentDescription = "Listen to ${word.english}"
        )
      }
    }
  }
}

@Composable
fun GoogleSignInButton(
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Sign in with Google"
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }

  Button(
    onClick = {
      isLoading = true
      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isLoading = false
          onAuthSuccess()
        },
        onAuthError = { msg ->
          isLoading = false
          onAuthError(msg)
        },
        scope = scope,
        onAuthCancelled = { isLoading = false }
      )
    },
    enabled = !isLoading,
    modifier = modifier
      .height(52.dp)
      .testTag("google_sign_in_button"),
    shape = RoundedCornerShape(16.dp)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
      )
    } else {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge
      )
    }
  }
}
