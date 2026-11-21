package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.audio.LocalVoiceRecorder
import com.example.data.firebase.CloudSyncRepository
import com.example.data.firebase.signOut
import com.example.data.local.FarsiDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.ui.components.GoogleSignInButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
  currentUser: FirebaseUser?,
  preferences: UserPreferences,
  onSelectSection: ((Int) -> Unit)? = null,
  onThemeChanged: (ThemeMode) -> Unit,
  onToggleHideFinglishAfter12: (Boolean) -> Unit,
  onToggleCalmFocusMode: (Boolean) -> Unit = {},
  onToggleDailyReminder: (Boolean) -> Unit,
  onSignedInSync: () -> Unit,
  onClearLocalProgress: () -> Unit,
  onAccountDeletedCleanup: () -> Unit = onClearLocalProgress,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isDeleting by remember { mutableStateOf(false) }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      onToggleDailyReminder(true)
    } else {
      statusMessage = "Notification permission was denied. Daily reminders stay off."
    }
  }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        text = "Menu, Account & Settings",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() }
      )

      if (onSelectSection != null) {
        Text(
          text = "Quick Navigation",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.semantics { heading() }
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("Lessons", "Review", "Stories", "Chat").forEachIndexed { idx, label ->
            FilterChip(
              selected = false,
              onClick = { onSelectSection(idx) },
              label = { Text(label) },
              modifier = Modifier.testTag("menu_jump_${label.lowercase()}")
            )
          }
        }
        HorizontalDivider()
      }

      if (statusMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = statusMessage.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Theme Selection
      Text(
        text = "Appearance",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
          FilterChip(
            selected = preferences.themeMode == mode,
            onClick = { onThemeChanged(mode) },
            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
          )
        }
      }

      // Finglish Preference
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Hide Finglish from Lesson 13+", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Encourages natural Persian script reading",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = preferences.hideFinglishAfter12,
          onCheckedChange = onToggleHideFinglishAfter12
        )
      }

      // Daily Notification Reminder
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Daily 10-Min Reminder (7:00 PM)", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Local notification on your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = preferences.dailyReminderEnabled,
          onCheckedChange = { enable ->
            if (!enable) {
              onToggleDailyReminder(false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
              ) == PackageManager.PERMISSION_GRANTED
              if (granted) onToggleDailyReminder(true)
              else notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onToggleDailyReminder(true)
            }
          }
        )
      }

      // Calm Focus & Reduced Motion (ADHD / Vestibular Accessibility)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Calm Focus & Reduced Motion", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "ADHD-friendly focus view with minimal animations",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = preferences.calmFocusMode,
          onCheckedChange = onToggleCalmFocusMode,
          modifier = Modifier.testTag("settings_calm_focus_switch")
        )
      }

      HorizontalDivider()

      // Account & Cloud Data
      Text(
        text = "Account & Data Safety",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
      )
      if (currentUser == null) {
        Text(
          text = "Not signed in. Offline lessons and Leitner progress are saved locally on this device.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GoogleSignInButton(
          onAuthSuccess = {
            onSignedInSync()
            statusMessage = "Signed in with Google! Cloud sync & AI Chat unlocked."
          },
          onAuthError = { statusMessage = it },
          label = "Continue with Google / Gmail",
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = currentUser.displayName ?: "Signed-in Learner",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = currentUser.email ?: currentUser.uid,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = {
              signOut(credentialManager, scope) {
                statusMessage = "Signed out."
              }
            },
            modifier = Modifier
              .weight(1f)
              .testTag("settings_sign_out_button")
          ) {
            Text("Sign Out")
          }
          Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            enabled = !isDeleting,
            modifier = Modifier
              .weight(1f)
              .testTag("settings_delete_account_button")
          ) {
            Text("Delete Account")
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = { showPrivacyDialog = true },
          modifier = Modifier.weight(1f)
        ) {
          Text("Privacy & Data Safety")
        }
        OutlinedButton(
          onClick = {
            onClearLocalProgress()
            statusMessage = "Local progress cleared."
          },
          modifier = Modifier.weight(1f)
        ) {
          Text("Reset Local Data")
        }
      }

      Text(
        text = "Farsi Joon v${BuildConfig.VERSION_NAME} • Support: siavashstara@gmail.com",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Account & All Data?") },
      text = {
        Text("This permanently deletes your Firebase Auth account, cloud progress, daily chat quota, chat reports, and local lesson progress.")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
          onClick = {
            showDeleteConfirm = false
            isDeleting = true
            scope.launch {
              withContext(NonCancellable) {
                try {
                  val user = Firebase.auth.currentUser
                  if (user == null) {
                    statusMessage = "Please sign in first to delete your account."
                    return@withContext
                  }
                  val lastSignInMs = user.metadata?.lastSignInTimestamp ?: 0L
                  if (lastSignInMs > 0L && (System.currentTimeMillis() - lastSignInMs) > 4 * 60_000L) {
                    statusMessage = "For your security, please sign out and sign in again before deleting your account."
                    return@withContext
                  }

                  // 1. Delete Firestore cloud data
                  val repo = CloudSyncRepository(context)
                  repo.deleteAllUserCloudData()

                  // 2. Clear Room database user data (without re-inserting starter cards)
                  val db = FarsiDatabase.getInstance(context)
                  db.farsiDao().clearLessonProgress()
                  db.farsiDao().clearLeitnerCards()
                  onAccountDeletedCleanup()

                  // 3. Clear DataStore preferences
                  val prefsRepo = PreferencesRepository(context)
                  prefsRepo.resetPreferences()

                  // 4. Delete locally recorded .m4a files and cache files
                  val recorder = LocalVoiceRecorder(context)
                  recorder.clearRecording()
                  runCatching {
                    context.cacheDir.listFiles()?.forEach { file ->
                      if (file.name.contains("farsi") || file.name.endsWith(".m4a")) {
                        file.delete()
                      }
                    }
                  }

                  // 5. Delete Firebase Authentication account
                  user.delete().await()

                  // 6. Clear Credential Manager state
                  runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }

                  statusMessage = "Account and all associated local and cloud data deleted successfully."
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                  statusMessage = "Security policy requires recent sign-in. Please sign out and sign in again before deleting your account."
                } catch (e: Exception) {
                  statusMessage = e.localizedMessage ?: "Failed to complete account deletion. Please try again."
                } finally {
                  isDeleting = false
                }
              }
            }
          }
        ) {
          Text("Delete Everything")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy & Data Safety") },
      text = {
        Text(
          "• Offline Lessons & Leitner Box: Stored locally on your device.\n" +
            "• Speak Practice Microphone: Recorded temporarily in local cache for self-playback only. Never uploaded.\n" +
            "• Google Sign-In & Cloud Sync: Optional, required only to unlock the 10-message/day Gemini Flash Persian tutor.\n" +
            "• Account Deletion: Deletes your Firebase account and all cloud & local records immediately."
        )
      },
      confirmButton = {
        Button(onClick = { showPrivacyDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}
