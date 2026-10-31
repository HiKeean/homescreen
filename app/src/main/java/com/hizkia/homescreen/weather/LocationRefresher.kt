package com.hizkia.homescreen.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.AppLocale
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mengambil lokasi perkiraan (kota) untuk cuaca. Hanya izin ACCESS_COARSE_LOCATION dan hanya saat dipanggil
 * dari layar depan; tidak ada pemantauan lokasi di latar belakang. Koordinat dibulatkan ke 2 desimal (~1 km).
 */
object LocationRefresher {
    private const val MAX_AGE_MS = 12L * 60 * 60 * 1000
    private const val FIX_TIMEOUT_MS = 10_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(context: Context): City? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null
        val lm = context.getSystemService(LocationManager::class.java)
        val loc = freshLastKnown(lm) ?: currentFix(context, lm) ?: return@withContext null
        val lat = LocationMath.round2(loc.latitude)
        val lon = LocationMath.round2(loc.longitude)
        City(name = reverseName(context, lat, lon) ?: context.getString(R.string.current_location_name), region = "", country = "", lat = lat, lon = lon)
    }

    private fun freshLastKnown(lm: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000 < MAX_AGE_MS }
            .maxByOrNull { it.elapsedRealtimeNanos }

    private suspend fun currentFix(context: Context, lm: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                runCatching {
                    lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancel, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseName(context: Context, lat: Double, lon: Double): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        Geocoder(context, AppLocale.of(context)).getFromLocation(lat, lon, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()
}

object LocationMath {
    private const val MOVED_KM = 3.0

    fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    /** Pindah cukup jauh untuk dianggap kota/tempat baru (dan cache cuaca dibuang). */
    fun moved(oldLat: Double, oldLon: Double, newLat: Double, newLon: Double): Boolean =
        com.hizkia.homescreen.alerts.AlertEvaluator.distanceKm(oldLat, oldLon, newLat, newLon) >= MOVED_KM
}
