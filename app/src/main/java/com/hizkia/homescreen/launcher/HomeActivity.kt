package com.hizkia.homescreen.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.hizkia.homescreen.enforcement.GateService
import com.hizkia.homescreen.ui.HomeTheme

class HomeActivity : ComponentActivity() {
    // Naik tiap kali Home kembali ke depan, untuk menyegarkan status izin (mis. setelah dari Setelan).
    private var resumeTick by mutableIntStateOf(0)

    // Naik tiap kali tombol Home ditekan saat Homescreen sudah terbuka (mis. sedang di Setelan atau Semua app).
    private var homeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeTheme { LauncherRoot(resumeTick = resumeTick, homeTick = homeTick) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Tombol Home: kembali ke layar Home, halaman pertama.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeTick++
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
        // Hidupkan gate kalau ada yang dijaga (bank/sosmed) dan semua izin lengkap.
        if (GateService.shouldRun(this)) GateService.start(this)
    }
}
