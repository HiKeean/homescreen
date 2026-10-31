package com.hizkia.homescreen.launcher

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hizkia.homescreen.R
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hizkia.homescreen.agenda.AddEventScreen
import com.hizkia.homescreen.agenda.CalEvent
import com.hizkia.homescreen.agenda.EventDetailScreen
import com.hizkia.homescreen.alarm.ReminderMode
import com.hizkia.homescreen.apps.AppUsage
import com.hizkia.homescreen.apps.RareApps
import com.hizkia.homescreen.apps.RareAppsRepository
import com.hizkia.homescreen.apps.RareAppsScreen
import com.hizkia.homescreen.apps.RareRow
import com.hizkia.homescreen.apps.RareSort
import com.hizkia.homescreen.alarm.AlarmPrefs
import com.hizkia.homescreen.alarm.AlarmScheduler
import com.hizkia.homescreen.alarm.AlarmService
import com.hizkia.homescreen.alarm.AlarmSettingsScreen
import com.hizkia.homescreen.agenda.AgendaScreen
import com.hizkia.homescreen.agenda.CalendarRepository
import com.hizkia.homescreen.agenda.HomeAgendaPage
import com.hizkia.homescreen.agenda.overlaps
import com.hizkia.homescreen.agenda.startMs
import com.hizkia.homescreen.data.AppDatabase
import com.hizkia.homescreen.data.BankPrefs
import com.hizkia.homescreen.data.Expense
import com.hizkia.homescreen.data.GrantStore
import com.hizkia.homescreen.data.MapsPrefs
import com.hizkia.homescreen.data.PinnedPrefs
import com.hizkia.homescreen.maps.MapsLauncher
import com.hizkia.homescreen.alerts.Alert
import com.hizkia.homescreen.alerts.AlertKind
import com.hizkia.homescreen.alerts.AlertNotifier
import com.hizkia.homescreen.alerts.AlertPrefs
import com.hizkia.homescreen.alerts.AlertSettings
import com.hizkia.homescreen.alerts.AlertWorker
import com.hizkia.homescreen.settings.AlertSettingsScreen
import com.hizkia.homescreen.settings.MapsSettingsScreen
import com.hizkia.homescreen.settings.WeatherSettingsScreen
import com.hizkia.homescreen.weather.City
import com.hizkia.homescreen.weather.LocationRefresher
import com.hizkia.homescreen.weather.WeatherCode
import com.hizkia.homescreen.weather.WeatherRepository
import com.hizkia.homescreen.weather.WeatherStore
import com.hizkia.homescreen.weather.WeatherUi
import com.hizkia.homescreen.settings.QuickAppsScreen
import com.hizkia.homescreen.data.SosmedStore
import com.hizkia.homescreen.data.Todo
import com.hizkia.homescreen.data.UsageReader
import com.hizkia.homescreen.enforcement.GatePermission
import com.hizkia.homescreen.enforcement.batteryIntent
import com.hizkia.homescreen.enforcement.hasUsageAccess
import com.hizkia.homescreen.enforcement.isBatteryExempt
import com.hizkia.homescreen.enforcement.missingGatePermission
import com.hizkia.homescreen.enforcement.settingsIntentFor
import com.hizkia.homescreen.finance.HistoryScreen
import com.hizkia.homescreen.finance.QuickAddScreen
import com.hizkia.homescreen.finance.todayExpenses
import com.hizkia.homescreen.settings.AppPickerScreen
import com.hizkia.homescreen.settings.PermissionRow
import com.hizkia.homescreen.settings.RulesScreen
import com.hizkia.homescreen.settings.SettingsScreen
import com.hizkia.homescreen.todo.TodoScreen
import com.hizkia.homescreen.todo.groupTodos
import com.hizkia.homescreen.todo.startOfDayMs
import com.hizkia.homescreen.ui.Palette
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen {
    Home, Drawer, QuickAdd, History, Settings, Rules, BankPicker, SosmedPicker, Agenda, Todo, AddEvent, Alarm, EventDetail, Rare, MapsSettings, PinnedPicker, WeatherSettings, Alerts
}

private const val USAGE_REFRESH_MS = 30_000L

@Composable
fun LauncherRoot(resumeTick: Int, homeTick: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }
    val dao = remember { db.expenseDao() }
    val todoDao = remember { db.todoDao() }
    val expenses by dao.observeAll().collectAsState(initial = emptyList())
    val todos by todoDao.observeAll().collectAsState(initial = emptyList())
    // Naik setelah user menghapus app, supaya daftar app dimuat ulang tanpa menunggu app dibuka ulang.
    var appsVersion by remember { mutableIntStateOf(0) }
    var pendingUninstall by remember { mutableStateOf(false) }
    val apps by produceState(emptyList<AppInfo>(), appsVersion) { value = AppRepository.load(context) }
    var bankPackages by remember { mutableStateOf(BankPrefs.get(context)) }
    var rules by remember { mutableStateOf(SosmedStore.get(context)) }
    var screen by remember { mutableStateOf(Screen.Home) }
    // Dari mana layar pilih-app dibuka, supaya "kembali" mendarat di tempat yang benar.
    var pickerReturnsTo by remember { mutableStateOf(Screen.Home) }
    // Naik tiap kali izin mungkin berubah (kembali dari layar pengaturan sistem).
    var permissionTick by remember { mutableIntStateOf(0) }
    var calendarTick by remember { mutableIntStateOf(0) }
    var agendaDate by remember { mutableStateOf(LocalDate.now()) }
    var detailEvent by remember { mutableStateOf<CalEvent?>(null) }
    var detailReturnsTo by remember { mutableStateOf(Screen.Agenda) }
    val homePager = rememberPagerState(pageCount = { 2 })

    // Tombol Home ditekan: tutup layar apa pun yang sedang terbuka dan kembali ke Home halaman pertama.
    LaunchedEffect(homeTick) {
        if (homeTick > 0) {
            screen = Screen.Home
            homePager.scrollToPage(0)
        }
    }
    var alarmSettings by remember { mutableStateOf(AlarmPrefs.get(context)) }
    var alertSettings by remember { mutableStateOf(AlertPrefs.settings(context)) }
    var weatherCity by remember { mutableStateOf(WeatherStore.city(context)) }
    var weatherAuto by remember { mutableStateOf(WeatherStore.isAuto(context)) }
    var pinnedPackages by remember { mutableStateOf(PinnedPrefs.get(context)) }
    var mapsHome by remember { mutableStateOf(MapsPrefs.home(context)) }
    var mapsOffice by remember { mutableStateOf(MapsPrefs.office(context)) }
    var askedNotificationPermission by remember { mutableStateOf(false) }

    fun updateAlarm(next: com.hizkia.homescreen.alarm.AlarmSettings) {
        AlarmPrefs.save(context, next)
        alarmSettings = next
        scope.launch(Dispatchers.IO) { AlarmScheduler.rescanAll(context) }
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            updateAlarm(alarmSettings.copy(soundUri = uri.toString()))
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick++ }
    // Lokasi otomatis: diperbarui hanya saat Home dibuka (di latar depan), tanpa izin lokasi latar belakang.
    suspend fun refreshAutoLocation() {
        if (!WeatherStore.isAuto(context) || !LocationRefresher.hasPermission(context)) return
        LocationRefresher.refresh(context)?.let { fresh ->
            val applied = WeatherStore.updateAutoCity(context, fresh)
            if (applied != weatherCity) weatherCity = applied
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            weatherAuto = true
            scope.launch {
                LocationRefresher.refresh(context)?.let { fresh ->
                    WeatherStore.saveCity(context, fresh, auto = true)
                    weatherCity = fresh
                } ?: Toast.makeText(context, context.getString(R.string.location_unavailable), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.location_denied), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(resumeTick) { refreshAutoLocation() }
    val notificationsGranted = remember(resumeTick, permissionTick) { notificationsGranted(context) }
    LaunchedEffect(screen) {
        if ((screen == Screen.Alarm || screen == Screen.AddEvent || screen == Screen.Alerts) && !notificationsGranted && !askedNotificationPermission) {
            askedNotificationPermission = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val soundName = remember(alarmSettings.soundUri) {
        alarmSettings.soundUri?.let { u ->
            runCatching {
                context.contentResolver.query(Uri.parse(u), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: context.getString(R.string.chosen_file)
        }
    }

    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        calendarTick++
    }
    val requestCalendar = { calendarLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }
    val hasCalendar = remember(resumeTick, calendarTick) { CalendarRepository.hasReadPermission(context) }

    // Aturan tertunda bisa jatuh tempo kapan saja; segarkan tiap Home kembali ke depan.
    LaunchedEffect(resumeTick) { rules = SosmedStore.get(context) }
    LaunchedEffect(resumeTick) { AlertWorker.sync(context) }
    LaunchedEffect(resumeTick) {
        if (pendingUninstall) {
            pendingUninstall = false
            appsVersion++
        }
    }
    LaunchedEffect(resumeTick) { withContext(Dispatchers.IO) { AlarmScheduler.rescanAll(context) } }
    val effectiveRules = rules.effective
    val missingPermission = remember(resumeTick, permissionTick) { missingGatePermission(context) }
    val batteryOk = remember(resumeTick, permissionTick) { isBatteryExempt(context) }

    val banks = remember(apps, bankPackages) { apps.filter { it.packageName in bankPackages } }
    val bankIcons = remember(apps) { apps.associate { it.packageName to it.icon } }

    val usageMs by produceState(emptyMap<String, Long>(), effectiveRules.packages, resumeTick, screen) {
        while (true) {
            value = withContext(Dispatchers.IO) { UsageReader.foregroundTodayMs(context, effectiveRules.packages) }
            delay(USAGE_REFRESH_MS)
        }
    }
    val usageMin = remember(usageMs) { usageMs.mapValues { (it.value / 60_000).toInt() } }
    val sosmedSummary = if (effectiveRules.packages.isEmpty()) null else SosmedSummary(
        usedMin = (usageMs.values.sum() / 60_000).toInt(),
        limitMin = effectiveRules.limitMin,
        activeToday = LocalDate.now().dayOfWeek.value in effectiveRules.activeDays,
        apps = usageMs.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .mapNotNull { (pkg, ms) -> apps.firstOrNull { it.packageName == pkg }?.let { SosmedAppUsage(it.label, it.icon, ms) } },
    )

    val today = remember(resumeTick) { LocalDate.now() }
    val todayEvents by produceState(emptyList(), hasCalendar, calendarTick, resumeTick) {
        value = if (hasCalendar) {
            CalendarRepository.events(context, today.startMs(), today.plusDays(1).startMs()).filter { it.overlaps(today) }
        } else emptyList()
    }
    val dueTodos = remember(todos, today) { groupTodos(todos, today, ZoneId.systemDefault(), System.currentTimeMillis()).today }
    val doneToday = remember(todos, today) {
        val dayStart = today.startMs()
        todos.filter { (it.doneAt ?: 0L) >= dayStart }.sortedByDescending { it.doneAt }
    }

    // Kandidat: app yang bukan sistem dan bukan bank (bank memang jarang dibuka, jangan disarankan dihapus).
    var rareSort by remember { mutableStateOf(RareSort.OLDEST) }
    val needRareData = screen == Screen.Drawer || screen == Screen.Rare
    val usageStats by produceState<List<AppUsage>?>(null, apps, bankPackages, needRareData, permissionTick) {
        if (!needRareData || apps.isEmpty()) return@produceState
        val candidates = apps.filter { !it.isSystem && it.packageName !in bankPackages }.map { it.packageName }
        value = withContext(Dispatchers.IO) { RareAppsRepository.load(context, candidates) }
    }
    val nowMs = remember(usageStats) { System.currentTimeMillis() }
    val rareRanked = remember(usageStats, rareSort) { usageStats?.let { RareApps.rank(it, nowMs, rareSort) }.orEmpty() }
    val rareDefault = remember(usageStats) { usageStats?.let { RareApps.rank(it, nowMs, RareSort.OLDEST) }.orEmpty() }

    // Cuaca: tampilkan cache dulu, lalu segarkan bila sudah lebih dari 30 menit. Hanya berjalan saat Home terlihat.
    val weather by produceState<WeatherUi>(WeatherUi.Loading, weatherCity, resumeTick, screen == Screen.Home) {
        val city = weatherCity
        if (city == null) {
            value = WeatherUi.NoCity
            return@produceState
        }
        fun ui(r: com.hizkia.homescreen.weather.Reading) = WeatherUi.Ready(Math.round(r.tempC).toInt(), context.getString(WeatherCode.labelRes(r.code)), city.name)
        val cached = WeatherStore.cached(context)
        if (cached != null) value = ui(cached.first)
        if (screen != Screen.Home) return@produceState
        while (true) {
            val age = System.currentTimeMillis() - (WeatherStore.cached(context)?.second ?: 0L)
            if (age >= WeatherStore.STALE_AFTER_MS) {
                val fresh = WeatherRepository.current(city)
                if (fresh != null) {
                    WeatherStore.saveReading(context, fresh)
                    value = ui(fresh)
                } else if (WeatherStore.cached(context) == null) {
                    value = WeatherUi.Unavailable
                }
            }
            delay(WeatherStore.STALE_AFTER_MS)
        }
    }

    // Geser ke atas di Home hal. 1 (area kosong / isi sudah mentok) membuka Semua app.
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val swipeUp = remember(swipeThresholdPx) {
        LeftoverSwipe(swipeThresholdPx, upward = true, enabled = { screen == Screen.Home && homePager.currentPage == 0 }) {
            screen = Screen.Drawer
        }
    }
    // Di Semua app, geser ke bawah saat daftar sudah di paling atas kembali ke Home.
    val swipeDown = remember(swipeThresholdPx) {
        LeftoverSwipe(swipeThresholdPx, upward = false, enabled = { screen == Screen.Drawer }) {
            screen = Screen.Home
        }
    }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            Screen.BankPicker, Screen.SosmedPicker -> pickerReturnsTo
            Screen.Rules -> Screen.Settings
            Screen.Settings -> Screen.Drawer
            Screen.AddEvent -> Screen.Agenda
            Screen.Alarm -> Screen.Settings
            Screen.EventDetail -> detailReturnsTo
            Screen.Rare -> Screen.Drawer
            Screen.MapsSettings, Screen.PinnedPicker, Screen.WeatherSettings, Screen.Alerts -> Screen.Settings
            else -> Screen.Home
        }
    }
    // Launcher tidak boleh keluar lewat Back: di halaman utama Home, Back sengaja tidak melakukan apa-apa.
    BackHandler(enabled = screen == Screen.Home && homePager.currentPage == 0) { }
    BackHandler(enabled = screen == Screen.Home && homePager.currentPage != 0) {
        scope.launch { homePager.animateScrollToPage(0) }
    }

    fun deleteEvent(e: CalEvent) {
        scope.launch {
            if (CalendarRepository.deleteEvent(context, e.id)) {
                AlarmPrefs.setEventOverride(context, e.id, null)
                withContext(Dispatchers.IO) { AlarmScheduler.rescanEvents(context) }
                calendarTick++
            } else {
                Toast.makeText(context, context.getString(R.string.delete_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun reminderLabel(e: CalEvent): String {
        if (e.allDay) return context.getString(R.string.reminder_none_allday)
        val override = AlarmPrefs.eventOverride(context, e.id)
        val mode = override ?: alarmSettings.eventMode
        if (mode == ReminderMode.OFF) return context.getString(R.string.off)
        val lead = alarmSettings.eventLeadMin
        val timing = if (lead == 0) context.getString(R.string.timing_at_start) else context.getString(R.string.timing_before, lead)
        return "${context.getString(mode.labelRes)}, $timing" + if (override == null) context.getString(R.string.default_suffix) else ""
    }

    fun toggleTodo(t: Todo) {
        scope.launch { todoDao.setDone(t.id, if (t.doneAt == null) System.currentTimeMillis() else null) }
    }

    val modifier = Modifier.fillMaxSize().background(Palette.Bg)
    when (screen) {
        Screen.Home -> Box(modifier.nestedScroll(swipeUp)) {
            HorizontalPager(state = homePager, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    HomeScreen(
                        todayExpenses = todayExpenses(expenses),
                        bankIcons = bankIcons,
                        sosmed = sosmedSummary,
                        gateWarning = missingPermission?.let { context.getString(it.labelRes) }.takeIf { bankPackages.isNotEmpty() || effectiveRules.packages.isNotEmpty() },
                        onFixGate = { missingPermission?.let { context.startActivity(settingsIntentFor(context, it)) } },
                        pinned = pinnedPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg && it.packageName !in bankPackages } },
                        onLaunchApp = { AppRepository.launch(context, it.packageName) },
                        onReorderPinned = {
                            PinnedPrefs.set(context, it)
                            pinnedPackages = it
                        },
                        weather = weather,
                        onWeatherClick = { screen = Screen.WeatherSettings },
                        onSearch = {
                            if (!GoogleSearch.open(context)) Toast.makeText(context, context.getString(R.string.google_not_found), Toast.LENGTH_LONG).show()
                        },
                        onMaps = { target ->
                            val address = when (target) {
                                MapsTarget.Home -> mapsHome
                                MapsTarget.Office -> mapsOffice
                                MapsTarget.Search -> null
                            }
                            when {
                                target != MapsTarget.Search && address.isNullOrBlank() -> screen = Screen.MapsSettings
                                else -> {
                                    val opened = if (address == null) MapsLauncher.openSearch(context) else MapsLauncher.navigateTo(context, address)
                                    if (!opened) Toast.makeText(context, context.getString(R.string.maps_not_found), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onOpenDrawer = { screen = Screen.Drawer },
                        onQuickAdd = { screen = Screen.QuickAdd },
                        onOpenHistory = { screen = Screen.History },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    HomeAgendaPage(
                        today = today,
                        events = todayEvents,
                        hasPermission = hasCalendar,
                        onRequestPermission = { requestCalendar() },
                        todos = dueTodos,
                        doneToday = doneToday,
                        onToggleTodo = ::toggleTodo,
                        onOpenEvent = { e ->
                            detailEvent = e
                            detailReturnsTo = Screen.Home
                            screen = Screen.EventDetail
                        },
                        onOpenAgenda = {
                            agendaDate = today
                            screen = Screen.Agenda
                        },
                        onOpenTodo = { screen = Screen.Todo },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            PageDots(
                count = 2,
                current = homePager.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp),
            )
        }
        Screen.Drawer -> AppDrawer(
            // App bank tidak tampil di daftar; dibuka lewat Catat pengeluaran.
            apps = apps.filter { it.packageName !in bankPackages },
            onLaunch = { AppRepository.launch(context, it.packageName) },
            usageMin = usageMin,
            rareCount = rareDefault.size,
            rareBytes = RareApps.totalBytes(rareDefault),
            onOpenRare = { screen = Screen.Rare },
            onOpenSettings = { screen = Screen.Settings },
            modifier = modifier.nestedScroll(swipeDown),
        )
        Screen.QuickAdd -> QuickAddScreen(
            banks = banks,
            onSave = { amount, category, note, bank ->
                scope.launch {
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
                screen = Screen.Home
                bank?.let {
                    GrantStore.grant(context, it.packageName)
                    AppRepository.launch(context, it.packageName)
                }
            },
            onManageBanks = {
                pickerReturnsTo = Screen.QuickAdd
                screen = Screen.BankPicker
            },
            onCancel = { screen = Screen.Home },
            modifier = modifier,
        )
        Screen.History -> HistoryScreen(
            expenses = expenses,
            bankIcons = bankIcons,
            onBack = { screen = Screen.Home },
            onDelete = { e -> scope.launch { dao.delete(e.id) } },
            modifier = modifier,
        )
        Screen.Agenda -> AgendaScreen(
            selectedDate = agendaDate,
            onSelectDate = { agendaDate = it },
            hasPermission = hasCalendar,
            onRequestPermission = { requestCalendar() },
            calendarTick = calendarTick,
            onAddEvent = { screen = Screen.AddEvent },
            onOpenEvent = { e ->
                detailEvent = e
                detailReturnsTo = Screen.Agenda
                screen = Screen.EventDetail
            },
            onDeleteEvent = ::deleteEvent,
            onOpenTodo = { screen = Screen.Todo },
            onBack = { screen = Screen.Home },
            modifier = modifier,
        )
        Screen.EventDetail -> detailEvent?.let { e ->
            EventDetailScreen(
                event = e,
                reminderLabel = reminderLabel(e),
                onDelete = {
                    deleteEvent(e)
                    screen = detailReturnsTo
                },
                onBack = { screen = detailReturnsTo },
                modifier = modifier,
            )
        }
        Screen.AddEvent -> AddEventScreen(
            initialDate = agendaDate,
            onSave = { calendarId, title, beginMs, endMs, location, reminder ->
                scope.launch {
                    val id = CalendarRepository.addEvent(context, calendarId, title, beginMs, endMs, location)
                    if (id != null) {
                        reminder?.let { AlarmPrefs.setEventOverride(context, id, it) }
                        withContext(Dispatchers.IO) { AlarmScheduler.rescanEvents(context) }
                    }
                    calendarTick++
                }
                screen = Screen.Agenda
            },
            onCancel = { screen = Screen.Agenda },
            modifier = modifier,
        )
        Screen.Todo -> TodoScreen(
            todos = todos,
            onAdd = { title, dueAt ->
                scope.launch {
                    todoDao.insert(Todo(title = title, dueAt = dueAt, doneAt = null, createdAt = System.currentTimeMillis()))
                }
            },
            onToggle = ::toggleTodo,
            onDelete = { t -> scope.launch { todoDao.delete(t.id) } },
            onOpenAgenda = {
                agendaDate = LocalDate.now()
                screen = Screen.Agenda
            },
            onBack = { screen = Screen.Home },
            modifier = modifier,
        )
        Screen.PinnedPicker -> QuickAppsScreen(
            apps = apps.filter { it.packageName !in bankPackages },
            selected = pinnedPackages,
            onSave = {
                PinnedPrefs.set(context, it)
                pinnedPackages = it
                screen = Screen.Settings
            },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.Alerts -> AlertSettingsScreen(
            settings = alertSettings,
            hasCity = weatherCity != null,
            notificationsMissing = !notificationsGranted,
            onChange = {
                AlertPrefs.save(context, it)
                alertSettings = it
                AlertWorker.sync(context)
            },
            onSendSample = {
                AlertNotifier.postSample(context)
            },
            onOpenWeather = { screen = Screen.WeatherSettings },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.WeatherSettings -> WeatherSettingsScreen(
            current = weatherCity,
            auto = weatherAuto,
            onUseLocation = {
                if (LocationRefresher.hasPermission(context)) {
                    scope.launch {
                        weatherAuto = true
                        val fresh = LocationRefresher.refresh(context)
                        if (fresh != null) {
                            WeatherStore.saveCity(context, fresh, auto = true)
                            weatherCity = fresh
                            screen = Screen.Settings
                        } else {
                            Toast.makeText(context, context.getString(R.string.location_unavailable), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onPick = { city ->
                WeatherStore.saveCity(context, city, auto = false)
                weatherAuto = false
                weatherCity = city
                screen = Screen.Settings
            },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.MapsSettings -> MapsSettingsScreen(
            home = mapsHome,
            office = mapsOffice,
            onSave = { h, o ->
                MapsPrefs.save(context, h, o)
                mapsHome = h.trim()
                mapsOffice = o.trim()
                screen = Screen.Settings
            },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.Rare -> {
            val byPkg = remember(apps) { apps.associateBy { it.packageName } }
            RareAppsScreen(
                rows = rareRanked.map { u -> RareRow(u, byPkg[u.pkg]?.label ?: u.pkg, byPkg[u.pkg]?.icon) },
                sort = rareSort,
                onSort = { rareSort = it },
                nowMs = nowMs,
                loading = usageStats == null,
                permissionMissing = !hasUsageAccess(context),
                onFixPermission = { context.startActivity(settingsIntentFor(context, GatePermission.UsageAccess)) },
                onDelete = { pkg ->
                    pendingUninstall = true
                    context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                onBack = { screen = Screen.Drawer },
                modifier = modifier,
            )
        }
        Screen.Settings -> SettingsScreen(
            limitSummary = stringResource(R.string.limit_per_day, rules.target.limitMin),
            sosmedCount = rules.target.packages.size,
            bankCount = bankPackages.size,
            permissions = listOf(
                PermissionRow(context.getString(GatePermission.UsageAccess.labelRes), hasUsageAccess(context)) {
                    context.startActivity(settingsIntentFor(context, GatePermission.UsageAccess))
                },
                PermissionRow(context.getString(GatePermission.Overlay.labelRes), Settings.canDrawOverlays(context)) {
                    context.startActivity(settingsIntentFor(context, GatePermission.Overlay))
                },
                PermissionRow(stringResource(R.string.battery_exempt), batteryOk) {
                    context.startActivity(batteryIntent(context))
                    permissionTick++
                },
                PermissionRow(stringResource(R.string.calendar_access), hasCalendar) { requestCalendar() },
            ),
            pinnedSummary = if (pinnedPackages.isEmpty()) stringResource(R.string.not_set) else pluralStringResource(R.plurals.n_apps, pinnedPackages.size, pinnedPackages.size),
            onPickPinned = { screen = Screen.PinnedPicker },
            alertsSummary = if (alertSettings.enabledCount == 0) stringResource(R.string.off) else stringResource(R.string.alerts_on_count, alertSettings.enabledCount),
            onOpenAlerts = { screen = Screen.Alerts },
            weatherSummary = weatherCity?.name ?: stringResource(R.string.not_set),
            onOpenWeather = { screen = Screen.WeatherSettings },
            mapsSummary = if (mapsHome.isBlank() && mapsOffice.isBlank()) stringResource(R.string.not_set) else listOfNotNull(stringResource(R.string.maps_home).takeIf { mapsHome.isNotBlank() }, stringResource(R.string.maps_office).takeIf { mapsOffice.isNotBlank() }).joinToString(", "),
            onOpenMaps = { screen = Screen.MapsSettings },
            alarmSummary = if (alarmSettings.wakeEnabled) "Routines %02d:%02d".format(alarmSettings.wakeHour, alarmSettings.wakeMinute) else stringResource(R.string.off),
            onOpenAlarm = { screen = Screen.Alarm },
            onOpenRules = { screen = Screen.Rules },
            onPickSosmed = {
                pickerReturnsTo = Screen.Settings
                screen = Screen.SosmedPicker
            },
            onPickBanks = {
                pickerReturnsTo = Screen.Settings
                screen = Screen.BankPicker
            },
            onBack = { screen = Screen.Drawer },
            modifier = modifier,
        )
        Screen.Alarm -> AlarmSettingsScreen(
            settings = alarmSettings,
            soundName = soundName,
            permissions = alarmPermissionRows(context, permissionTick, notificationLauncher::launch, onChanged = { permissionTick++ }),
            notificationsMissing = !notificationsGranted,
            onChange = ::updateAlarm,
            onPickSound = { soundPicker.launch(arrayOf("audio/*")) },
            onTest = { AlarmService.start(context, AlarmService.KIND_TEST, context.getString(R.string.test_alarm_title), context.getString(R.string.test_alarm_text)) },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.Rules -> RulesScreen(
            state = rules,
            onSave = { proposed ->
                rules = SosmedStore.propose(context, proposed)
                screen = Screen.Settings
            },
            onBack = { screen = Screen.Settings },
            modifier = modifier,
        )
        Screen.BankPicker -> AppPickerScreen(
            title = stringResource(R.string.pick_bank_title),
            hint = stringResource(R.string.pick_bank_hint),
            apps = apps,
            selected = bankPackages,
            onSave = {
                BankPrefs.set(context, it)
                bankPackages = it
                screen = pickerReturnsTo
            },
            onBack = { screen = pickerReturnsTo },
            modifier = modifier,
        )
        Screen.SosmedPicker -> AppPickerScreen(
            title = stringResource(R.string.pick_social_title),
            hint = stringResource(R.string.pick_social_hint),
            apps = apps.filter { it.packageName !in bankPackages },
            selected = rules.target.packages,
            onSave = { picked ->
                rules = SosmedStore.propose(context, rules.target.copy(packages = picked))
                screen = pickerReturnsTo
            },
            onBack = { screen = pickerReturnsTo },
            modifier = modifier,
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Palette.Text else androidx.compose.ui.graphics.Color(0xFF3A4A43)),
            )
        }
    }
}

/** Baris status izin yang dipakai alarm. [tick] hanya agar daftar dihitung ulang setelah kembali dari pengaturan sistem. */
@Composable
private fun alarmPermissionRows(
    context: android.content.Context,
    tick: Int,
    requestNotification: (String) -> Unit,
    onChanged: () -> Unit,
): List<PermissionRow> = remember(tick) {
    val rows = mutableListOf<PermissionRow>()
    val notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    rows += PermissionRow(context.getString(R.string.notifications), notifOk) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        onChanged()
    }

    val nm = context.getSystemService(NotificationManager::class.java)
    rows += PermissionRow(context.getString(R.string.perm_dnd), nm.isNotificationPolicyAccessGranted) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        onChanged()
    }

    val am = context.getSystemService(AlarmManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rows += PermissionRow(context.getString(R.string.perm_exact_alarm), am.canScheduleExactAlarms()) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            onChanged()
        }
    }
    rows
}

private fun notificationsGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
