package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.LessonProgressEntity
import com.example.model.CloudLessonProgress
import com.example.model.DailyChatUsage
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureUserProfile(
    displayName: String,
    totalStars: Int,
    completedLessonsCount: Int
  ) {
    val uid = requireUserId()
    val safeName = displayName.trim().ifBlank { "Farsi Learner" }.take(80)
    val docRef = db.collection("users").document(uid)
    try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "totalStars" to totalStars.coerceIn(0, 60).toLong(),
          "completedLessonsCount" to completedLessonsCount.coerceIn(0, 20).toLong(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "displayName" to safeName,
          "totalStars" to totalStars.coerceIn(0, 60).toLong(),
          "completedLessonsCount" to completedLessonsCount.coerceIn(0, 20).toLong(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  fun observeUserProfile(): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, OperationType.GET, docRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun syncLessonProgress(
    progress: LessonProgressEntity,
    totalStars: Int,
    completedCount: Int
  ) {
    val uid = requireUserId()
    val docId = "lesson_${progress.lessonId}"
    val docRef = db.collection("users").document(uid)
      .collection("lesson_progress").document(docId)
    try {
      val existing = docRef.get().await()
      val payload = mapOf(
        "userId" to uid,
        "lessonId" to progress.lessonId.toLong(),
        "completed" to progress.completed,
        "stars" to progress.stars.coerceIn(0, 3).toLong(),
        "bestScore" to progress.bestScore.coerceIn(0, 20).toLong(),
        "totalQuestions" to progress.totalQuestions.coerceIn(1, 20).toLong(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (!existing.exists()) {
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "completed" to progress.completed,
          "stars" to progress.stars.coerceIn(0, 3).toLong(),
          "bestScore" to progress.bestScore.coerceIn(0, 20).toLong(),
          "totalQuestions" to progress.totalQuestions.coerceIn(1, 20).toLong(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
      }
      // Update summary counts on profile if profile exists
      val profileRef = db.collection("users").document(uid)
      val profileSnap = profileRef.get().await()
      if (profileSnap.exists()) {
        profileRef.update(
          mapOf(
            "totalStars" to totalStars.coerceIn(0, 60).toLong(),
            "completedLessonsCount" to completedCount.coerceIn(0, 20).toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  fun observeCloudLessonProgress(): Flow<List<CloudLessonProgress>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("lesson_progress")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(CloudLessonProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun getDailyChatCount(dateKey: String): Int {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
      .collection("daily_usage").document(dateKey)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) 0 else (snap.getLong("messageCount") ?: 0L).toInt()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path, auth)
      throw e
    }
  }

  suspend fun incrementDailyChatCount(dateKey: String): Int {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
      .collection("daily_usage").document(dateKey)
    try {
      val snap = docRef.get().await()
      val nextCount = if (!snap.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "dateKey" to dateKey,
          "messageCount" to 1L,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        1
      } else {
        val current = (snap.getLong("messageCount") ?: 0L).toInt()
        val updated = (current + 1).coerceAtMost(10)
        docRef.update(
          mapOf(
            "messageCount" to updated.toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        updated
      }
      return nextCount
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  suspend fun submitChatReport(promptText: String, responseText: String, reason: String) {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("chat_reports")
    val docRef = colRef.document()
    val payload = mapOf(
      "userId" to uid,
      "promptText" to promptText.take(500),
      "responseText" to responseText.trim().ifBlank { "No text" }.take(2000),
      "reason" to reason.trim().ifBlank { "Inappropriate response" }.take(200),
      "createdAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteAllUserCloudData() {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      val subcollections = listOf("lesson_progress", "daily_usage", "chat_reports")
      for (sub in subcollections) {
        val docs = userRef.collection(sub).get().await()
        for (doc in docs.documents) {
          doc.reference.delete().await()
        }
      }
      val profileSnap = userRef.get().await()
      if (profileSnap.exists()) {
        userRef.delete().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path, auth)
      throw e
    }
  }
}
