package com.example.mikrotik.presentation.diagnostics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.data.backup.BackupJsonCodec
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.localization.AppLanguage
import com.example.mikrotik.presentation.ui.localization.LocalAppLanguage
import com.example.mikrotik.ui.theme.AppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupSettingsScreen(viewModel: DiagnosticsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val createBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            status = null
            try {
                val json = viewModel.exportBackupJson()
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Cannot open selected file for writing")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                }
                status = if (isEnglish) "Backup file created successfully." else "اكتمل إنشاء ملف النسخة الاحتياطية."
            } catch (error: Exception) {
                status = if (isEnglish) "Could not create backup: ${error.message ?: "Unknown error"}" else "تعذر إنشاء النسخة: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                busy = false
            }
        }
    }

    val importBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            status = null
            try {
                val json = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Cannot open selected file")
                    input.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= BackupJsonCodec.MAX_BYTES) { "Backup file exceeds the 5 MiB limit" }
                            output.write(buffer, 0, count)
                        }
                        output.toString(Charsets.UTF_8.name())
                    }
                }
                val result = viewModel.restoreBackupJson(json)
                status = if (isEnglish) {
                    "Merged successfully: ${result.insertedVouchers} vouchers added; ${result.skippedExistingVouchers} existing vouchers were left unchanged. Restart the app for imported appearance preferences to take effect."
                } else {
                    "تمت الاستعادة بالدمج: أضيفت ${result.insertedVouchers} بطاقة، وتُركت ${result.skippedExistingVouchers} بطاقة موجودة كما هي. أعد تشغيل التطبيق لتطبيق تفضيلات المظهر المستوردة."
                }
            } catch (error: Exception) {
                status = if (isEnglish) "Backup was not restored: ${error.message ?: "Invalid file"}" else "لم تُستعد النسخة: ${error.message ?: "الملف غير صالح"}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text(appText("رجوع إلى الإعدادات", "Back to settings")) }
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AppPalette.Border, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    appText("نسخ البيانات يدوياً", "Manual data backup"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.TextNavy
                )
                Text(
                    appText(
                        "تُحفظ القسائم وإعدادات اللغة والمظهر والبوابة وسعات الباقات وأسماء الأجهزة وسجلات الاستهلاك المتاحة في ملف JSON. الاستعادة تدمج البيانات دون حذفها؛ وتعيد إعدادات المظهر والبوابة والسعات الموجودة في الملف، وتضيف القسائم غير الموجودة فقط دون استبدال بطاقة قائمة. لا تتضمن النسخة جلسات اتصال نشطة أو PIN قفل التطبيق أو أسرار root/الاتصال.",
                        "The JSON file includes vouchers, language and appearance preferences, portal settings, package capacities, device names, and available usage history. Restore merges without deleting data: it restores the backed-up appearance, portal and capacity settings, and adds only missing vouchers without replacing existing ones. Active sessions, the app-lock PIN, and root/connection secrets are not included."
                    ),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AppPalette.Muted
                )
                Text(
                    appText(
                        "تنبيه أمان: يحتوي الملف على رموز بطاقات vouchers، ويمكن لمن يحصل عليها استخدام البطاقات. احفظ الملف في مكان موثوق وآمن ولا تشاركه علناً.",
                        "Security: the backup contains voucher codes that can be used to redeem vouchers. Store it securely in a trusted location and do not share it publicly."
                    ),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB3261E)
                )
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = { createBackup.launch("mikrotik-backup-v1.json") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Teal)
                ) { Text(appText("إنشاء نسخة احتياطية…", "Create backup…")) }
                OutlinedButton(
                    onClick = { importBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(appText("استعادة / دمج نسخة…", "Restore / merge backup…")) }
                if (busy) Text(appText("جارٍ معالجة الملف…", "Processing file…"), color = AppPalette.Muted)
                status?.let {
                    Text(it, fontSize = 13.sp, lineHeight = 19.sp, color = AppPalette.TextNavy)
                }
            }
        }
    }
}
