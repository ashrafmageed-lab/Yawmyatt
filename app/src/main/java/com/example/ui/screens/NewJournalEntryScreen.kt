package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BiblePickerDialog
import com.example.ui.theme.SacredCardBorder
import com.example.ui.theme.SacredGoldBadge
import com.example.ui.theme.SacredGoldPrimaryDark
import com.example.ui.viewmodel.YawmiyatiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJournalEntryScreen(
    viewModel: YawmiyatiViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Automatic Date & Time capturing
    val now = remember { Date() }
    val autoDateText = remember(now) {
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(now)
    }
    val autoTimeText = remember(now) {
        SimpleDateFormat("hh:mm a", Locale("ar")).format(now)
    }
    val fullTimestamp = remember(now) {
        SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.US).format(now)
    }

    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("سلام عميق 🕊️") }
    var isFavorite by remember { mutableStateOf(false) }
    var attachedVerseRef by remember { mutableStateOf("") }
    var attachedVerseText by remember { mutableStateOf("") }
    var showBiblePicker by remember { mutableStateOf(false) }

    val tags = listOf(
        "سلام عميق 🕊️",
        "شكر وامتنان 💛",
        "صلاة وتضرع 🙏",
        "فرح وهدوء ✨",
        "حديث مع الرب 🌿",
        "رجاء ووعد ⚓",
        "تعزية في ضيق 🛡️"
    )

    if (showBiblePicker) {
        BiblePickerDialog(
            title = "البحث في الكتاب المقدس وإدراج آية",
            onDismiss = { showBiblePicker = false },
            onPickVerse = { ref, text ->
                attachedVerseRef = ref
                attachedVerseText = text
            },
            onInsertVerseIntoText = { ref, text ->
                val formattedQuote = "«${text.trim()}»\n($ref)"
                contentText = if (contentText.isBlank()) {
                    formattedQuote
                } else {
                    "$contentText\n\n$formattedQuote"
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تدوين يومية جديدة ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_new_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                            val msg = if (isFavorite) "تمت الإضافة للمفضلة 💛" else "تمت الإزالة من المفضلة"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("toggle_favorite_entry_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "تمييز بالمفضلة",
                            tint = if (isFavorite) SacredGoldBadge else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (contentText.trim().isBlank()) {
                                Toast.makeText(context, "اكتب كلماتك وتأملك أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addJournalEntry(
                                title = titleText.trim(),
                                content = contentText.trim(),
                                timestamp = "$autoDateText • $autoTimeText",
                                verse = attachedVerseText,
                                ref = attachedVerseRef,
                                tag = selectedTag,
                                mood = selectedTag,
                                isFav = isFavorite
                            )
                            Toast.makeText(context, "تم حفظ اليومية مع الوقت والتاريخ بنجاح ✨", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_journal_entry_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Automatic Timestamp Card with Sacred Gold Accent
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_timestamp_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, SacredGoldBadge.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "الوقت",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = autoDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يُحفظ تلقائيًا في: $autoTimeText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SacredGoldBadge.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "تلقائي ⚡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Category / Mood Pills Row
            Column {
                Text(
                    text = "نوع التأمل أو المشاعر 🏷️",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tags) { tag ->
                        val isSelected = tag == selectedTag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Optional Title Input Field
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("عنوان اليومية أو الكلمة الافتتاحية (اختياري)") },
                placeholder = { Text("مثال: في هدوء الصباح، نعم الرب في ضيقتي…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_title_input")
            )

            // Attached Bible Verse or Search Card
            if (attachedVerseText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, SacredGoldBadge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آية مرفقة: $attachedVerseRef",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    attachedVerseRef = ""
                                    attachedVerseText = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة الآية", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = attachedVerseText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val formattedQuote = "«${attachedVerseText.trim()}»\n($attachedVerseRef)"
                                    contentText = if (contentText.isBlank()) formattedQuote else "$contentText\n\n$formattedQuote"
                                    Toast.makeText(context, "تم إدراج نص الآية في اليومية ✍️", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إدراج نصها في اليومية", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(onClick = { showBiblePicker = true }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تغيير الآية", fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_offline_bible_card"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, SacredGoldBadge.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "البحث في الكتاب المقدس وإدراج آية 📖",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ابحث بالكلمات أو الشواهد لإدراجها في نص تأملك",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showBiblePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("open_bible_search_button")
                        ) {
                            Text("بحث الآيات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Large Expansive Text Editor Field
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ما يفيض به القلب والروح ✍️",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { showBiblePicker = true },
                            modifier = Modifier.testTag("inline_search_bible_button")
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp), tint = SacredGoldBadge)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اقتباس آية 📖", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        placeholder = {
                            Text(
                                text = "اكتب ما يفيض به قلبك في محضر الرب…\n\nحديثك الشخصي، صلاتك، شكرك، أفكارك، مشاعرك، أو ما علّمك إياه الروح القدس خلال اليوم…",
                                fontSize = 14.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        minLines = 14,
                        maxLines = 40,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_large_content_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Character & Word Counter
                    val wordsCount = remember(contentText) {
                        contentText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    }
                    val charsCount = contentText.length

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordsCount كلمة • $charsCount حرف",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (contentText.isNotBlank()) {
                            TextButton(
                                onClick = { contentText = "" },
                                modifier = Modifier.size(width = 64.dp, height = 32.dp)
                            ) {
                                Text("مسح", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Bottom Primary Save Button
            Button(
                onClick = {
                    if (contentText.trim().isBlank()) {
                        Toast.makeText(context, "اكتب كلماتك وتأملك أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addJournalEntry(
                        title = titleText.trim(),
                        content = contentText.trim(),
                        timestamp = "$autoDateText • $autoTimeText",
                        verse = attachedVerseText,
                        ref = attachedVerseRef,
                        tag = selectedTag,
                        mood = selectedTag,
                        isFav = isFavorite
                    )
                    Toast.makeText(context, "تم حفظ اليومية مع الوقت والتاريخ بنجاح ✨", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_journal_entry_bottom_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حفظ اليومية تلقائياً مع التاريخ والوقت",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
