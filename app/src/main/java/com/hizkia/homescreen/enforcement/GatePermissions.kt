package com.hizkia.homescreen.enforcement

import android.app.AppOpsManager
import androidx.annotation.StringRes
import com.hizkia.homescreen.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/** Izin yang dibutuhkan gate bank. Sengaja bukan Aksesibilitas: app bank (mis. myBCA) menolak jalan kalau itu menyala. */
enum class GatePermission(@StringRes val labelRes: Int) {
    UsageAccess(R.string.perm_usage_access),
    Overlay(R.string.perm_overlay),
}

fun missingGatePermission(context: Context): GatePermission? = when {
    !hasUsageAccess(context) -> GatePermission.UsageAccess
    !Settings.canDrawOverlays(context) -> GatePermission.Overlay
    else -> null
}

fun hasUsageAccess(context: Context): Boolean {
    val ops = context.getSystemService(AppOpsManager::class.java)
    val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

/** Bebas hemat baterai: tanpa ini Samsung bisa mematikan service gate diam-diam. */
fun isBatteryExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

fun batteryIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun settingsIntentFor(context: Context, permission: GatePermission): Intent = when (permission) {
    GatePermission.UsageAccess -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    GatePermission.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
}.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
