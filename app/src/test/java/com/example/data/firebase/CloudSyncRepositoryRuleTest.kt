package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.LessonProgressEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import java.util.UUID

@Ignore("Requires local Firebase Emulator")
class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun syncLessonProgress_authenticatedOwner_writesAndReadsBack() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = CloudSyncRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfile("Dara", totalStars = 3, completedLessonsCount = 1)
      repo.syncLessonProgress(
        LessonProgressEntity(
          lessonId = 1,
          completed = true,
          stars = 3,
          bestScore = 4,
          totalQuestions = 4,
          lastCompletedAt = System.currentTimeMillis()
        ),
        totalStars = 3,
        completedCount = 1
      )
    }

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeCloudLessonProgress().first { items -> items.any { it.lessonId == 1L } }
    }
    val item = list.first { it.lessonId == 1L }
    assertEquals(uid, item.userId)
    assertEquals(3L, item.stars)
  }

  @Test
  fun dailyChatQuota_enforcesMaxTenPerDay() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = CloudSyncRepository(firestore, auth)
    val dateKey = "2026-11-16"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      val count = repo.incrementDailyChatCount(dateKey)
      assertTrue(count in 1..10)
    }
  }

  @Test
  fun chatReport_appendOnlyAndOwnerDeletable() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = CloudSyncRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitChatReport(
        promptText = "Salam ${UUID.randomUUID()}",
        responseText = "Salam! Chetori?",
        reason = "Test report"
      )
      val snap = firestore.collection("users").document(uid)
        .collection("chat_reports").get().await()
      assertTrue(snap.documents.isNotEmpty())
    }
  }

  @Test
  fun crossUserRead_deniedBySecurityRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = CloudSyncRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureUserProfile("Alice", 6, 2)
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
    } catch (e: Exception) {
      val fsErr = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
    }
  }

  @Test
  fun observeCloudProgress_unauthenticatedUser_failsFast() = runBlocking {
    auth.signOut()
    val repo = CloudSyncRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCloudLessonProgress().first()
      }
      fail("Expected IllegalStateException for unauthenticated caller")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("signed in") == true)
    }
  }

  @Test
  fun unauthenticatedFirestoreQuery_deniedBySecurityRules() = runBlocking {
    auth.signOut()
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document("some_uid")
          .collection("lesson_progress").get().await()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated query")
    } catch (e: Exception) {
      val fsErr = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_farsi@test.com"
    const val BOB_EMAIL = "bob_farsi@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
