package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EntryEntity
import com.example.ui.components.BiblePickerDialog
import com.example.ui.theme.SacredCardBorder
import com.example.ui.theme.SacredGoldBadge
import com.example.ui.viewmodel.YawmiyatiViewModel

@Composable
fun LibraryScreen(
    viewModel: YawmiyatiViewModel
) {
    val dreams by viewModel.dreams.collectAsState()
    val fears by viewModel.fears.collectAsState()
    val wishes by viewModel.wishes.collectAsState()
    val taboos by viewModel.taboos.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val obstacles by viewModel.obstacles.collectAsState()

    var activeFearForVerse by remember { mutableStateOf<EntryEntity?>(null) }

    if (activeFearForVerse != null) {
        BiblePickerDialog(
            onDismiss = { activeFearForVerse = null },
            onPickVerse = { ref, text ->
                activeFearForVerse?.let { fear ->
                    viewModel.linkFearVerse(fear.id, ref, text)
                }
                activeFearForVerse = null
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
        item {
            Text(
                text = "المكتبة 📚",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "سجل لأحلامك ومخاوفك وأهدافك وربطها بوعود الرب",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. أحلامي
        item {
            LibraryKindCard(
                title = "أحلامي 🌟",
                items = dreams,
                onAdd = { viewModel.addEntry("dreams", it) },
                onDelete = { viewModel.deleteEntry(it) }
            )
        }

        // 2. مخاوفي مع ربط الوعود
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مخاوفي 🛡️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "اربط كل خوف بوعد من كلمة الله",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    fears.forEach { fear ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fear.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { activeFearForVerse = fear }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SacredGoldBadge
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (fear.ref.isNotBlank()) "تغيير الوعد" else "اربط بوعد",
                                        color = SacredGoldBadge,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteEntry(fear.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                                }
                            }

                            if (fear.ref.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = fear.ref,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = fear.ptext,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    AddEntryRow(
                        placeholder = "أضف خوفًا للتعامل معه…",
                        onAdd = { viewModel.addEntry("fears", it) }
                    )
                }
            }
        }

        // 3. أمنياتي
        item {
            LibraryKindCard(
                title = "أمنياتي 🕊️",
                items = wishes,
                onAdd = { viewModel.addEntry("wishes", it) },
                onDelete = { viewModel.deleteEntry(it) }
            )
        }

        // 4. محذوراتي
        item {
            LibraryKindCard(
                title = "محذوراتي ⛔",
                items = taboos,
                onAdd = { viewModel.addEntry("taboos", it) },
                onDelete = { viewModel.deleteEntry(it) }
            )
        }

        // 5. أهدافي ومعوقاتها
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SacredGoldBadge.copy(alpha = 0.6f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "أهدافي ومعوقاتها 🎯",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "كل هدف مع معوقاته وأصغر خطوة جاية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    goals.forEach { goal ->
                        val goalObstacles = remember(obstacles, goal.id) {
                            obstacles.filter { it.parentId == goal.id }
                        }
                        GoalItemView(
                            goal = goal,
                            obstacles = goalObstacles,
                            onDeleteGoal = { viewModel.deleteGoal(goal.id) },
                            onAddObstacle = { text ->
                                viewModel.addEntry("obstacle", text, parentId = goal.id)
                            },
                            onDeleteObstacle = { obsId -> viewModel.deleteEntry(obsId) },
                            onUpdateDetails = { step, verse ->
                                viewModel.updateGoalStepAndVerse(goal.id, step, verse)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AddEntryRow(
                        placeholder = "هدف جديد…",
                        onAdd = { viewModel.addEntry("goal", it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryKindCard(
    title: String,
    items: List<EntryEntity>,
    onAdd: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDelete(item.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            AddEntryRow(placeholder = "أضف عنصراً جديداً…", onAdd = onAdd)
        }
    }
}

@Composable
fun GoalItemView(
    goal: EntryEntity,
    obstacles: List<EntryEntity>,
    onDeleteGoal: () -> Unit,
    onAddObstacle: (String) -> Unit,
    onDeleteObstacle: (Long) -> Unit,
    onUpdateDetails: (step: String, verse: String) -> Unit
) {
    var stepText by remember(goal.step) { mutableStateOf(goal.step) }
    var verseText by remember(goal.verse) { mutableStateOf(goal.verse) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteGoal, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "حذف الهدف", tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Obstacles
            obstacles.forEach { obs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• معوق: ${obs.text}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteObstacle(obs.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "حذف المعوق", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                    }
                }
            }

            AddEntryRow(
                placeholder = "أضف معوقًا…",
                onAdd = onAddObstacle
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = stepText,
                onValueChange = {
                    stepText = it
                    onUpdateDetails(it, verseText)
                },
                label = { Text("أصغر خطوة جاية", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = verseText,
                onValueChange = {
                    verseText = it
                    onUpdateDetails(stepText, it)
                },
                label = { Text("آية / وعد متعلق", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun AddEntryRow(
    placeholder: String,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onAdd(text.trim())
                    text = ""
                }
            },
            modifier = Modifier
                .size(44.dp)
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
