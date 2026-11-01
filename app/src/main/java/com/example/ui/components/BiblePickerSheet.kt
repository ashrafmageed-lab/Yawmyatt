package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bible.BibleManager
import com.example.bible.BibleScope
import com.example.bible.BibleVerseHit
import com.example.ui.theme.SacredGoldBadge
import com.example.ui.theme.SacredGoldPrimaryDark
import kotlinx.coroutines.launch

enum class BiblePickerMode {
    SEARCH,
    BROWSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblePickerDialog(
    title: String = "البحث في الكتاب المقدس (فاندايك)",
    initialSearchQuery: String = "",
    onDismiss: () -> Unit,
    onPickVerse: (reference: String, text: String) -> Unit,
    onInsertVerseIntoText: ((reference: String, text: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var pickerMode by remember { mutableStateOf(BiblePickerMode.SEARCH) }
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedScope by remember { mutableStateOf(BibleScope.ALL) }
    var searchHits by remember { mutableStateOf<List<BibleVerseHit>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Browse Mode state
    var selectedBookIndex by remember { mutableIntStateOf(18) } // Default to Psalms (المزامير)
    var selectedChapterIndex by remember { mutableIntStateOf(22) } // Psalm 23 (index 22)
    val bible = remember { BibleManager.getBible() }

    // Preset quick spiritual topics
    val spiritualTopics = listOf(
        "سلام وطمأنينة 🕊️" to "سلام",
        "رجاء ووعد ✨" to "رجاء",
        "المحبة 💛" to "المحبة",
        "لا تخف 🛡️" to "لا تخف",
        "مزمور ٢٣ 🌿" to "مزمور 23",
        "شكر وتسبيح 🎵" to "شكر",
        "الرب راعي 🐑" to "الرب راعي",
        "حكمة وإرشاد 💡" to "حكمة"
    )

    fun performSearch(query: String, scope: BibleScope) {
        if (query.trim().length >= 2) {
            coroutineScope.launch {
                isSearching = true
                searchHits = BibleManager.search(query = query, scope = scope, maxHits = 60)
                isSearching = false
            }
        } else {
            searchHits = emptyList()
        }
    }

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotBlank()) {
            performSearch(initialSearchQuery, selectedScope)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("bible_search_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "نص كامل أوفلاين بدون إنترنت (الترجمة العربية المشتركة/فاندايك)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_bible_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs (Search vs Browse)
                TabRow(
                    selectedTabIndex = if (pickerMode == BiblePickerMode.SEARCH) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = pickerMode == BiblePickerMode.SEARCH,
                        onClick = { pickerMode = BiblePickerMode.SEARCH },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("البحث في الآيات والشواهد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_bible_search")
                    )
                    Tab(
                        selected = pickerMode == BiblePickerMode.BROWSE,
                        onClick = { pickerMode = BiblePickerMode.BROWSE },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصفح الأسفار والإصحاحات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_bible_browse")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (pickerMode == BiblePickerMode.SEARCH) {
                    // Search Bar with Clear Button
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            performSearch(q, selectedScope)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bible_search_input"),
                        placeholder = { Text("ابحث بكلمة (سلام، محبة، قوة) أو شاهد (مزمور 23، يوحنا 3)...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "بحث", tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchHits = emptyList()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Spiritual Topic Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(spiritualTopics) { (label, keyword) ->
                            AssistChip(
                                onClick = {
                                    searchQuery = keyword
                                    performSearch(keyword, selectedScope)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Testament / Scope Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BibleScope.values()) { scope ->
                            val isSelected = scope == selectedScope
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedScope = scope
                                    if (searchQuery.isNotBlank()) {
                                        performSearch(searchQuery, scope)
                                    }
                                },
                                label = { Text(scope.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGoldBadge,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Results Header
                    if (searchQuery.trim().length >= 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نتائج البحث: تم العثور على (${searchHits.size}) آية",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (searchHits.isNotEmpty()) {
                                Text(
                                    text = "اضغط للإدراج أو الإرفاق",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "اكتب كلمتين أو حرفين على الأقل للبحث في كامل الكتاب المقدس",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Results List
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "جاري البحث في أسفار الكتاب المقدس...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchQuery.trim().length >= 2 && searchHits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لم يتم العثور على آيات تطابق «$searchQuery»",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "جرّب البحث بكلمة مرادفة أخرى أو تأكد من اختيار نطاق «كل الكتاب»",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchHits.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .testTag("bible_search_results_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchHits) { hit ->
                                VerseSearchResultCard(
                                    hit = hit,
                                    onInsertText = if (onInsertVerseIntoText != null) {
                                        {
                                            onInsertVerseIntoText(hit.reference, hit.text)
                                            Toast.makeText(context, "تم إدراج الآية في اليومية بنجاح ✍️", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    } else null,
                                    onAttachVerse = {
                                        onPickVerse(hit.reference, hit.text)
                                        Toast.makeText(context, "تم إرفاق الآية لليومية بنجاح 📎", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onCopyVerse = {
                                        clipboardManager.setText(AnnotatedString(hit.toJournalQuote()))
                                        Toast.makeText(context, "تم نسخ الآية والشاهد إلى الحافظة", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    } else {
                        // Helpful Initial Empty State with instructions
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = SacredGoldBadge,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ابحث في كلمة الله وضمّنها في يوميتك",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "يمكنك كتابة أي كلمة مفتاحية مثل (المحبة، سلام، صلاة، قوة) أو كتابة شاهد محدد مثل (مزمور 23 أو يوحنا 3:16) للوصول المباشر وإدراجها بنقرة واحدة.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                } else {
                    // Browse Mode: Book -> Chapter -> Verses
                    if (bible == null || bible.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            // Books horizontal list
                            Text(
                                text = "السفر (اختر من ٦٦ سفر)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(BibleManager.BOOKS) { idx, bookName ->
                                    val isSelected = idx == selectedBookIndex
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedBookIndex = idx
                                            selectedChapterIndex = 0
                                        },
                                        label = { Text(bookName, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Chapters horizontal list
                            val currentBook = bible.getOrNull(selectedBookIndex) ?: emptyList()
                            val chapterCount = currentBook.size

                            Text(
                                text = "الإصحاح (${selectedChapterIndex + 1} من أصل $chapterCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(chapterCount) { cIdx ->
                                    val isSelected = cIdx == selectedChapterIndex
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedChapterIndex = cIdx },
                                        label = { Text("${cIdx + 1}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SacredGoldBadge,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // Verses list in chapter
                            val currentChapter = currentBook.getOrNull(selectedChapterIndex) ?: emptyList()
                            val bookName = BibleManager.BOOKS.getOrElse(selectedBookIndex) { "" }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(currentChapter) { vIdx, verseText ->
                                    val ref = "$bookName ${selectedChapterIndex + 1}:${vIdx + 1}"
                                    val hit = BibleVerseHit(
                                        bookIndex = selectedBookIndex,
                                        chapterIndex = selectedChapterIndex,
                                        verseIndex = vIdx,
                                        bookName = bookName,
                                        reference = ref,
                                        text = verseText,
                                        isExactMatch = true
                                    )
                                    VerseSearchResultCard(
                                        hit = hit,
                                        onInsertText = if (onInsertVerseIntoText != null) {
                                            {
                                                onInsertVerseIntoText(ref, verseText)
                                                Toast.makeText(context, "تم إدراج الآية في اليومية بنجاح ✍️", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        } else null,
                                        onAttachVerse = {
                                            onPickVerse(ref, verseText)
                                            Toast.makeText(context, "تم إرفاق الآية لليومية بنجاح 📎", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        onCopyVerse = {
                                            clipboardManager.setText(AnnotatedString(hit.toJournalQuote()))
                                            Toast.makeText(context, "تم نسخ الآية إلى الحافظة", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerseSearchResultCard(
    hit: BibleVerseHit,
    onInsertText: (() -> Unit)?,
    onAttachVerse: () -> Unit,
    onCopyVerse: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verse_hit_${hit.reference}"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Reference Badge and Copy Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SacredGoldBadge.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = hit.reference,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onCopyVerse,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ الآية",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Verse Full Text
            Text(
                text = hit.text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons to Include in Journal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onInsertText != null) {
                    Button(
                        onClick = onInsertText,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إدراج في النص",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAttachVerse,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (onInsertText != null) "إرفاق كآية اليومية" else "اختيار هذه الآية",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
