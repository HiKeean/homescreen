package com.hizkia.homescreen.enforcement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.hizkia.homescreen.R
import com.hizkia.homescreen.data.BankPrefs
import com.hizkia.homescreen.data.GrantStore
import com.hizkia.homescreen.data.SosmedStore
import com.hizkia.homescreen.data.UsageReader
import com.hizkia.homescreen.policy.BlockReason
import com.hizkia.homescreen.policy.Decision
import com.hizkia.homescreen.policy.PolicyEngine
import java.time.LocalDate

/**
 * Memantau app yang sedang di depan lewat UsageStats (tanpa Aksesibilitas, karena app bank menolaknya).
 * - App bank tanpa izin berlaku: buka layar gate (catat pengeluaran dulu). Selama user tetap di app bank,
 *   izinnya diperpanjang supaya sesi yang sedang berjalan tidak terputus.
 * - App sosmed yang jatah hariannya habis: buka layar limit.
 */
class GateService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastQueryAt = 0L
    private var lastGateAt = 0L
    private var currentPkg: String? = null
    private var lastEvaluatedPkg: String? = null
    private var usageCachedAt = 0L
    private var usageCachedMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        lastQueryAt = System.currentTimeMillis()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        // Layar mati = tidak ada yang perlu dijaga; hemat baterai.
        if (!getSystemService(PowerManager::class.java).isInteractive) {
            lastQueryAt = System.currentTimeMillis()
            return
        }
        val now = System.currentTimeMillis()
        val from = maxOf(lastQueryAt - OVERLAP_MS, now - MAX_LOOKBACK_MS)
        lastQueryAt = now

        latestResumedPackage(from, now)?.let { currentPkg = it }
        val pkg = currentPkg ?: return
        if (pkg == packageName) return

        val banks = BankPrefs.get(this)
        val storedRules = SosmedStore.get(this, now).effective
        // Dijeda sementara lewat DevFlags: limit sosmed diabaikan, gate bank tetap jalan.
        val rules = if (DevFlags.sosmedLimitPausedToday()) storedRules.copy(packages = emptySet()) else storedRules
        if (pkg !in banks && pkg !in rules.packages) return

        // Hitung ulang pemakaian saat pindah app, lalu tiap USAGE_TTL_MS selama masih di app itu.
        val pkgChanged = pkg != lastEvaluatedPkg
        lastEvaluatedPkg = pkg
        val engine = PolicyEngine(
            bankPackages = banks,
            sosmed = rules,
            sosmedUsedMs = {
                if (pkgChanged || now - usageCachedAt > USAGE_TTL_MS) {
                    usageCachedMs = UsageReader.totalTodayMs(this, rules.packages, now)
                    usageCachedAt = now
                }
                usageCachedMs
            },
            grantExpiryMs = { GrantStore.expiry(this, it) },
        )

        when (val decision = engine.decide(pkg, now, LocalDate.now().dayOfWeek.value)) {
            is Decision.Allow -> if (pkg in banks) GrantStore.extend(this, pkg, now)
            is Decision.Block -> if (now - lastGateAt > DEBOUNCE_MS) {
                lastGateAt = now
                startActivity(
                    when (decision.reason) {
                        BlockReason.BANK_NEEDS_EXPENSE -> GateActivity.intent(this, pkg)
                        BlockReason.SOSMED_LIMIT -> LimitActivity.intent(this, pkg)
                    },
                )
            }
        }
    }

    private fun latestResumedPackage(from: Long, to: Long): String? {
        val events = getSystemService(UsageStatsManager::class.java).queryEvents(from, to)
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) latest = event.packageName
        }
        return latest
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.gate_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.gate_active))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    companion object {
        private const val CHANNEL_ID = "gate"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 700L
        private const val OVERLAP_MS = 1_000L
        private const val MAX_LOOKBACK_MS = 5_000L
        private const val DEBOUNCE_MS = 1_500L
        private const val USAGE_TTL_MS = 4_000L

        /** Layak jalan kalau ada yang dijaga (bank atau sosmed) dan izin lengkap. */
        fun shouldRun(context: Context): Boolean =
            (BankPrefs.get(context).isNotEmpty() || SosmedStore.get(context).effective.packages.isNotEmpty()) &&
                missingGatePermission(context) == null

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GateService::class.java))
        }
    }
}
