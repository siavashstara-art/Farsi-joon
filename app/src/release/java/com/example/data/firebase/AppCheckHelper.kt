package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private const val TAG = "AppCheckInit"

fun configureAppCheck(context: Context, @Suppress("UNUSED_PARAMETER") intent: Intent?) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
      PlayIntegrityAppCheckProviderFactory.getInstance()
    )
    Log.d(TAG, "PlayIntegrityAppCheckProviderFactory installed for production.")
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}
