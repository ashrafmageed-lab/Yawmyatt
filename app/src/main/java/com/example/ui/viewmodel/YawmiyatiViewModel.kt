package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bible.BibleManager
import com.example.bible.BibleVerseHit
import com.example.data.local.AppDatabase
import com.example.data.model.DayEntity
import com.example.data.model.EntryEntity
import com.example.data.model.JournalItem
import com.example.data.repository.YawmiyatiRepository
import com.example.reminder.ReminderManager
import com.example.reminder.ReminderSettings
import com.example.security.SecurityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class YawmiyatiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = YawmiyatiRepository(db.dayDao(), db.entryDao())

    // Date selection
    private val _selectedDate = MutableStateFlow(YawmiyatiRepository.todayDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Day record for selected date
    val currentDay: StateFlow<DayEntity?> = _selectedDate.flatMapLatest { date ->
        repository.getDay(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Entries by kind
    val dreams: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("dreams")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fears: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("fears")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishes: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("wishes")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taboos: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("taboos")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("goal")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val obstacles: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("obstacle")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayers: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("prayer")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<EntryEntity>> = repository.getEntriesByKind("journal")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDays: StateFlow<List<DayEntity>> = repository.getAllDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bible Picker State
    private val _isBibleLoading = MutableStateFlow(false)
    val isBibleLoading: StateFlow<Boolean> = _isBibleLoading.asStateFlow()

    private val _bibleSearchResults = MutableStateFlow<List<BibleVerseHit>>(emptyList())
    val bibleSearchResults: StateFlow<List<BibleVerseHit>> = _bibleSearchResults.asStateFlow()

    // Reminders
    private val _reminders = MutableStateFlow(ReminderManager.getSettings(application))
    val reminders: StateFlow<ReminderSettings> = _reminders.asStateFlow()

    // Font Scale Preference (1.0f = normal, 1.15f = comfortable, 1.3f = large)
    private val prefs = application.getSharedPreferences("yawmiyati_prefs", Context.MODE_PRIVATE)
    private val _fontScale = MutableStateFlow(prefs.getFloat("font_scale", 1.0f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    // Spiritual Rhythm / Streak of consecutive active days
    val streak: StateFlow<Int> = combine(allDays, journalEntries) { days, entries ->
        calculateStreakDays(days, entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Preload bible in background
        viewModelScope.launch {
            _isBibleLoading.value = true
            BibleManager.loadBible(getApplication())
            _isBibleLoading.value = false
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun updateDayField(
        ref: String? = null,
        verse: String? = null,
        prayer: String? = null,
        toGod: String? = null,
        fromGod: String? = null,
        kept: String? = null,
        thanks: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val updated = current.copy(
                ref = ref ?: current.ref,
                verse = verse ?: current.verse,
                prayer = prayer ?: current.prayer,
                toGod = toGod ?: current.toGod,
                fromGod = fromGod ?: current.fromGod,
                kept = kept ?: current.kept,
                thanks = thanks ?: current.thanks,
                note = note ?: current.note
            )
            repository.saveDay(updated)
        }
    }

    fun addTodo(text: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val items = YawmiyatiRepository.parseItems(current.todosJson).toMutableList()
            items.add(JournalItem(t = text, d = false))
            val updated = current.copy(todosJson = YawmiyatiRepository.serializeItems(items))
            repository.saveDay(updated)
        }
    }

    fun toggleTodo(index: Int, done: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val items = YawmiyatiRepository.parseItems(current.todosJson).toMutableList()
            if (index in items.indices) {
                items[index] = items[index].copy(d = done)
                val updated = current.copy(todosJson = YawmiyatiRepository.serializeItems(items))
                repository.saveDay(updated)
            }
        }
    }

    fun removeTodo(index: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val items = YawmiyatiRepository.parseItems(current.todosJson).toMutableList()
            if (index in items.indices) {
                items.removeAt(index)
                val updated = current.copy(todosJson = YawmiyatiRepository.serializeItems(items))
                repository.saveDay(updated)
            }
        }
    }

    fun addDont(text: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val items = YawmiyatiRepository.parseItems(current.dontsJson).toMutableList()
            items.add(JournalItem(t = text, d = false))
            val updated = current.copy(dontsJson = YawmiyatiRepository.serializeItems(items))
            repository.saveDay(updated)
        }
    }

    fun removeDont(index: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val items = YawmiyatiRepository.parseItems(current.dontsJson).toMutableList()
            if (index in items.indices) {
                items.removeAt(index)
                val updated = current.copy(dontsJson = YawmiyatiRepository.serializeItems(items))
                repository.saveDay(updated)
            }
        }
    }

    fun addEntry(kind: String, text: String, parentId: Long? = null) {
        viewModelScope.launch {
            repository.addEntry(EntryEntity(kind = kind, text = text, parentId = parentId))
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            repository.deleteGoalAndObstacles(goalId)
        }
    }

    fun updateGoalStepAndVerse(goalId: Long, step: String, verse: String) {
        viewModelScope.launch {
            val all = repository.getAllEntries()
            // We find the goal and update it
            val goal = goals.value.find { it.id == goalId } ?: return@launch
            repository.updateEntry(goal.copy(step = step, verse = verse))
        }
    }

    fun linkFearVerse(fearId: Long, ref: String, ptext: String) {
        viewModelScope.launch {
            val fear = fears.value.find { it.id == fearId } ?: return@launch
            repository.updateEntry(fear.copy(ref = ref, ptext = ptext))
        }
    }

    fun markPrayerAnswered(prayerId: Long, date: String, note: String) {
        viewModelScope.launch {
            val prayer = prayers.value.find { it.id == prayerId } ?: return@launch
            repository.updateEntry(prayer.copy(ansDate = date, ansNote = note))
        }
    }

    fun reopenPrayer(prayerId: Long) {
        viewModelScope.launch {
            val prayer = prayers.value.find { it.id == prayerId } ?: return@launch
            repository.updateEntry(prayer.copy(ansDate = null, ansNote = null))
        }
    }

    // Bible Search
    fun searchBible(query: String) {
        viewModelScope.launch {
            _bibleSearchResults.value = BibleManager.search(query)
        }
    }

    // Reminders
    fun updateReminders(settings: ReminderSettings) {
        ReminderManager.saveSettings(getApplication(), settings)
        _reminders.value = settings
    }

    // Font Scale Setting
    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
        _fontScale.value = scale
    }

    // Favorites
    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            val existing = journalEntries.value.find { it.id == id } ?: return@launch
            val newFav = if (existing.isFavorite) "" else "fav"
            repository.updateEntry(existing.copy(ansNote = newFav))
        }
    }

    companion object {
        fun calculateStreakDays(days: List<DayEntity>, entries: List<EntryEntity>): Int {
        val activeDates = mutableSetOf<String>()
        days.forEach { day ->
            if (day.note.isNotBlank() || day.prayer.isNotBlank() || day.thanks.isNotBlank() || day.toGod.isNotBlank()) {
                activeDates.add(day.date)
            }
        }
        entries.forEach { entry ->
            val dateMatch = Regex("\\d{4}-\\d{2}-\\d{2}").find(entry.ref)
            if (dateMatch != null) {
                activeDates.add(dateMatch.value)
            }
        }
        if (activeDates.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        var streak = 0

        val todayStr = dateFormat.format(cal.time)
        val isTodayActive = activeDates.contains(todayStr)

        if (!isTodayActive) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)
            if (!activeDates.contains(yesterdayStr)) {
                return 0
            }
        }

        while (true) {
            val checkDate = dateFormat.format(cal.time)
            if (activeDates.contains(checkDate)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}

    fun addJournalEntry(
        title: String,
        content: String,
        timestamp: String,
        verse: String = "",
        ref: String = "",
        tag: String = "",
        mood: String = "",
        isFav: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addEntry(
                EntryEntity(
                    kind = "journal",
                    text = content,
                    step = if (title.isNotBlank()) title else tag,
                    ref = timestamp,
                    verse = verse,
                    ptext = ref,
                    ansDate = mood.ifBlank { tag },
                    ansNote = if (isFav) "fav" else ""
                )
            )
            val date = _selectedDate.value
            val current = repository.getDayOrDefault(date)
            val header = if (title.isNotBlank()) "[$timestamp] $title" else "[$timestamp]"
            val newNote = if (current.note.isBlank()) "$header\n$content" else "${current.note}\n\n$header\n$content"
            repository.saveDay(current.copy(note = newNote))
        }
    }

    fun updateJournalEntry(
        id: Long,
        title: String,
        content: String,
        verse: String = "",
        ref: String = "",
        tag: String = "",
        mood: String = "",
        isFav: Boolean? = null
    ) {
        viewModelScope.launch {
            val existing = journalEntries.value.find { it.id == id } ?: return@launch
            val updatedFav = when (isFav) {
                true -> "fav"
                false -> ""
                null -> existing.ansNote
            }
            repository.updateEntry(
                existing.copy(
                    text = content,
                    step = if (title.isNotBlank()) title else tag,
                    verse = verse,
                    ptext = ref,
                    ansDate = if (mood.isNotBlank()) mood else existing.ansDate,
                    ansNote = updatedFav
                )
            )
        }
    }

    fun updateDayReflection(
        date: String,
        note: String,
        prayer: String = "",
        toGod: String = "",
        fromGod: String = "",
        thanks: String = ""
    ) {
        viewModelScope.launch {
            val current = repository.getDayOrDefault(date)
            repository.saveDay(
                current.copy(
                    note = note,
                    prayer = if (prayer.isNotBlank()) prayer else current.prayer,
                    toGod = if (toGod.isNotBlank()) toGod else current.toGod,
                    fromGod = if (fromGod.isNotBlank()) fromGod else current.fromGod,
                    thanks = if (thanks.isNotBlank()) thanks else current.thanks
                )
            )
        }
    }
}
