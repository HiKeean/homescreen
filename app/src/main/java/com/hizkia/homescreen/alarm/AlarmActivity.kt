package com.hizkia.homescreen.alarm

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.HomeTheme
import com.hizkia.homescreen.ui.Palette
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Layar penuh alarm: tampil di atas kunci layar, menyalakan layar, dan menutup sendiri saat alarm berhenti. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty()
        val snoozeMin = intent.getIntExtra(AlarmService.EXTRA_SNOOZE_MIN, 5)
        setContent {
            HomeTheme {
                AlarmContent(title = title, text = text, snoozeMin = snoozeMin, onClose = { finish() })
            }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun AlarmContent(title: String, text: String, snoozeMin: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val ringing by AlarmState.ringing.collectAsState()
    val wasRinging = remember { mutableStateOf(false) }
    val now by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }

    // Tutup layar ini kalau alarm dimatikan dari tempat lain (mis. tombol di notifikasi).
    LaunchedEffect(ringing) {
        if (ringing) wasRinging.value = true else if (wasRinging.value) onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(now.format(timeFormat), color = Palette.Text, fontSize = 88.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Text(title.ifBlank { stringResource(R.string.alarm) }, color = Palette.Amber, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(text, color = Palette.Muted, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { AlarmService.command(context, AlarmService.CMD_STOP); onClose() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
            ) {
                Text(stringResource(R.string.stop), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { AlarmService.command(context, AlarmService.CMD_SNOOZE); onClose() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Surface, contentColor = Palette.Text),
            ) {
                Text(stringResource(R.string.snooze_minutes, snoozeMin), fontSize = 16.sp)
            }
        }
    }
}
