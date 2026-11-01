package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class JournalItem(
    val t: String,
    val d: Boolean = false
)

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey
    val date: String,
    val todosJson: String = "[]",
    val dontsJson: String = "[]",
    val ref: String = "",
    val verse: String = "",
    val prayer: String = "",
    val toGod: String = "",
    val fromGod: String = "",
    val kept: String = "", // "y", "p", "n", or ""
    val thanks: String = "",
    val note: String = ""
)

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String, // "dreams", "fears", "wishes", "taboos", "goal", "obstacle", "prayer", "journal"
    val text: String,
    val parentId: Long? = null,
    val step: String = "",
    val verse: String = "",
    val ref: String = "",
    val ptext: String = "",
    val ansDate: String? = null,
    val ansNote: String? = null
) {
    val isFavorite: Boolean
        get() = ansNote == "fav"

    val moodTag: String
        get() = (ansDate ?: ptext).ifBlank { step.ifBlank { "تأمل روحي 🌿" } }
}
