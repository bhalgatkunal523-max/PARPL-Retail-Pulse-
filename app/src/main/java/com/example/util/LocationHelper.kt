package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHelper {

    /**
     * Calculates geodesic distance between two points in meters using Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Checks if coordinates fall within geofence radius.
     */
    fun isWithinGeofence(
        userLat: Double,
        userLon: Double,
        branchLat: Double,
        branchLon: Double,
        radiusMeters: Double
    ): Boolean {
        // If branch lat/long is 0.0 (unconfigured), allow for setup/testing
        if (branchLat == 0.0 && branchLon == 0.0) return true
        val distance = calculateDistanceMeters(userLat, userLon, branchLat, branchLon)
        return distance <= radiusMeters
    }

    /**
     * Fetches current GPS coordinates asynchronously.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val tokenSource = CancellationTokenSource()
            suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { last ->
                                continuation.resume(last)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
                continuation.invokeOnCancellation {
                    tokenSource.cancel()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
