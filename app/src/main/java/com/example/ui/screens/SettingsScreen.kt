package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.repository.YawmiyatiRepository
import com.example.reminder.ReminderSettings
import com.example.security.SecurityManager
import com.example.ui.theme.SacredGoldBadge
import com.example.ui.viewmodel.YawmiyatiViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: YawmiyatiViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val reminders by viewModel.reminders.collectAsState()

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var importPasswordText by remember { mutableStateOf("") }
    var exportPasswordText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!content.isNullOrBlank()) {
                    if (content.contains("\"enc\":true") || content.contains("\"enc\": true")) {
                        pendingImportContent = content
                        showImportPasswordDialog = true
                    } else {
                        coroutineScope.launch {
                            try {
                                viewModel.repository.importData(content)
                                Toast.makeText(context, "تم استيراد البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "ملف غير صالح للاستيراد", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف المختار", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Share/Save Exported file
    fun shareExportFile(data: String, isEncrypted: Boolean) {
        try {
            val fileName = "yawmiyati-${YawmiyatiRepository.todayDate()}${if (isEncrypted) ".enc" else ""}.json"
            val file = File(context.cacheDir, fileName).apply {
                writeText(data)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "حفظ أو مشاركة نسخة يوميتي"))
        } catch (e: Exception) {
            // Fallback: copy to clipboard or direct intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, data)
            }
            context.startActivity(Intent.createChooser(shareIntent, "نسخ بيانات يوميتي"))
        }
    }


    // Encrypted Export Password Dialog
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("تصدير مشفر (AES-GCM)") },
            text = {
                Column {
                    Text(
                        text = "اكتب كلمة سر قوية للنسخة. احفظها كويس لأن مفيش طريقة لاسترجاعها لو نسيتها:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportPasswordText,
                        onValueChange = { exportPasswordText = it },
                        placeholder = { Text("كلمة سر النسخة…") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPasswordText.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    val encrypted = viewModel.repository.exportData(exportPasswordText)
                                    shareExportFile(encrypted, isEncrypted = true)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر التشفير والتصدير", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showExportPasswordDialog = false
                            exportPasswordText = ""
                        }
                    }
                ) {
                    Text("تصدير مشفر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Import Encrypted Password Dialog
    if (showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showImportPasswordDialog = false },
            title = { Text("فك تشفير النسخة") },
            text = {
                Column {
                    Text(
                        text = "الملف ده مشفر بكلمة سر. اكتب كلمة السر لاستيراد بياناتك:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importPasswordText,
                        onValueChange = { importPasswordText = it },
                        placeholder = { Text("كلمة السر…") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = pendingImportContent
                        if (content != null && importPasswordText.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    viewModel.repository.importData(content, importPasswordText)
                                    Toast.makeText(context, "تم فك التشفير واستيراد البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                                    showImportPasswordDialog = false
                                    pendingImportContent = null
                                    importPasswordText = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "كلمة السر غير صحيحة أو الملف تالف", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("استيراد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPasswordDialog = false }) {
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
        item {
            Text(
                text = "الإعدادات ⚙️",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "الخصوصية التامة، التذكيرات، والنسخ الاحتياطي",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. الخصوصية والأمان المحلي
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("privacy_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الخصوصية والبيانات المحلية 🛡️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تطبيق مفتوح وفوري بدون قفل شاشة أو تعقيد",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "جميع صلواتك، يومياتك، وتأملاتك تُحفظ محلياً بنسبة ١٠٠٪ داخل ذاكرة هاتفك وقاعدة بيانات Room دون إرسال أي معلومة إلى أي خادم خارجي. التطبيق يفتح فوراً بدون أي شاشات قفل لتكتب ما بقلبك بكل بساطة وحرية.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1.5. تخصيص حجم الخط المريح
        item {
            val fontScale by viewModel.fontScale.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("font_scale_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SacredGoldBadge.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "حجم خط القراءة والكتابة 🔤",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "اختر الحجم الأنسب لراحة عينيك أثناء التأمل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scales = listOf(
                            Triple("طبيعي (١٠٠٪)", 1.0f, "normal"),
                            Triple("مريح (١١٥٪)", 1.15f, "comfortable"),
                            Triple("كبير (١٣٠٪)", 1.30f, "large")
                        )

                        scales.forEach { (label, scaleVal, tag) ->
                            val isSelected = Math.abs(fontScale - scaleVal) < 0.05f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setFontScale(scaleVal)
                                    Toast.makeText(context, "تم ضبط حجم الخط: $label", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f).testTag("font_scale_$tag"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGoldBadge,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, SacredGoldBadge.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "معاينة حجم الخط المختار:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "«الرَّبُّ نُورِي وَخَلاَصِي، مِمَّنْ أَخَافُ؟ الرَّبُّ حِصْنُ حَيَاتِي، مِمَّنْ أَرْتَعِبُ؟» (المزامير ٢٧:١)",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. التذكيرات اليومية
        item {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.updateReminders(reminders.copy(enabled = true))
                    Toast.makeText(context, "تم تفعيل التذكيرات اليومية بنجاح ✨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "يرجى السماح بالإشعارات من إعدادات الهاتف لتصلك التذكيرات", Toast.LENGTH_LONG).show()
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("reminders_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تذكير المساء لكتابة اليومية 🌙",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = reminders.enabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.updateReminders(reminders.copy(enabled = true))
                                        Toast.makeText(context, "تم تفعيل تذكير المساء اليومي بنجاح ✨", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.updateReminders(reminders.copy(enabled = false))
                                    Toast.makeText(context, "تم إيقاف تذكير المساء", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("evening_reminder_switch").testTag("reminders_toggle_switch")
                        )
                    }

                    Text(
                        text = "نظام إشعارات محلي يرسل تذكيراً هادئاً كل مساء في وقت محدد لتسجيل يوميتك وتأملك وشكرك في محضر الرب.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (reminders.enabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Evening Presets
                        Text(
                            text = "اختر موعد التذكير المسائي الأنسب لك:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val eveningPresets = listOf(
                            Triple("٨:٠٠ م", 20, 0),
                            Triple("٩:٠٠ م ✨", 21, 0),
                            Triple("٩:٣٠ م", 21, 30),
                            Triple("١٠:٠٠ م", 22, 0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            eveningPresets.forEach { (label, h, m) ->
                                val isSelected = reminders.dailyHour == h && reminders.dailyMinute == m
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateReminders(
                                            reminders.copy(dailyHour = h, dailyMinute = m)
                                        )
                                        Toast.makeText(context, "تم ضبط موعد تذكير المساء: $label", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f).testTag("evening_preset_${h}_${m}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGoldBadge,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Time Picker Button
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        viewModel.updateReminders(
                                            reminders.copy(dailyHour = h, dailyMinute = m)
                                        )
                                        Toast.makeText(context, "تم ضبط موعد تذكير المساء: %02d:%02d".format(h, m), Toast.LENGTH_SHORT).show()
                                    },
                                    reminders.dailyHour,
                                    reminders.dailyMinute,
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("pick_daily_reminder_time_button")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تعديل الموعد المسائي يدوياً (%02d:%02d مساءً)".format(reminders.dailyHour, reminders.dailyMinute),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Reminder Mode Selection
                        Text(
                            text = "نظام التذكير:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = reminders.mode == "daily",
                                onClick = {
                                    viewModel.updateReminders(reminders.copy(mode = "daily"))
                                },
                                label = { Text("تذكير المساء لكتابة اليومية", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = reminders.mode == "both",
                                onClick = {
                                    viewModel.updateReminders(reminders.copy(mode = "both"))
                                },
                                label = { Text("وقفتان (الصبح + المساء)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (reminders.mode == "both") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                viewModel.updateReminders(
                                                    reminders.copy(morningHour = h, morningMinute = m)
                                                )
                                            },
                                            reminders.morningHour,
                                            reminders.morningMinute,
                                            false
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "الصبح: %02d:%02d".format(reminders.morningHour, reminders.morningMinute),
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                viewModel.updateReminders(
                                                    reminders.copy(dailyHour = h, dailyMinute = m)
                                                )
                                            },
                                            reminders.dailyHour,
                                            reminders.dailyMinute,
                                            false
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "المساء: %02d:%02d".format(reminders.dailyHour, reminders.dailyMinute),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Notification Button
                        FilledTonalButton(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                com.example.reminder.ReminderManager.sendEveningTestReminder(context)
                                Toast.makeText(context, "تم إرسال تذكير المساء التجريبي! تفقّد الإشعارات 🔔", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_test_notification_button")
                                .testTag("send_test_evening_notification_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تجربة إشعار تذكير المساء الآن 🔔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. نسخة احتياطية
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نسخة احتياطية واسترجاع 💾",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "كل بياناتك بتفضل على موبايلك فقط، تقدر تصدّرها أو تستوردها في أي وقت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val data = viewModel.repository.exportData()
                                        shareExportFile(data, isEncrypted = false)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر التصدير", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نسخة عادية (JSON)")
                        }

                        OutlinedButton(
                            onClick = { showExportPasswordDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نسخة مشفرة بكلمة سر")
                        }

                        FilledTonalButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("استيراد نسخة احتياطية من ملف")
                        }
                    }
                }
            }
        }

        // 4. عن يوميتي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "عن تطبيق يوميتي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تطبيق يوميات وممارسات روحية خاصة بنظام أندرويد الأصيل (Offline-First).\n• بدون حسابات، خوادم، أو تتبع.\n• الكتاب المقدس الكامل ترجمة سميث وفاندايك بدون إنترنت.\n• تصميم دافئ ومريح مصمم لمرافقة خلوتك اليومية.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
