package com.hizkia.homescreen.enforcement

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.data.SosmedStore
import com.hizkia.homescreen.data.UsageReader
import com.hizkia.homescreen.launcher.AppInfo
import com.hizkia.homescreen.launcher.AppRepository
import com.hizkia.homescreen.ui.HomeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LimitBg = Color(0xFF150E0D)
private val LimitText = Color(0xFFF5ECEA)
private val LimitMuted = Color(0xFFC9B5B1)
private val LimitRed = Color(0xFFF27C6B)

/** Layar yang muncul ketika jatah harian sosmed habis. */
class LimitActivity : ComponentActivity() {
    private var targetPkg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        targetPkg = intent.getStringExtra(EXTRA_PKG)
        setContent {
            HomeTheme { LimitContent(targetPkg = targetPkg, onHome = ::goHome) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetPkg = intent.getStringExtra(EXTRA_PKG)
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_PKG = "pkg"

        fun intent(context: Context, pkg: String): Intent =
            Intent(context, LimitActivity::class.java)
                .putExtra(EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

private data class LimitInfo(val limitMin: Int, val usedMs: Map<String, Long>)

@Composable
private fun LimitContent(targetPkg: String?, onHome: () -> Unit) {
    val context = LocalContext.current
    val apps by produceState(emptyList<AppInfo>()) { value = AppRepository.load(context) }
    val info by produceState<LimitInfo?>(null) {
        value = withContext(Dispatchers.IO) {
            val rules = SosmedStore.get(context).effective
            LimitInfo(rules.limitMin, UsageReader.foregroundTodayMs(context, rules.packages))
        }
    }
    val labels = remember(apps) { apps.associate { it.packageName to it.label } }
    val name = labels[targetPkg] ?: stringResource(R.string.this_app)

    BackHandler(onBack = onHome)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimitBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.limit_reached_title, name), color = LimitText, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
            Spacer(Modifier.height(16.dp))
            val i = info
            if (i != null) {
                val usedMin = i.usedMs.values.sum() / 60_000
                Text(
                    stringResource(R.string.limit_reached_body, usedMin, i.limitMin),
                    color = LimitMuted,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF211412), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    i.usedMs.entries.sortedByDescending { it.value }.forEach { (pkg, ms) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(labels[pkg] ?: pkg, color = LimitMuted, fontSize = 14.sp)
                            Text(stringResource(R.string.min_short, ms / 60_000), color = LimitMuted, fontSize = 14.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A2622)))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.total_social), color = LimitText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.social_usage, usedMin, i.limitMin), color = LimitRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF3EF), contentColor = Color(0xFF0E1311)),
        ) {
            Text(stringResource(R.string.back_to_home), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.loosen_note),
            color = LimitMuted,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
