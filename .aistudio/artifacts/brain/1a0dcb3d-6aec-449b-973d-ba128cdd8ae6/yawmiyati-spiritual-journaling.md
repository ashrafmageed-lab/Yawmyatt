---
name: "yawmiyati-spiritual-journaling"
description: >-
  Architectural patterns, domain models, and UI guidelines for building
  offline-first, reverent spiritual journaling apps on Android with Jetpack Compose,
  Room database, bundled offline Scripture search, local evening notification reminders,
  and Sacred Gold Material 3 aesthetics. Use whenever building personal journaling,
  devotional reflections, prayer logs, or offline scripture-linked mobile experiences.
---

# Spiritual Journaling & Offline Devotional Apps (Yawmiyati Pattern)

This skill documents proven design, architectural patterns, and code recipes extracted from building **Yawmiyati (يوميتي)** — a serene, offline-first spiritual journaling Android application built with Jetpack Compose, Room, and Material 3.

---

## 1. Core Architectural Pillars

### 1.1 Frictionless Sacred Space (No Lock Screens)
- **Principle**: When a user experiences a moment of inspiration, prayer, or sorrow, friction is the enemy. Do **not** impose PINs, biometric gates, or passwords on app launch unless explicitly demanded by the user.
- **Instant Entry**: Launching the app or clicking a reminder notification must immediately drop the user into the writing canvas with current date/time pre-populated.

### 1.2 Zero-Cloud Privacy & Local Persistence (Room)
- **Offline-First by Design**: Every reflection, confession, prayer request, and thanksgiving item lives strictly on-device in a local SQLite/Room database.
- **Export/Import**: Provide user-controlled backups via standard Android SAF (Storage Access Framework) with optional AES-GCM encryption for users wanting extra transport security.

### 1.3 Sacred Gold Palette & Visual Reverence
- **Dark Elegance**: Deep background (`#121316` / `#1B1C1F`) providing quiet focus for nightly or early-morning reflections.
- **Sacred Gold Accents**:
  - Primary Gold: `#C5A059`
  - Accent / Badge Gold: `#E0B86C`
  - Warm Container Tint: `#2A241A`
  - Verse / Parchment Card: `#FFF4D4`
- **Arabic Typography**: Full RTL support (`LocalLayoutDirection provides LayoutDirection.Rtl`), high line-height (`lineHeight = 26.sp+`) for Arabic script clarity, and user-adjustable font scale multipliers (1.0x, 1.15x, 1.30x).

---

## 2. Bundled Offline Scripture Engine

Spiritual journaling often incorporates sacred verses. To keep the app 100% offline, bundle the full text (e.g. 66 books of the Holy Bible in Arabic) as compressed assets.

### 2.1 Arabic Tashkeel-Insensitive Search
Arabic text in scriptures usually has diacritics (تَشْكِيل: فَتْحَة، كَسْرَة، ضَمَّة، سُكُون). Searching without stripping diacritics results in missed queries.

```kotlin
object BibleManager {
    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u0652\\u0670\\u0640]")

    fun stripArabic(input: String): String {
        return input
            .replace(TASHKEEL_REGEX, "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .trim()
    }

    suspend fun search(query: String): List<BibleVerseHit> {
        val cleanQuery = stripArabic(query)
        if (cleanQuery.isBlank()) return emptyList()
        // Search through loaded books/verses matching cleanQuery
        ...
    }
}
```

### 2.2 Direct Journal Citation
Provide a bottom sheet or dialog to search verses by keyword or reference (e.g. `مزمور 23`), preview the verse, and insert it formatted directly into the reflection:
```kotlin
fun BibleVerseHit.toJournalQuote(): String = "«$text» ($reference)"
```

---

## 3. History Timeline & Live Content Search

### 3.1 Unified History Model
A spiritual day often contains both structured daily records (prayers, answered items, gratitudes) and unstructured journal reflections. Unify them into a sealed interface for chronological display:

```kotlin
sealed interface HistoryItem {
    val dateSortKey: String
    data class JournalReflection(val entry: EntryEntity) : HistoryItem {
        override val dateSortKey: String get() = entry.ref // e.g. "2026-11-01 21:00"
    }
    data class DayRecord(val day: DayEntity) : HistoryItem {
        override val dateSortKey: String get() = "${day.date} 00:00"
    }
}
```

### 3.2 Content Filtering
Filter across all content fields (reflection text, scripture quotes, prayer notes, mood tags):
```kotlin
val filtered = if (query.isNotBlank()) {
    list.filter { item ->
        when (item) {
            is HistoryItem.JournalReflection -> {
                item.entry.text.contains(query, ignoreCase = true) ||
                item.entry.verse.contains(query, ignoreCase = true) ||
                item.entry.step.contains(query, ignoreCase = true)
            }
            is HistoryItem.DayRecord -> {
                item.day.note.contains(query, ignoreCase = true) ||
                item.day.prayer.contains(query, ignoreCase = true) ||
                item.day.thanks.contains(query, ignoreCase = true)
            }
        }
    }
} else list
```

---

## 4. Evening Gentle Reminder System

Spiritual journaling is built on daily evening rhythm. Evening reminders must be gentle, reliable, and respectful.

### 4.1 System Configuration
- **Manifest Permissions**:
  - `android.permission.POST_NOTIFICATIONS`
  - `android.permission.SCHEDULE_EXACT_ALARM`
  - `android.permission.RECEIVE_BOOT_COMPLETED`
- **AlarmManager + BroadcastReceiver**:
  - Use `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ...)` to ensure delivery across Doze mode.
  - Automatically reschedule the next day upon alarm reception.
  - Re-register alarms on device reboot (`BootReceiver`).

### 4.2 Evening Presets & Instant Testing
- Provide sensible evening defaults: **9:00 PM (21:00)** or quick chips for `8:00 PM`, `9:00 PM`, `9:30 PM`, `10:00 PM`.
- Always provide an in-app button: **«تجربة إشعار تذكير المساء الآن 🔔»** (`sendEveningTestReminder`) so users can test on the emulator or verify permissions immediately.

### 4.3 Direct Deep-Link to Compose Writing Canvas
The reminder notification's PendingIntent should pass an extra (`open_new_entry = true`) to `MainActivity`:
```kotlin
val launchIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra("open_new_entry", true)
}
```

---

## 5. Spiritual Rhythm (Streak) Calculation

Unlike gamified streaks that penalize the user aggressively, spiritual rhythm should celebrate consistency:
```kotlin
fun calculateStreakDays(days: List<DayEntity>, entries: List<EntryEntity>): Int {
    val activeDates = mutableSetOf<String>()
    days.forEach { if (it.hasActivity()) activeDates.add(it.date) }
    entries.forEach { entry ->
        val dateMatch = Regex("\\d{4}-\\d{2}-\\d{2}").find(entry.ref)
        if (dateMatch != null) activeDates.add(dateMatch.value)
    }
    // Calculate consecutive days backwards from today or yesterday
    ...
}
```
