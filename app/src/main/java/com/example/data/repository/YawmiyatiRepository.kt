package com.example.data.repository

import com.example.data.local.DayDao
import com.example.data.local.EntryDao
import com.example.data.model.DayEntity
import com.example.data.model.EntryEntity
import com.example.data.model.JournalItem
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YawmiyatiRepository(
    private val dayDao: DayDao,
    private val entryDao: EntryDao
) {
    companion object {
        fun todayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        fun parseItems(json: String): List<JournalItem> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<JournalItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        JournalItem(
                            t = obj.optString("t", ""),
                            d = obj.optBoolean("d", false)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeItems(items: List<JournalItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("t", item.t)
                    put("d", item.d)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }

    fun getDay(date: String): Flow<DayEntity?> = dayDao.getDay(date)

    suspend fun getDayOrDefault(date: String): DayEntity {
        return dayDao.getDayDirect(date) ?: DayEntity(date = date)
    }

    suspend fun saveDay(day: DayEntity) = withContext(Dispatchers.IO) {
        dayDao.insertOrUpdate(day)
    }

    fun getAllDays(): Flow<List<DayEntity>> = dayDao.getAllDays()

    fun getEntriesByKind(kind: String): Flow<List<EntryEntity>> = entryDao.getEntriesByKind(kind)

    fun getAllEntries(): Flow<List<EntryEntity>> = entryDao.getAllEntries()

    suspend fun addEntry(entry: EntryEntity): Long = withContext(Dispatchers.IO) {
        entryDao.insert(entry)
    }

    suspend fun updateEntry(entry: EntryEntity) = withContext(Dispatchers.IO) {
        entryDao.update(entry)
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        entryDao.deleteById(id)
    }

    suspend fun deleteGoalAndObstacles(goalId: Long) = withContext(Dispatchers.IO) {
        entryDao.deleteByParentId(goalId)
        entryDao.deleteById(goalId)
    }

    suspend fun exportData(password: String? = null): String = withContext(Dispatchers.IO) {
        val days = dayDao.getAllDaysList()
        val entries = entryDao.getAllEntriesList()

        val root = JSONObject().apply {
            put("v", 1)

            val daysArray = JSONArray()
            for (d in days) {
                val dObj = JSONObject().apply {
                    put("date", d.date)
                    put("todo", JSONArray(d.todosJson))
                    put("dont", JSONArray(d.dontsJson))
                    put("ref", d.ref)
                    put("verse", d.verse)
                    put("prayer", d.prayer)
                    put("toGod", d.toGod)
                    put("fromGod", d.fromGod)
                    put("kept", d.kept)
                    put("thanks", d.thanks)
                    put("note", d.note)
                }
                daysArray.put(dObj)
            }
            put("days", daysArray)

            val entriesArray = JSONArray()
            for (e in entries) {
                val eObj = JSONObject().apply {
                    put("id", e.id)
                    put("kind", e.kind)
                    put("text", e.text)
                    if (e.parentId != null) put("parent", e.parentId)
                    if (e.step.isNotBlank()) put("step", e.step)
                    if (e.verse.isNotBlank()) put("verse", e.verse)
                    if (e.ref.isNotBlank()) put("ref", e.ref)
                    if (e.ptext.isNotBlank()) put("ptext", e.ptext)
                    if (e.ansDate != null) put("ans", e.ansDate)
                    if (e.ansNote != null) put("note", e.ansNote)
                }
                entriesArray.put(eObj)
            }
            put("entries", entriesArray)
        }

        val jsonString = root.toString(2)
        if (!password.isNullOrBlank()) {
            SecurityManager.encryptText(jsonString, password)
        } else {
            jsonString
        }
    }

    suspend fun importData(rawJson: String, password: String? = null) = withContext(Dispatchers.IO) {
        var jsonText = rawJson
        val testObj = JSONObject(rawJson)
        if (testObj.optBoolean("enc", false)) {
            if (password.isNullOrBlank()) {
                throw IllegalArgumentException("كلمة السر مطلوبة لفك تشفير النسخة")
            }
            jsonText = SecurityManager.decryptText(rawJson, password)
        }

        val root = JSONObject(jsonText)
        val daysArray = root.optJSONArray("days") ?: JSONArray()
        val entriesArray = root.optJSONArray("entries") ?: JSONArray()

        dayDao.clearAll()
        entryDao.clearAll()

        for (i in 0 until daysArray.length()) {
            val dObj = daysArray.getJSONObject(i)
            val todoList = dObj.optJSONArray("todo")?.toString() ?: "[]"
            val dontList = dObj.optJSONArray("dont")?.toString() ?: "[]"
            val day = DayEntity(
                date = dObj.optString("date", todayDate()),
                todosJson = todoList,
                dontsJson = dontList,
                ref = dObj.optString("ref", ""),
                verse = dObj.optString("verse", ""),
                prayer = dObj.optString("prayer", ""),
                toGod = dObj.optString("toGod", ""),
                fromGod = dObj.optString("fromGod", ""),
                kept = dObj.optString("kept", ""),
                thanks = dObj.optString("thanks", ""),
                note = dObj.optString("note", "")
            )
            dayDao.insertOrUpdate(day)
        }

        for (i in 0 until entriesArray.length()) {
            val eObj = entriesArray.getJSONObject(i)
            val entry = EntryEntity(
                id = eObj.optLong("id", 0),
                kind = eObj.optString("kind", "prayer"),
                text = eObj.optString("text", ""),
                parentId = if (eObj.has("parent")) eObj.optLong("parent") else null,
                step = eObj.optString("step", ""),
                verse = eObj.optString("verse", ""),
                ref = eObj.optString("ref", ""),
                ptext = eObj.optString("ptext", ""),
                ansDate = if (eObj.has("ans")) eObj.optString("ans") else null,
                ansNote = if (eObj.has("note")) eObj.optString("note") else null
            )
            entryDao.insert(entry)
        }
    }
}
