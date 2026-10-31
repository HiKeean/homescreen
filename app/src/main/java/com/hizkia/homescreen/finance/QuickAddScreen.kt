package com.hizkia.homescreen.finance

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.launcher.AppInfo
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

private val AmberDark = Color(0xFF2A2210)
private val OnAmber = Color(0xFF1A1405)
private const val CASH = "cash"

/**
 * @param banks app bank yang sudah di-set user. Kosong = baru bisa pilih Tunai sampai user mengatur bank.
 * @param onSave bank = null berarti tunai.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddScreen(
    banks: List<AppInfo>,
    onSave: (amount: Long, category: String, note: String, bank: AppInfo?) -> Unit,
    onManageBanks: (() -> Unit)?,
    onCancel: () -> Unit,
    initialMethod: String? = null,
    lockedLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    var digits by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var method by remember(initialMethod) { mutableStateOf(initialMethod) }
    var note by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val locale = appLocale()
    val thousands = remember(locale) { ThousandsTransformation(groupingSeparator(locale)) }
    val amount = digits.toLongOrNull() ?: 0L
    val bank = banks.firstOrNull { it.packageName == method }
    val canSave = amount > 0 && category != null && method != null

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.cancel_back),
            color = Palette.Muted,
            fontSize = 14.sp,
            modifier = Modifier.clickableNoRipple(onCancel),
        )
        if (lockedLabel != null) {
            Surface(color = AmberDark, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF4A3B14))) {
                Text(
                    text = stringResource(R.string.bank_locked, lockedLabel),
                    color = Palette.Amber,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Text(
            text = if (lockedLabel != null) stringResource(R.string.log_first_title) else stringResource(R.string.log_expense_title),
            color = Palette.Text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        // Bisa di-scroll supaya kolom catatan tidak ketutup keyboard; tombol Simpan tetap di bawah.
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.amount), color = Palette.Muted, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rp", color = Palette.Muted, fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                        BasicTextField(
                            value = digits,
                            onValueChange = { input -> digits = input.filter(Char::isDigit).take(12).trimStart('0') },
                            visualTransformation = thousands,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(color = Palette.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(Palette.Mint),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            decorationBox = { inner ->
                                if (digits.isEmpty()) Text("0", color = Palette.Border, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                inner()
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.category), color = Palette.Muted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categories.forEach { name ->
                        val selected = name == category
                        ChoiceChip(selected = selected, onClick = { category = name }) {
                            Text(
                                text = categoryLabel(name),
                                color = if (selected) Palette.Amber else Palette.Text,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.pay_with), color = Palette.Muted, fontSize = 13.sp)
                    Text(
                        text = when {
                            onManageBanks == null -> ""
                            banks.isEmpty() -> stringResource(R.string.set_banks)
                            else -> stringResource(R.string.edit_arrow)
                        },
                        color = Palette.Mint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickableNoRipple { onManageBanks?.invoke() },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    banks.forEach { app ->
                        val selected = method == app.packageName
                        ChoiceChip(selected = selected, onClick = { method = app.packageName }) {
                            BankBadge(app.icon, size = 24.dp)
                            Text(
                                text = app.label,
                                color = if (selected) Palette.Amber else Palette.Text,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    ChoiceChip(selected = method == CASH, onClick = { method = CASH }) {
                        Text(
                            text = stringResource(R.string.cash),
                            color = if (method == CASH) Palette.Amber else Palette.Text,
                            fontSize = 15.sp,
                            fontWeight = if (method == CASH) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(stringResource(R.string.note_optional), color = Palette.Muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Palette.Surface,
                    unfocusedContainerColor = Palette.Surface,
                    focusedTextColor = Palette.Text,
                    unfocusedTextColor = Palette.Text,
                    cursorColor = Palette.Mint,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = { onSave(amount, category!!, note.trim(), bank) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Amber,
                contentColor = OnAmber,
                disabledContainerColor = Palette.Surface,
                disabledContentColor = Palette.Muted,
            ),
        ) {
            Text(
                text = if (bank != null) stringResource(R.string.save_and_open, bank.label) else stringResource(R.string.save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChoiceChip(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) AmberDark else Palette.Surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
        modifier = Modifier.height(44.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

/** Menampilkan angka dengan pemisah ribuan titik (85000 -> 85.000). */
private class ThousandsTransformation(private val sep: Char) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val len = raw.length
        val out = StringBuilder()
        raw.forEachIndexed { i, c ->
            if (i > 0 && (len - i) % 3 == 0) out.append(sep)
            out.append(c)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset + (0 until offset).count { it > 0 && (len - it) % 3 == 0 }

            override fun transformedToOriginal(offset: Int): Int =
                out.take(offset).count { it != sep }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}
