package com.hizkia.homescreen.enforcement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hizkia.homescreen.data.AppDatabase
import com.hizkia.homescreen.data.AppScope
import com.hizkia.homescreen.data.BankPrefs
import com.hizkia.homescreen.data.Expense
import com.hizkia.homescreen.data.GrantStore
import com.hizkia.homescreen.finance.QuickAddScreen
import com.hizkia.homescreen.launcher.AppInfo
import com.hizkia.homescreen.launcher.AppRepository
import com.hizkia.homescreen.ui.HomeTheme
import com.hizkia.homescreen.ui.Palette
import kotlinx.coroutines.launch

/** Layar gate: muncul ketika app bank dibuka tanpa izin. Catat pengeluaran dulu, baru app bank dibuka. */
class GateActivity : ComponentActivity() {
    private var targetPkg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        targetPkg = intent.getStringExtra(EXTRA_PKG)
        setContent {
            HomeTheme {
                GateContent(targetPkg = targetPkg, onSaved = { finish() }, onCancel = ::goHome)
            }
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
            Intent(context, GateActivity::class.java)
                .putExtra(EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

@Composable
private fun GateContent(targetPkg: String?, onSaved: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).expenseDao() }
    val apps by produceState(emptyList<AppInfo>()) { value = AppRepository.load(context) }
    val bankPackages = remember { BankPrefs.get(context) }
    val banks = remember(apps) { apps.filter { it.packageName in bankPackages } }
    val locked = banks.firstOrNull { it.packageName == targetPkg }

    BackHandler(onBack = onCancel)

    QuickAddScreen(
        banks = banks,
        onSave = { amount, category, note, bank ->
            // AppScope, bukan scope layar: activity langsung ditutup dan tulisan DB tidak boleh batal.
            AppScope.launch {
                dao.insert(
                    Expense(
                        amount = amount,
                        category = category,
                        note = note,
                        createdAt = System.currentTimeMillis(),
                        bankPackage = bank?.packageName,
                    ),
                )
            }
            bank?.let {
                GrantStore.grant(context, it.packageName)
                AppRepository.launch(context, it.packageName)
            }
            onSaved()
        },
        onManageBanks = null,
        onCancel = onCancel,
        initialMethod = targetPkg,
        lockedLabel = locked?.label,
        modifier = Modifier.fillMaxSize().background(Palette.Bg),
    )
}
