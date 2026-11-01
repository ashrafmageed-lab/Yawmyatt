package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.YawmiyatiRepository
import com.example.ui.theme.SacredGoldBadge
import com.example.ui.theme.SacredKeptYes
import com.example.ui.viewmodel.YawmiyatiViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReviewScreen(
    viewModel: YawmiyatiViewModel
) {
    var periodDays by remember { mutableIntStateOf(7) } // 7 or 30 days

    val allDays by viewModel.allDays.collectAsState()
    val prayers by viewModel.prayers.collectAsState()

    val cutoffDate = remember(periodDays) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -periodDays)
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    val todayDate = remember { YawmiyatiRepository.todayDate() }

    val filteredDays = remember(allDays, cutoffDate) {
        allDays.filter { it.date > cutoffDate && it.date <= todayDate }
            .sortedByDescending { it.date }
    }

    val daysWithActivity = remember(filteredDays) {
        filteredDays.filter {
            it.toGod.isNotBlank() || it.fromGod.isNotBlank() || it.prayer.isNotBlank() ||
            it.todosJson != "[]" || it.thanks.isNotBlank()
        }.size
    }

    // Todos stats
    val allTodos = remember(filteredDays) {
        filteredDays.flatMap { YawmiyatiRepository.parseItems(it.todosJson) }
    }
    val completedTodosCount = remember(allTodos) { allTodos.count { it.d } }

    // Donts frequency analysis
    val topDonts = remember(filteredDays) {
        val freqMap = mutableMapOf<String, Int>()
        filteredDays.flatMap { YawmiyatiRepository.parseItems(it.dontsJson) }
            .forEach { item ->
                val key = item.t.trim()
                if (key.isNotBlank()) {
                    freqMap[key] = (freqMap[key] ?: 0) + 1
                }
            }
        freqMap.entries.sortedByDescending { it.value }.take(3)
    }

    // Gratitude highlights
    val thanksHighlights = remember(filteredDays) {
        filteredDays.filter { it.thanks.isNotBlank() }.take(3)
    }

    // Answered prayers in period
    val recentAnsweredPrayers = remember(prayers, cutoffDate) {
        prayers.filter { it.ansDate != null && it.ansDate!! > cutoffDate }
    }

    val keptYes = filteredDays.count { it.kept == "y" }
    val keptPartial = filteredDays.count { it.kept == "p" }
    val keptNo = filteredDays.count { it.kept == "n" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "مراجعة دورية 🔍",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "نظرة هادئة على مسارك وعاداتك بدون لوم أو ذنب",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Period Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = periodDays == 7,
                    onClick = { periodDays = 7 },
                    label = { Text("آخر ٧ أيام") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = periodDays == 30,
                    onClick = { periodDays = 30 },
                    label = { Text("آخر ٣٠ يوم") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Card 1: ملخص الفترة
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ملخص الفترة 📊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• أيام فيها تسجيل: $daysWithActivity من أصل $periodDays يوم",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• وعودي للرب: وفيت $keptYes — جزئيًا $keptPartial — لم أفِ $keptNo",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (allTodos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• اللي عملته من قايمة «أعمل»: $completedTodosCount من إجمالي ${allTodos.size}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Card 2: «ما أعملش» الأكتر تكرارًا
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "«ما أعملش» الأكثر تكرارًا 🛑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "أنماط المشتتات عشان تاخد بالك منها بلطف",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topDonts.isNotEmpty()) {
                        topDonts.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${entry.value} يوم",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "لا توجد عادات مسجلة في قائمة «ما أعملش» خلال الفترة دي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card 3: من شكري
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "من شكري في الفترة دي 💛",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (thanksHighlights.isNotEmpty()) {
                        thanksHighlights.forEach { day ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = day.date,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = day.thanks,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    } else {
                        Text(
                            text = "مفيش نقاط شكر مسجلة في الفترة دي. تذكّر نعم ربنا البسيطة وسجّلها بالليل.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card 4: صلوات استُجيبت
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "صلوات استُجيبت في الفترة دي 🙌",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SacredKeptYes
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentAnsweredPrayers.isNotEmpty()) {
                        recentAnsweredPrayers.forEach { prayer ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "«${prayer.text}»",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "استُجيبت: ${prayer.ansDate}${if (!prayer.ansNote.isNullOrBlank()) " — " + prayer.ansNote else ""}",
                                    fontSize = 12.sp,
                                    color = SacredKeptYes
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "لا توجد صلوات تم وسمها كمستجابة في الفترة دي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
