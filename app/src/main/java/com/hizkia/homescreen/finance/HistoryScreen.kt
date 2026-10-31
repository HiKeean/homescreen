package com.hizkia.homescreen.finance

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.data.Expense
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun Expense.dateTime() = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())

fun todayExpenses(expenses: List<Expense>): List<Expense> {
    val today = LocalDate.now()
    return expenses.filter { it.dateTime().toLocalDate() == today }
}

fun todayTotal(expenses: List<Expense>): Long = todayExpenses(expenses).sumOf { it.amount }

fun timeLabel(expense: Expense): String = expense.dateTime().format(timeFormat)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    expenses: List<Expense>,
    bankIcons: Map<String, ImageBitmap>,
    onBack: () -> Unit,
    onDelete: (Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(expenses) { expenses.groupBy { it.dateTime().toLocalDate() }.toList() }
    val month = YearMonth.now()
    val locale = appLocale()
    val dayFormat = rememberDateFormatter("EEEEdMMMM")
    val monthTotal = remember(expenses) {
        expenses.filter { YearMonth.from(it.dateTime()) == month }.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.back_home), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.spending_title), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.today), color = Palette.Muted, fontSize = 13.sp)
                    Text(formatRupiah(todayTotal(expenses), locale), color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.this_month), color = Palette.Muted, fontSize = 13.sp)
                    Text(formatRupiah(monthTotal, locale), color = Palette.Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val byCategory = remember(expenses) { categoryTotals(expenses, month) }
        if (byCategory.isNotEmpty()) {
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.by_category), color = Palette.Muted, fontSize = 13.sp)
                    byCategory.forEach { c ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(categoryLabel(c.category), color = Palette.Text, fontSize = 14.sp)
                                Text(formatRupiah(c.amount, locale), color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxWidth().height(6.dp)
                                    .background(Palette.Border, RoundedCornerShape(3.dp)),
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    Modifier.fillMaxWidth(c.share.coerceIn(0.02f, 1f)).height(6.dp)
                                        .background(Palette.Mint, RoundedCornerShape(3.dp)),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (expenses.isEmpty()) {
            Text(stringResource(R.string.no_entries), color = Palette.Muted, fontSize = 15.sp)
        }

        LazyColumn(Modifier.weight(1f)) {
            grouped.forEach { (date, items) ->
                item(key = "h$date") {
                    Text(
                        text = date.format(dayFormat),
                        color = Palette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                items(items, key = { it.id }) { e ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .combinedClickable(onClick = {}, onLongClick = { onDelete(e) }),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BankBadge(e.bankPackage?.let(bankIcons::get), modifier = Modifier.padding(end = 8.dp))
                                Text(categoryLabel(e.category), color = Palette.Text, fontSize = 16.sp)
                            }
                            val sub = listOf(e.dateTime().format(timeFormat), e.note).filter { it.isNotBlank() }.joinToString(" · ")
                            Text(sub, color = Palette.Muted, fontSize = 13.sp)
                        }
                        Text(formatRupiah(e.amount, locale), color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (expenses.isNotEmpty()) {
            Text(stringResource(R.string.long_press_delete), color = Palette.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}
