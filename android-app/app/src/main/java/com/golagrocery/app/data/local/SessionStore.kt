package com.golagrocery.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.golagrocery.app.data.remote.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "gola_session")

data class Session(val accessToken: String, val refreshToken: String, val user: UserDto)
data class DeliveryLocation(val pincode: String, val label: String, val etaMinutes: Int)

/**
 * Persists the session and user preferences with DataStore and mirrors them in memory so the
 * OkHttp interceptor can read the access token synchronously.
 */
class SessionStore(private val context: Context, private val json: Json) {
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _location = MutableStateFlow<DeliveryLocation?>(null)
    val location: StateFlow<DeliveryLocation?> = _location.asStateFlow()

    var onboardingDone: Boolean = false
        private set

    private object Keys {
        val access = stringPreferencesKey("access")
        val refresh = stringPreferencesKey("refresh")
        val user = stringPreferencesKey("user")
        val pincode = stringPreferencesKey("pincode")
        val locLabel = stringPreferencesKey("loc_label")
        val eta = intPreferencesKey("eta")
        val onboarding = booleanPreferencesKey("onboarding_done")
    }

    /** Loads persisted state once at start-up (called from the splash screen). */
    suspend fun load() {
        val p: Preferences = context.dataStore.data.first()
        val a = p[Keys.access]; val r = p[Keys.refresh]; val u = p[Keys.user]
        _session.value = if (a != null && r != null && u != null) runCatching { Session(a, r, json.decodeFromString<UserDto>(u)) }.getOrNull() else null
        val pin = p[Keys.pincode]
        _location.value = pin?.let { DeliveryLocation(it, p[Keys.locLabel] ?: it, p[Keys.eta] ?: 30) }
        onboardingDone = p[Keys.onboarding] ?: false
    }

    suspend fun saveSession(s: Session) {
        _session.value = s
        context.dataStore.edit {
            it[Keys.access] = s.accessToken
            it[Keys.refresh] = s.refreshToken
            it[Keys.user] = json.encodeToString(s.user)
        }
    }

    suspend fun updateUser(user: UserDto) {
        val s = _session.value ?: return
        saveSession(s.copy(user = user))
    }

    suspend fun clearSession() {
        _session.value = null
        context.dataStore.edit {
            it.remove(Keys.access); it.remove(Keys.refresh); it.remove(Keys.user)
        }
    }

    suspend fun saveLocation(loc: DeliveryLocation) {
        _location.value = loc
        context.dataStore.edit {
            it[Keys.pincode] = loc.pincode; it[Keys.locLabel] = loc.label; it[Keys.eta] = loc.etaMinutes
        }
    }

    suspend fun setOnboardingDone() {
        onboardingDone = true
        context.dataStore.edit { it[Keys.onboarding] = true }
    }
}
