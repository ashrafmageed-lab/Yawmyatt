package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayEntity
import com.example.data.repository.YawmiyatiRepository
import com.example.ui.components.BiblePickerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.YawmiyatiViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    viewModel: YawmiyatiViewModel,
    onOpenNewJournalEntry: () -> Unit,
    onNavigateToDay: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()
    val prayers by viewModel.prayers.collectAsState()
    val fears by viewModel.fears.collectAsState()

    val effectiveDay = currentDay ?: DayEntity(date = selectedDate)

    // Quick Action Dialog State
    var showQuickWriteDialog by remember { mutableStateOf(false) }
    var quickWriteType by remember { mutableStateOf("prayer") } // "prayer", "toGod", "todo", "thanks"
    var quickWriteInput by remember { mutableStateOf("") }
    var showBiblePicker by remember { mutableStateOf(false) }

    // Dynamic Greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "صباح مبارك بنعمة وسلام ☀️"
            in 12..16 -> "يوم مبارك في محضر الرب 🌿"
            in 17..22 -> "مساء هادئ ومقدس 🌙"
            else -> "ليلة هادئة في سلام المسيح ✨"
        }
    }

    // Friendly Arabic Date Format
    val arabicDateText = remember(selectedDate) {
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)
            if (parsed != null) {
                SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(parsed)
            } else selectedDate
        } catch (_: Exception) {
            selectedDate
        }
    }

    // Fear of the day
    val fearWithPromise = remember(fears, selectedDate) {
        val validFears = fears.filter { it.ref.isNotBlank() && it.ptext.isNotBlank() }
        if (validFears.isNotEmpty()) {
            val hash = abs(selectedDate.replace("-", "").toIntOrNull() ?: selectedDate.hashCode())
            validFears[hash % validFears.size]
        } else null
    }

    // Quick Write Dialog
    if (showQuickWriteDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuickWriteDialog = false
                quickWriteInput = ""
            },
            title = {
                Text(
                    text = when (quickWriteType) {
                        "prayer" -> "صلاتي الصباحية 🙏"
                        "toGod" -> "وعدي للرب النهاردة 🤝"
                        "todo" -> "عمل ألتزم بيه النهاردة 🎯"
                        "thanks" -> "شكر لربنا من القلب 💛"
                        else -> "كتابة سريعة في يوميتي ✨"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = when (quickWriteType) {
                            "prayer" -> "اكتب صلاتك القصيرة ليسوع في بداية نهارك:"
                            "toGod" -> "ما هو الوعد اللطيف اللي حابب تلتزم بيه أمام الله اليوم؟"
                            "todo" -> "اكتب مهمة أو خطوة روحية/عملية ترغب في تحقيقها:"
                            "thanks" -> "اذكر نعمة أو أمراً جميلاً تشكر ربنا عليه:"
                            else -> "اكتب ما يجول في قلبك:"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quickWriteInput,
                        onValueChange = { quickWriteInput = it },
                        placeholder = { Text("اكتب هنا…", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_write_input"),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = quickWriteInput.trim()
                        if (text.isNotBlank()) {
                            when (quickWriteType) {
                                "prayer" -> viewModel.updateDayField(prayer = text)
                                "toGod" -> viewModel.updateDayField(toGod = text)
                                "todo" -> viewModel.addTodo(text)
                                "thanks" -> {
                                    val currentThanks = effectiveDay.thanks
                                    val updated = if (currentThanks.isBlank()) text else "$currentThanks\n• $text"
                                    viewModel.updateDayField(thanks = updated)
                                }
                            }
                        }
                        showQuickWriteDialog = false
                        quickWriteInput = ""
                    },
                    modifier = Modifier.testTag("save_quick_entry_button")
                ) {
                    Text("حفظ في اليومية")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuickWriteDialog = false
                        quickWriteInput = ""
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBiblePicker) {
        BiblePickerDialog(
            onDismiss = { showBiblePicker = false },
            onPickVerse = { ref, text ->
                viewModel.updateDayField(ref = ref, verse = text)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. Spiritual Greeting & Date Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SacredGoldBadge.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SacredGoldBadge)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "يوميتي • مساحتك الروحية الهادئة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = arabicDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val reminders by viewModel.reminders.collectAsState()
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSettings() }
                        .testTag("dashboard_reminder_status_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (reminders.enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = SacredGoldBadge,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (reminders.enabled) {
                                "التذكير اليومي مفعل: %02d:%02d".format(reminders.dailyHour, reminders.dailyMinute)
                            } else {
                                "التذكيرات متوقفة — اضغط لضبط موعد يومي لكتابة اليومية"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (reminders.enabled) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "الإعدادات",
                            fontSize = 11.sp,
                            color = SacredGoldBadge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1.5. أيام الأمانة ونبض السلام والمفضلة
        item {
            val streak by viewModel.streak.collectAsState()
            val journalEntries by viewModel.journalEntries.collectAsState()
            val favoritesCount = remember(journalEntries) { journalEntries.count { it.isFavorite } }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rhythm / Streak Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("spiritual_rhythm_card"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, SacredGoldBadge.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (streak > 0) "🔥" else "🌿", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أمانة التدوين",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (streak > 0) "$streak أيام متتالية" else "يوم جديد مبارك",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (streak > 0) "أمانة هادئة مع الرب" else "«مَرَاحِمُهُ جَدِيدَةٌ كُلَّ صَبَاحٍ»",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Favorites Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHistory() }
                        .testTag("dashboard_favorites_card"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, SacredGoldBadge.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = SacredGoldBadge,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تأملاتي المفضلة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$favoritesCount تأمل مميز",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اضغط لعرض المفضلة 💛",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Primary Sacred Gold Action Banner ("كتابة يومية جديدة")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_journal_primary_card")
                    .clickable { onOpenNewJournalEntry() },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, SacredGoldBadge),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2E2413),
                                    Color(0xFF1E1A12),
                                    Color(0xFF141311)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تدوين يومية جديدة ✨",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SacredGoldPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "حفظ تلقائي للوقت والتاريخ مع مساحة رحبة للتأمل والسكينة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDDD2C2),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenNewJournalEntry,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SacredGoldBadge,
                                        contentColor = Color(0xFF2A1C00)
                                    ),
                                    modifier = Modifier.testTag("open_new_entry_editor_button")
                                ) {
                                    Text(
                                        text = "ابدأ التدوين الآن",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onNavigateToDay,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("open_day_journal_button")
                                ) {
                                    Text(
                                        text = "صفحة اليوم",
                                        fontSize = 12.sp,
                                        color = SacredGoldBadge
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.4f)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "كتابة",
                                    tint = SacredGoldPrimaryDark,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Actions Grid (٤ أزرار سريعة ومباشرة)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تسجيل سريع في ثوانٍ ⚡",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "صلاتي",
                        subtitle = "وقفة الصباح",
                        icon = Icons.Default.FavoriteBorder,
                        color = SacredGoldBadge,
                        onClick = {
                            quickWriteType = "prayer"
                            quickWriteInput = effectiveDay.prayer
                            showQuickWriteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_prayer_tile")
                    )

                    QuickActionTile(
                        title = "وعدي",
                        subtitle = "التزامي للرب",
                        icon = Icons.Default.Handshake,
                        color = Color(0xFF5BA4E5),
                        onClick = {
                            quickWriteType = "toGod"
                            quickWriteInput = effectiveDay.toGod
                            showQuickWriteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_promise_tile")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "أعمل النهاردة",
                        subtitle = "هدف أو طاعة",
                        icon = Icons.Default.CheckCircleOutline,
                        color = SacredKeptYes,
                        onClick = {
                            quickWriteType = "todo"
                            quickWriteInput = ""
                            showQuickWriteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_todo_tile")
                    )

                    QuickActionTile(
                        title = "شكر ومحبة",
                        subtitle = "نعم اليوم",
                        icon = Icons.Default.VolunteerActivism,
                        color = SacredKeptPartial,
                        onClick = {
                            quickWriteType = "thanks"
                            quickWriteInput = ""
                            showQuickWriteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_thanks_tile")
                    )
                }
            }
        }

        // 4. آية اليوم المقدسة (Sacred Verse of the Day Card)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.7f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "آية ووعد اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showBiblePicker = true },
                            modifier = Modifier.testTag("browse_bible_button")
                        ) {
                            Text("من الكتاب المقدس", fontSize = 12.sp, color = SacredGoldBadge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val verseText = effectiveDay.verse.ifBlank {
                        "«الرَّبُّ رَاعِيَّ فَلَا يُعْوِزُنِي شَيْءٌ. فِي مَرَاعٍ خُضْرٍ يُرْبِضُنِي. إِلَى مِيَاهِ ٱلرَّاحَةِ يُورِدُنِي.»"
                    }
                    val verseRef = effectiveDay.ref.ifBlank { "مزمور ٢٣: ١-٢" }

                    Text(
                        text = verseText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = verseRef,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 5. حالة الوفاء والإنجاز لليوم (Today's Spiritual Rhythm Indicators)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محطات يومك 🕊️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "خطوات يومية خفيفة بدون لوم أو إثقال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val hasPrayer = effectiveDay.prayer.isNotBlank()
                    val hasVerse = effectiveDay.verse.isNotBlank()
                    val hasPromise = effectiveDay.toGod.isNotBlank()
                    val hasThanks = effectiveDay.thanks.isNotBlank()

                    RhythmCheckItem(title = "صلاتي الصباحية", isCompleted = hasPrayer)
                    RhythmCheckItem(title = "آية اليوم من كلمة الله", isCompleted = hasVerse)
                    RhythmCheckItem(title = "وعدي والتزامي لليوم", isCompleted = hasPromise)
                    RhythmCheckItem(title = "شكر الليل والوفاء بالوعد", isCompleted = hasThanks)
                }
            }
        }

        // 6. همسة طمأنينة لو الخوف جالك (Spiritual Peace Card)
        if (fearWithPromise != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SacredGoldBadge,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "همسة طمأنينة: لو الخوف جالك",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "بخصوص «${fearWithPromise.text}»:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«${fearWithPromise.ptext}» — ${fearWithPromise.ref}",
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 7. صلاة قلبية مفتوحة للتأمل
        val openPrayer = prayers.firstOrNull { it.ansDate == null }
        if (openPrayer != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "صلاة معلقة في قلبك 🙏",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "«${openPrayer.text}»",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("فتح أرشيف الصلوات", fontSize = 12.sp, color = SacredGoldBadge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RhythmCheckItem(
    title: String,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isCompleted) SacredKeptYes.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم",
                        tint = SacredKeptYes,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
