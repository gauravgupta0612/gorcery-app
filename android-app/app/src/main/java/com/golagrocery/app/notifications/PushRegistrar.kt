package com.golagrocery.app.notifications

import android.util.Log
import com.golagrocery.app.BuildConfig
import com.golagrocery.app.data.repository.EngagementRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Registers this device's FCM token with the backend after login (only if google-services.json was provided). */
object PushRegistrar {
    fun register(scope: CoroutineScope, repo: EngagementRepository, customer: Boolean) {
        if (!BuildConfig.FCM_ENABLED) return
        try {
            val fm = FirebaseMessaging.getInstance()
            fm.token.addOnSuccessListener { token -> scope.launch { repo.registerDevice(token) } }
            if (customer) fm.subscribeToTopic("all-customers")
        } catch (e: IllegalStateException) {
            Log.w("Gola", "Firebase not initialised: ${e.message}")
        }
    }
}
