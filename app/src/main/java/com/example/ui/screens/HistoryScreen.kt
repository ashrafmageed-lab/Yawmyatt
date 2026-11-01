package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DayEntity
import com.example.data.model.EntryEntity
import com.example.ui.components.BiblePickerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.YawmiyatiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Unified model for sorting all past journal reflections and days chronologically
sealed class HistoryItem(val dateSortKey: String) {
    data class JournalReflection(val entry: EntryEntity) : HistoryItem(entry.ref.ifBlank { "0000-00-00" })
    data class DayRecord(val day: DayEntity) : HistoryItem(day.date)
}

@Composable
fun HistoryScreen(
    viewModel: YawmiyatiViewModel,
    onNavigateToDayWithDate: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val allDays by viewModel.allDays.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val prayers by viewModel.prayers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") } // "الكل", "اليوميات", "الأيام", "الصلوات"

    // Edit and Detail Dialog States
    var editingJournalEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var editingDayRecord by remember { mutableStateOf<DayEntity?>(null) }
    var viewingDetailEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var viewingDayDetail by remember { mutableStateOf<DayEntity?>(null) }
    var deletingEntryId by remember { mutableStateOf<Long?>(null) }

    // Combine and sort all entries chronologically by date (newest first)
    val sortedHistoryItems = remember(allDays, journalEntries, searchQuery, selectedFilter) {
        val list = mutableListOf<HistoryItem>()

        if (selectedFilter == "الكل" || selectedFilter == "اليوميات") {
            journalEntries.forEach { entry ->
                list.add(HistoryItem.JournalReflection(entry))
            }
        } else if (selectedFilter == "المفضلة 💛") {
            journalEntries.filter { it.isFavorite }.forEach { entry ->
                list.add(HistoryItem.JournalReflection(entry))
            }
        }

        if (selectedFilter == "الكل" || selectedFilter == "الأيام") {
            allDays.filter { day ->
                day.note.isNotBlank() || day.toGod.isNotBlank() || day.fromGod.isNotBlank() ||
                day.prayer.isNotBlank() || day.thanks.isNotBlank() || day.kept.isNotBlank()
            }.forEach { day ->
                list.add(HistoryItem.DayRecord(day))
            }
        }

        // Apply search query filter if user typed text
        val query = searchQuery.trim()
        val filtered = if (query.isNotBlank()) {
            list.filter { item ->
                when (item) {
                    is HistoryItem.JournalReflection -> {
                        item.entry.text.contains(query, ignoreCase = true) ||
                        item.entry.step.contains(query, ignoreCase = true) ||
                        item.entry.verse.contains(query, ignoreCase = true) ||
                        item.entry.ref.contains(query, ignoreCase = true)
                    }
                    is HistoryItem.DayRecord -> {
                        item.day.note.contains(query, ignoreCase = true) ||
                        item.day.prayer.contains(query, ignoreCase = true) ||
                        item.day.toGod.contains(query, ignoreCase = true) ||
                        item.day.fromGod.contains(query, ignoreCase = true) ||
                        item.day.thanks.contains(query, ignoreCase = true) ||
                        item.day.date.contains(query, ignoreCase = true)
                    }
                }
            }
        } else {
            list
        }

        // Sort descending by dateSortKey
        filtered.sortedByDescending { it.dateSortKey }
    }

    // Delete Confirmation Dialog
    if (deletingEntryId != null) {
        AlertDialog(
            onDismissRequest = { deletingEntryId = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه اليومية من السجل؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingEntryId?.let { id ->
                            viewModel.deleteEntry(id)
                            Toast.makeText(context, "تم حذف اليومية", Toast.LENGTH_SHORT).show()
                        }
                        deletingEntryId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntryId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Journal Reflection Dialog
    if (editingJournalEntry != null) {
        EditJournalEntryDialog(
            entry = editingJournalEntry!!,
            onDismiss = { editingJournalEntry = null },
            onSave = { updatedTitle, updatedContent, updatedTag, updatedVerse, updatedRef ->
                viewModel.updateJournalEntry(
                    id = editingJournalEntry!!.id,
                    title = updatedTitle,
                    content = updatedContent,
                    verse = updatedVerse,
                    ref = updatedRef,
                    tag = updatedTag
                )
                Toast.makeText(context, "تم حفظ التعديلات بنجاح ✨", Toast.LENGTH_SHORT).show()
                editingJournalEntry = null
            }
        )
    }

    // Edit Day Record Dialog
    if (editingDayRecord != null) {
        EditDayReflectionDialog(
            day = editingDayRecord!!,
            onDismiss = { editingDayRecord = null },
            onSave = { updatedNote, updatedPrayer, updatedToGod, updatedFromGod, updatedThanks ->
                viewModel.updateDayReflection(
                    date = editingDayRecord!!.date,
                    note = updatedNote,
                    prayer = updatedPrayer,
                    toGod = updatedToGod,
                    fromGod = updatedFromGod,
                    thanks = updatedThanks
                )
                Toast.makeText(context, "تم تعديل يومية ${editingDayRecord!!.date} بنجاح ✨", Toast.LENGTH_SHORT).show()
                editingDayRecord = null
            }
        )
    }

    // View Journal Detail Dialog
    if (viewingDetailEntry != null) {
        JournalDetailDialog(
            entry = viewingDetailEntry!!,
            onDismiss = { viewingDetailEntry = null },
            onEdit = {
                val entryToEdit = viewingDetailEntry!!
                viewingDetailEntry = null
                editingJournalEntry = entryToEdit
            },
            onToggleFavorite = {
                viewingDetailEntry?.let { currentEntry ->
                    viewModel.toggleFavorite(currentEntry.id)
                    val newFav = !currentEntry.isFavorite
                    val msg = if (newFav) "تمت الإضافة للمفضلة 💛" else "تمت الإزالة من المفضلة"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewingDetailEntry = currentEntry.copy(ansNote = if (newFav) "fav" else "")
                }
            },
            onShare = {
                viewingDetailEntry?.let { shareJournalReflection(context, it) }
            }
        )
    }

    // View Day Detail Dialog
    if (viewingDayDetail != null) {
        DayDetailDialog(
            day = viewingDayDetail!!,
            onDismiss = { viewingDayDetail = null },
            onEdit = {
                val dayToEdit = viewingDayDetail!!
                viewingDayDetail = null
                editingDayRecord = dayToEdit
            },
            onOpenInDay = {
                val date = viewingDayDetail!!.date
                viewingDayDetail = null
                viewModel.setSelectedDate(date)
                onNavigateToDayWithDate?.invoke(date)
            },
            onShare = {
                viewingDayDetail?.let { shareDayReflection(context, it) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "سجل اليوميات والتأملات 📜",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "استعرض تأملاتك ويومياتك السابقة مع إمكانية التعديل والمراجعة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar for filtering past journal entries
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في محتوى وتأملات اليوميات…", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "بحث في اليوميات", tint = SacredGoldBadge)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "مسح البحث", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )

            // Search Results count indicator
            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_results_summary"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نتائج البحث عن \"$searchQuery\": ${sortedHistoryItems.size} مدخلة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { searchQuery = "" },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("مسح البحث", fontSize = 11.sp, color = SacredGoldBadge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            val filterOptions = listOf("الكل", "المفضلة 💛", "اليوميات", "الأيام")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = filter == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // LazyColumn displaying past journal entries sorted by date
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("history_entries_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
        ) {
            if (sortedHistoryItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = SacredGoldBadge.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = SacredGoldBadge,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "لا توجد مدخلات تطابق: \"$searchQuery\"" else "لا توجد يوميات أو تأملات مسجلة بعد.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "جرّب البحث بكلمة مختلفة أو جزء من آية أو تاريخ آخر." else "ابدأ بتدوين أول تأمل من الزر العائم أو الواجهة الرئيسية.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("clear_search_empty_state_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مسح البحث وعرض كل اليوميات", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = sortedHistoryItems,
                    key = { item ->
                        when (item) {
                            is HistoryItem.JournalReflection -> "journal_${item.entry.id}"
                            is HistoryItem.DayRecord -> "day_${item.day.date}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is HistoryItem.JournalReflection -> {
                            JournalReflectionCard(
                                entry = item.entry,
                                onViewDetail = { viewingDetailEntry = item.entry },
                                onEdit = { editingJournalEntry = item.entry },
                                onDelete = { deletingEntryId = item.entry.id },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(item.entry.id)
                                    val msg = if (item.entry.isFavorite) "تمت الإزالة من المفضلة" else "تمت الإضافة للمفضلة 💛"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    shareJournalReflection(context, item.entry)
                                }
                            )
                        }
                        is HistoryItem.DayRecord -> {
                            DayReflectionCard(
                                day = item.day,
                                onViewDetail = { viewingDayDetail = item.day },
                                onEdit = { editingDayRecord = item.day },
                                onOpenInDay = {
                                    viewModel.setSelectedDate(item.day.date)
                                    onNavigateToDayWithDate?.invoke(item.day.date)
                                },
                                onShare = {
                                    shareDayReflection(context, item.day)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalReflectionCard(
    entry: EntryEntity,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .testTag("journal_reflection_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, if (entry.isFavorite) SacredGoldBadge else SacredGoldBadge.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Date and Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = SacredGoldBadge,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.ref.ifBlank { "تأمل سابق" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SacredGoldBadge.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = entry.moodTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (entry.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "مفضل",
                            tint = SacredGoldBadge,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (entry.step.isNotBlank() && entry.step != entry.moodTag) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.step,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attached Bible Verse
            if (entry.verse.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (entry.ptext.isNotBlank()) {
                            Text(
                                text = "📖 ${entry.ptext}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = entry.verse,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Reflection Text
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (تعديل، مشاركة، مفضلة، حذف)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (entry.isFavorite) SacredGoldBadge else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onViewDetail,
                        modifier = Modifier.testTag("view_journal_detail_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SacredGoldBadge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "عرض التفاصيل",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGoldBadge.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("edit_reflection_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "تعديل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DayReflectionCard(
    day: DayEntity,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onOpenInDay: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .testTag("day_reflection_card_${day.date}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date and Kept Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "يومية ${day.date}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (day.kept.isNotBlank()) {
                    val (badgeText, badgeColor) = when (day.kept) {
                        "y" -> "وفيت بالوعد" to SacredKeptYes
                        "p" -> "وفاء جزئي" to SacredKeptPartial
                        else -> "لم أفِ بالوعد" to SacredKeptNo
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (day.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = day.note,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (day.prayer.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "صلاتي: ${day.prayer}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (day.toGod.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "وعدي للرب: ${day.toGod}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (day.thanks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "شكر اليوم: ${day.thanks}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_day_button_${day.date}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة اليومية",
                            tint = SacredGoldBadge,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    TextButton(onClick = onOpenInDay) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صفحة اليوم", fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onViewDetail,
                        modifier = Modifier.testTag("view_day_detail_button_${day.date}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SacredGoldBadge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التفاصيل", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGoldBadge.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("edit_day_button_${day.date}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditJournalEntryDialog(
    entry: EntryEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tag: String, verse: String, ref: String) -> Unit
) {
    var title by remember { mutableStateOf(entry.step) }
    var content by remember { mutableStateOf(entry.text) }
    var tag by remember { mutableStateOf(entry.step.ifBlank { "تأمل روحي 🌿" }) }
    var verseText by remember { mutableStateOf(entry.verse) }
    var verseRef by remember { mutableStateOf(entry.ptext) }
    var showBiblePicker by remember { mutableStateOf(false) }

    if (showBiblePicker) {
        BiblePickerDialog(
            title = "البحث في الكتاب المقدس وإدراج آية",
            onDismiss = { showBiblePicker = false },
            onPickVerse = { ref, text ->
                verseRef = ref
                verseText = text
            },
            onInsertVerseIntoText = { ref, text ->
                val formattedQuote = "«${text.trim()}»\n($ref)"
                content = if (content.isBlank()) formattedQuote else "$content\n\n$formattedQuote"
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل التأمل واليومية ✏️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان التأمل (اختياري)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Verse Attachment
                if (verseText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📖 $verseRef",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(text = verseText, fontSize = 11.sp, maxLines = 2)
                            }
                            IconButton(onClick = {
                                verseText = ""
                                verseRef = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة الآية", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showBiblePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرفاق آية من الكتاب المقدس", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Large Content Text Field
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("نص التأمل") },
                    minLines = 8,
                    maxLines = 16,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("edit_reflection_content_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(title.trim(), content.trim(), tag, verseText, verseRef)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_edited_reflection_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditDayReflectionDialog(
    day: DayEntity,
    onDismiss: () -> Unit,
    onSave: (note: String, prayer: String, toGod: String, fromGod: String, thanks: String) -> Unit
) {
    var note by remember { mutableStateOf(day.note) }
    var prayer by remember { mutableStateOf(day.prayer) }
    var toGod by remember { mutableStateOf(day.toGod) }
    var fromGod by remember { mutableStateOf(day.fromGod) }
    var thanks by remember { mutableStateOf(day.thanks) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل يومية ${day.date} ✏️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("الملاحظات والتأمل الحر") },
                            minLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_day_note_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = prayer,
                            onValueChange = { prayer = it },
                            label = { Text("صلاتي") },
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = toGod,
                            onValueChange = { toGod = it },
                            label = { Text("وعدي للرب") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = fromGod,
                            onValueChange = { fromGod = it },
                            label = { Text("وعد الرب لي") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = thanks,
                            onValueChange = { thanks = it },
                            label = { Text("نعم أشكر عليها") },
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSave(note.trim(), prayer.trim(), toGod.trim(), fromGod.trim(), thanks.trim())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_edited_day_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JournalDetailDialog(
    entry: EntryEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val wordCount = remember(entry.text) {
        entry.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val charCount = entry.text.length

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.5f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = SacredGoldBadge,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تفاصيل التأمل واليومية ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = entry.ref.ifBlank { "تاريخ سابق" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_journal_detail_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mood and Favorite Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SacredGoldBadge.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = entry.moodTag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (entry.isFavorite) SacredGoldBadge else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = SacredGoldBadge
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(entry.text))
                                Toast.makeText(context, "تم نسخ نص اليومية 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (entry.step.isNotBlank() && entry.step != entry.moodTag) {
                        Text(
                            text = entry.step,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Attached Scripture Verse
                    if (entry.verse.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (entry.ptext.isNotBlank()) {
                                    Text(
                                        text = "📖 ${entry.ptext}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = entry.verse,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Main Text
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount كلمة • $charCount حرف",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGoldBadge.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل التأمل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DayDetailDialog(
    day: DayEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenInDay: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.5f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تفاصيل يومية ${day.date}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_day_detail_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kept Promise Status & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (day.kept.isNotBlank()) {
                        val (badgeText, badgeColor) = when (day.kept) {
                            "y" -> "وفيت بالوعد" to SacredKeptYes
                            "p" -> "وفاء جزئي" to SacredKeptPartial
                            else -> "لم أفِ بالوعد" to SacredKeptNo
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "سجل اليوم الكامل 📜",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = SacredGoldBadge)
                        }
                        IconButton(
                            onClick = {
                                val fullText = buildString {
                                    append("يومية ${day.date}\n")
                                    if (day.prayer.isNotBlank()) append("صلاتي: ${day.prayer}\n")
                                    if (day.toGod.isNotBlank()) append("وعدي: ${day.toGod}\n")
                                    if (day.thanks.isNotBlank()) append("شكري: ${day.thanks}\n")
                                    if (day.note.isNotBlank()) append("التأمل: ${day.note}\n")
                                }
                                clipboardManager.setText(AnnotatedString(fullText))
                                Toast.makeText(context, "تم نسخ تفاصيل اليومية 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (day.verse.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📖 آية اليوم: ${day.ref}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = day.verse,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (day.note.isNotBlank()) {
                        Column {
                            Text("✍️ ملاحظات وتأملات اليوم:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day.note, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                        }
                    }

                    if (day.prayer.isNotBlank()) {
                        Column {
                            Text("🙏 صلاتي القلبية:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day.prayer, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }

                    if (day.toGod.isNotBlank()) {
                        Column {
                            Text("⚓ وعدي والتزامي للرب:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day.toGod, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }

                    if (day.fromGod.isNotBlank()) {
                        Column {
                            Text("🌿 وعد الرب لي:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day.fromGod, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }

                    if (day.thanks.isNotBlank()) {
                        Column {
                            Text("💛 نعم أشكر عليها اليوم:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day.thanks, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onOpenInDay) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح في صفحة اليوم", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGoldBadge.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل اليومية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun shareJournalReflection(context: Context, entry: EntryEntity) {
    val shareText = buildString {
        append("🕊️ من تأملاتي في «يوميتي»\n\n")
        if (entry.step.isNotBlank()) {
            append("📌 ${entry.step}\n\n")
        }
        if (entry.verse.isNotBlank()) {
            append("📖 «${entry.verse.trim()}»\n— ${entry.ptext.ifBlank { "آية اليومية" }}\n\n")
        }
        append("✍️ التأمل:\n${entry.text}\n")
        if (entry.ref.isNotBlank()) {
            append("\n📅 ${entry.ref}\n")
        }
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة التأمل الروحي 🕊️"))
}

fun shareDayReflection(context: Context, day: DayEntity) {
    val shareText = buildString {
        append("🕊️ يومية ${day.date} — «يوميتي»\n\n")
        if (day.verse.isNotBlank()) {
            append("📖 آية اليوم: «${day.verse.trim()}» (${day.ref})\n\n")
        }
        if (day.prayer.isNotBlank()) {
            append("🙏 صلاتي: ${day.prayer}\n\n")
        }
        if (day.toGod.isNotBlank()) {
            append("⚓ وعدي للرب: ${day.toGod}\n\n")
        }
        if (day.thanks.isNotBlank()) {
            append("💛 شكر ومحبة: ${day.thanks}\n\n")
        }
        if (day.note.isNotBlank()) {
            append("✍️ ملاحظات وتأملات:\n${day.note}\n\n")
        }
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة يومية ${day.date} 🕊️"))
}

