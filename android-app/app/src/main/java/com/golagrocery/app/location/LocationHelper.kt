package com.golagrocery.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedAddress(val lat: Double, val lng: Double, val pincode: String?, val city: String?, val state: String?, val line: String?)

class LocationHelper(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Caller must hold ACCESS_COARSE/FINE_LOCATION. Returns null if location is unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun detect(): DetectedAddress? {
        val cts = CancellationTokenSource()
        val loc = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        } ?: return null
        return withContext(Dispatchers.IO) {
            val a = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale("en", "IN")).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
            }.getOrNull()
            DetectedAddress(loc.latitude, loc.longitude, a?.postalCode, a?.locality ?: a?.subAdminArea, a?.adminArea, a?.getAddressLine(0))
        }
    }
}
