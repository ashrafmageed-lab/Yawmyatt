package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayEntity
import com.example.data.model.EntryEntity
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
fun DayScreen(
    viewModel: YawmiyatiViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()
    val fears by viewModel.fears.collectAsState()

    var showBiblePicker by remember { mutableStateOf(false) }
    var showLimitWarningDialog by remember { mutableStateOf(false) }
    var pendingTodoText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val effectiveDay = currentDay ?: DayEntity(date = selectedDate)

    // Calculate fear of the day
    val fearsWithPromises = remember(fears) {
        fears.filter { it.ref.isNotBlank() && it.ptext.isNotBlank() }
    }
    val fearOfTheDay = remember(fearsWithPromises, selectedDate) {
        if (fearsWithPromises.isNotEmpty()) {
            val hash = abs(selectedDate.replace("-", "").toIntOrNull() ?: selectedDate.hashCode())
            fearsWithPromises[hash % fearsWithPromises.size]
        } else null
    }

    if (showBiblePicker) {
        BiblePickerDialog(
            onDismiss = { showBiblePicker = false },
            onPickVerse = { ref, text ->
                viewModel.updateDayField(ref = ref, verse = text)
            }
        )
    }

    if (showLimitWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLimitWarningDialog = false },
            title = { Text("تنبيه لطيف") },
            text = { Text("الحد المقترح هو ٣ مهام لليوم عشان تضمن التركيز والراحة. تحب تضيف برضه؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pendingTodoText.isNotBlank()) {
                            viewModel.addTodo(pendingTodoText)
                            pendingTodoText = ""
                        }
                        showLimitWarningDialog = false
                    }
                ) {
                    Text("أيوة، أضفها")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitWarningDialog = false }) {
                    Text("إلغاء")
                }
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
        // Date Selector Bar
        item {
            DateSelectorBar(
                currentDate = selectedDate,
                onDateSelected = { newDate -> viewModel.setSelectedDate(newDate) }
            )
        }

        // Card 1: آية اليوم (Golden Card)
        item {
            VerseCard(
                ref = effectiveDay.ref,
                verse = effectiveDay.verse,
                onOpenBiblePicker = { showBiblePicker = true },
                onRefChange = { viewModel.updateDayField(ref = it) },
                onVerseChange = { viewModel.updateDayField(verse = it) }
            )
        }

        // Card 2: لو الخوف جالك (Prompt card)
        if (fearOfTheDay != null) {
            item {
                FearPromptCard(
                    fear = fearOfTheDay,
                    onTakeAsPromise = {
                        viewModel.updateDayField(fromGod = "${fearOfTheDay.ref}: ${fearOfTheDay.ptext}")
                    }
                )
            }
        }

        // Card 3: أعمل النهاردة
        item {
            val todos = remember(effectiveDay.todosJson) {
                YawmiyatiRepository.parseItems(effectiveDay.todosJson)
            }
            ListSectionCard(
                title = "أعمل النهاردة",
                subtitle = "حد أقصى ٣ أفعال للتركيز والراحة",
                items = todos,
                canCheck = true,
                onToggle = { idx, done -> viewModel.toggleTodo(idx, done) },
                onDelete = { idx -> viewModel.removeTodo(idx) },
                onAdd = { text ->
                    if (todos.size >= 3) {
                        pendingTodoText = text
                        showLimitWarningDialog = true
                    } else {
                        viewModel.addTodo(text)
                    }
                }
            )
        }

        // Card 4: ما أعملش النهاردة
        item {
            val donts = remember(effectiveDay.dontsJson) {
                YawmiyatiRepository.parseItems(effectiveDay.dontsJson)
            }
            ListSectionCard(
                title = "ما أعملش النهاردة",
                subtitle = "عادات أو مشتتات باقرر أتجنبها",
                items = donts,
                canCheck = false,
                onToggle = { _, _ -> },
                onDelete = { idx -> viewModel.removeDont(idx) },
                onAdd = { text -> viewModel.addDont(text) }
            )
        }

        // Card 5: الصبح
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الصبح ☀️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DebouncedTextField(
                        label = "صلاتي للنهار ده",
                        value = effectiveDay.prayer,
                        placeholder = "اكتب صلاتك للرب في بداية يومك…",
                        onSave = { viewModel.updateDayField(prayer = it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DebouncedTextField(
                        label = "وعدي للرب",
                        value = effectiveDay.toGod,
                        placeholder = "وعد أو التزام لطيف بتقدّمه النهاردة…",
                        onSave = { viewModel.updateDayField(toGod = it) }
                    )
                }
            }
        }

        // Card 6: الليل (Golden Highlight)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الليل 🌙",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DebouncedTextField(
                        label = "وعد الرب لي",
                        value = effectiveDay.fromGod,
                        placeholder = "آية أو كلمة شعرت إنها وعد ليك…",
                        onSave = { viewModel.updateDayField(fromGod = it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kept Promise Selector
                    Text(
                        text = "وفيت بوعدي للرب؟",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeptButton(
                            label = "وفيت",
                            selected = effectiveDay.kept == "y",
                            activeColor = SacredKeptYes,
                            onClick = {
                                viewModel.updateDayField(kept = if (effectiveDay.kept == "y") "" else "y")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        KeptButton(
                            label = "جزئيًا",
                            selected = effectiveDay.kept == "p",
                            activeColor = SacredKeptPartial,
                            onClick = {
                                viewModel.updateDayField(kept = if (effectiveDay.kept == "p") "" else "p")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        KeptButton(
                            label = "لم أفِ",
                            selected = effectiveDay.kept == "n",
                            activeColor = SacredKeptNo,
                            onClick = {
                                viewModel.updateDayField(kept = if (effectiveDay.kept == "n") "" else "n")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DebouncedTextField(
                        label = "٣ حاجات أشكر عليها",
                        value = effectiveDay.thanks,
                        placeholder = "١. \n٢. \n٣. ",
                        minLines = 3,
                        onSave = { viewModel.updateDayField(thanks = it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DebouncedTextField(
                        label = "ملاحظة حرة",
                        value = effectiveDay.note,
                        placeholder = "أي شعور، تأمل، أو حدث شفته النهاردة…",
                        onSave = { viewModel.updateDayField(note = it) }
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelectorBar(
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember(currentDate) {
        Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(currentDate)
                if (parsed != null) time = parsed
            } catch (_: Exception) {}
        }
    }

    val displayDate = remember(currentDate) {
        val today = YawmiyatiRepository.todayDate()
        if (currentDate == today) {
            "اليوم (${currentDate})"
        } else {
            currentDate
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                    onDateSelected(newDate)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "اليوم السابق"
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val dpd = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val cal = Calendar.getInstance().apply {
                                    set(y, m, d)
                                }
                                val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                                onDateSelected(formatted)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "التقويم",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = {
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                    onDateSelected(newDate)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "اليوم التالي"
                )
            }
        }
    }
}

@Composable
fun VerseCard(
    ref: String,
    verse: String,
    onOpenBiblePicker: () -> Unit,
    onRefChange: (String) -> Unit,
    onVerseChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, SacredGoldBadge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آية اليوم 📖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FilledTonalButton(
                    onClick = onOpenBiblePicker,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SacredGoldBadge.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("من الكتاب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DebouncedTextField(
                label = "المرجع",
                value = ref,
                placeholder = "مثال: مزمور ٢٣: ١",
                singleLine = true,
                onSave = onRefChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebouncedTextField(
                label = "نص الآية",
                value = verse,
                placeholder = "«الرَّبُّ رَاعِيَّ فَلَا يُعْوِزُنِي شَيْءٌ»",
                minLines = 2,
                onSave = onVerseChange
            )
        }
    }
}

@Composable
fun FearPromptCard(
    fear: EntryEntity,
    onTakeAsPromise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "لو الخوف ده جالك: «${fear.text}»",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${fear.ref}: ${fear.ptext}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onTakeAsPromise,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("خدها كوعد النهاردة ✨", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ListSectionCard(
    title: String,
    subtitle: String,
    items: List<com.example.data.model.JournalItem>,
    canCheck: Boolean,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: (String) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Item list
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canCheck) {
                        Checkbox(
                            checked = item.d,
                            onCheckedChange = { isChecked -> onToggle(index, isChecked) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = item.t,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.d && canCheck) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.d && canCheck) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onDelete(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("اكتب وأضف…", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newItemText.isNotBlank()) {
                            onAdd(newItemText.trim())
                            newItemText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun KeptButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) activeColor.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (selected) activeColor else MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable
fun DebouncedTextField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    onSave: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onSave(it)
            },
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(10.dp)
        )
    }
}
