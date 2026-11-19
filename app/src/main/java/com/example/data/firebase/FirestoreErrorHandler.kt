package com.example.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
  auth: FirebaseAuth = FirebaseAuth.getInstance()
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val currentUser = auth.currentUser
  val redactedPath = path?.replace(Regex("users/[^/]+"), "users/[redacted]")
  val authInfoJson = JSONObject().apply {
    put("isSignedIn", currentUser != null)
    put("emailVerified", currentUser?.isEmailVerified)
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", redactedPath)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}
