# Homescreen

A personal Android launcher for people who are forgetful and spend too fast.

I kept losing track of my spending and losing hours to social media, so instead of relying on willpower I built a home screen that puts small, deliberate obstacles in the way.

> Personal project, built for and used daily on a Samsung (One UI) phone. Sideloaded, not on the Play Store.

## What it does

**Money**
- **Log first, bank second.** Banking apps are hidden from the app list. Opening one from anywhere (Recents, notifications, shortcuts) shows a quick "log your expense" screen first. After saving, the bank app opens and stays unlocked for 3 minutes of activity.
- Fast expense entry: amount, category, bank or cash, optional note.
- History with per-day grouping and a "this month by category" breakdown.

**Focus**
- **Daily social media budget.** Pick the apps and one shared daily limit in minutes, plus which days it applies. When the time is up, those apps are blocked for the rest of the day.
- **Loosening is delayed.** Tightening the rules applies instantly; loosening them (raising the limit, removing an app or day) only takes effect after 24 hours.
- Today's usage on the home screen, with a per-app breakdown.

**Day planning**
- Home page 2 shows today's agenda and to-dos.
- **Agenda** reads and writes your phone's calendar (so it syncs with Google/Samsung): week strip that expands into a month view, event details, add and delete.
- **Reminders per event:** off, notification, or full-screen alarm.
- **Morning Routines:** a wake-up alarm at a time you choose, which also turns the phone's sound on beforehand and plays a loud built-in tone (or your own audio file).
- Local to-do list.

**Home screen**
- Weather for a city you choose, and a search button that opens the Google app.
- Maps shortcuts (Home, Work, Find a place).
- Up to 5 quick apps; long-press and drag to reorder.
- Swipe up to open all apps; swipe down from the top of the list to go back.
- **Rarely used apps:** apps unopened for 30+ days, sorted by age or size, with a one-tap uninstall (Android still asks for confirmation).
- Optional alerts for rain, thunderstorms and earthquakes (off by default).
- English, Indonesian and German, following the phone's language (anything else falls back to English).

## How it works

- **Bank and social media gate:** a foreground service polls `UsageStatsManager` while the screen is on and opens a blocking activity when a restricted app comes to the front. It deliberately does **not** use an Accessibility service, because some banking apps refuse to run while one is enabled.
- **Rules live in one place:** `PolicyEngine` is pure Kotlin with no Android dependencies, so the gate and limit logic is unit-tested.
- **Data stays on the device:** Room (SQLite) for expenses and to-dos, SharedPreferences for settings.
- **Calendar:** the system Calendar Provider, so there is no separate event database.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · AlarmManager · UsageStatsManager · CalendarContract · Android Gradle Plugin 9 · minSdk 29 / targetSdk 36

## Permissions and why

| Permission | Used for |
|---|---|
| Usage access | Detecting which app is in front, daily social media time, last-opened and size of apps |
| Display over other apps | Showing the gate and limit screens on top of a restricted app |
| Calendar (read/write) | Agenda and event reminders |
| Notifications | Reminders, alarms and alerts |
| Exact alarms, boot completed | Scheduling alarms and reminders reliably |
| Internet | Weather (Open-Meteo) and optional earthquake alerts (BMKG) |
| Do Not Disturb access (optional) | Letting Morning Routines turn DND off |
| Battery optimization exemption (recommended) | Keeping the gate service alive on Samsung devices |

## Privacy

No account, no analytics, no ads. Expenses, to-dos and settings never leave the phone. The only network calls are to [Open-Meteo](https://open-meteo.com) (city name or approximate coordinates, for weather) and [BMKG](https://data.bmkg.go.id) (public earthquake feed, only if you enable alerts).

## Build and run

Requirements: Android Studio (its bundled JDK is fine) and an Android 10+ device with USB debugging.

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug       # debug APK
```

Or open the project in Android Studio and press Run. Then grant the permissions shown in the app and set it as the default home app:
**Settings > Apps > Choose default apps > Home app > Homescreen**.

To go back to your old launcher, choose it in the same menu, or uninstall Homescreen.

## Project layout

```
app/src/main/java/com/hizkia/homescreen/
├─ launcher/      home screen, app list, gestures
├─ enforcement/   gate service, gate and limit screens
├─ policy/        PolicyEngine and rule logic (pure Kotlin)
├─ finance/       expense entry, history, summaries
├─ agenda/ todo/  calendar and to-do screens
├─ alarm/         reminders, Morning Routines, alarm playback
├─ alerts/        weather and earthquake alerts
├─ weather/ maps/ weather data and Maps shortcuts
├─ apps/          rarely used apps
├─ settings/      settings screens
└─ data/ ui/      Room database, preferences, theme and locale helpers
```

More detail: [PRD.md](PRD.md), [ARCHITECTURE.md](ARCHITECTURE.md), [TODO.md](TODO.md).

## Known limitations

- The gate reacts with a delay of roughly half a second to a second, so a restricted app can flash briefly before the gate covers it.
- It is a speed bump, not a lock: you can still switch back to another launcher or uninstall the app.
- Samsung's battery management can stop background services; exempt the app from battery optimization.
- Earthquake alerts are notifications after the event, not early warnings. Region names come from BMKG and stay in Indonesian.
- Only tested on a Samsung One UI device. Expense amounts are shown in rupiah.

## Roadmap

- Backup of expenses to a self-hosted server, then yearly archiving.
- Automatic location for weather via a manual refresh button.
- A "pay a fine to loosen the limit" mode.


