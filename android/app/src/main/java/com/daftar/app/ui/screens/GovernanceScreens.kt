package com.daftar.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.AppThemeMode
import com.daftar.app.core.security.UserRole
import com.daftar.app.domain.audit.AuditSeverity
import com.daftar.app.ui.components.*
import com.daftar.app.ui.theme.*
import com.daftar.app.ui.viewmodel.AppUiState
import com.daftar.app.ui.viewmodel.AppViewModel
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel

@Composable
fun ReportsAndAuditScreen(lang: AppLanguage, snapshot: WorkspaceSnapshot) {
    val report = snapshot.auditReport
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "التقارير ومركز المراجعة التشخيصية" else "Reports & 10-Point Diagnostic Audit",
            subtitle = if (lang == AppLanguage.ARABIC) "فحص قراءة فقط (لا يعدل أي رصيد)" else "Strictly read-only ledger integrity audit",
            badge = "${report?.healthScorePercent ?: 100}%",
        )
        if (report == null) {
            EmptyStatePanel(if (lang == AppLanguage.ARABIC) "جاري الفحص..." else "Running diagnostic", "")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(report.findings) { f ->
                    val color = when (f.severity) {
                        AuditSeverity.PASS -> EmeraldProfit
                        AuditSeverity.WARNING -> AmberWarning
                        AuditSeverity.CRITICAL -> CrimsonError
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${f.code} • ${f.title.get(lang)}", fontWeight = FontWeight.Bold)
                                Text(f.detail.get(lang), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${if (lang == AppLanguage.ARABIC) "التأثير:" else "Impact:"} ${f.impact.get(lang)}", style = MaterialTheme.typography.labelMedium)
                                Text("${if (lang == AppLanguage.ARABIC) "الحل المقترح:" else "Fix:"} ${f.recommendation.get(lang)}", style = MaterialTheme.typography.labelMedium, color = EmeraldProfit)
                            }
                            Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                                Text(f.metricValue, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAssistantScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var prompt by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "المساعد المحاسبي الذكي المحلي" else "Offline Smart Accounting Assistant",
            subtitle = if (lang == AppLanguage.ARABIC) "يعمل محلياً ١٠٠٪ للقراءة فقط بدون إنترنت" else "100% offline read-only financial analyst",
            badge = "READ-ONLY",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SuggestionChip(onClick = { vm.askLocalAssistant("ما هو تحليل الربح والدخل؟", lang) }, label = { Text(if (lang == AppLanguage.ARABIC) "الربحية" else "Profit") })
            SuggestionChip(onClick = { vm.askLocalAssistant("ما وضع السيولة والنقدية؟", lang) }, label = { Text(if (lang == AppLanguage.ARABIC) "السيولة" else "Liquidity") })
            SuggestionChip(onClick = { vm.askLocalAssistant("هل توجد أخطاء في المراجعة؟", lang) }, label = { Text(if (lang == AppLanguage.ARABIC) "المراجعة" else "Audit") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(prompt, { prompt = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسأل المساعد المحاسبي..." else "Ask local analyst...") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (prompt.isNotBlank()) { vm.askLocalAssistant(prompt, lang); prompt = "" } }) {
                Text(if (lang == AppLanguage.ARABIC) "تحليل" else "Analyze")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snapshot.assistantHistory) { reply ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reply.title, style = MaterialTheme.typography.titleMedium, color = NavyClassic)
                        Text(reply.summary, style = MaterialTheme.typography.bodyMedium)
                        reply.bulletPoints.forEach { bp ->
                            Text("• $bp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    snapshot: WorkspaceSnapshot,
    onOpenRestore: () -> Unit,
) {
    val lang = uiState.language
    var backupPassword by remember { mutableStateOf("") }
    var backupAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val activeId = snapshot.profile?.id
            val ids = if (backupAll || activeId == null) null else setOf(activeId)
            appViewModel.exportBackupToUri(uri, ids, backupPassword)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "النسخ الاحتياطي المشفر AES-256" else "AES-256 Encrypted Backup",
            subtitle = if (lang == AppLanguage.ARABIC) "تصدير نشاط منفرد أو جميع الأنشطة عبر SAF" else "Single vault or full multi-vault SAF archive",
            badge = "SHA-256",
        )
        uiState.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { appViewModel.clearMessages() }) }
        uiState.successMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { appViewModel.clearMessages() }) }

        OutlinedTextField(
            value = backupPassword,
            onValueChange = { backupPassword = it },
            label = { Text(if (lang == AppLanguage.ARABIC) "مفتاح الاسترداد أو كلمة مرور التشفير (٦+ أحرف)" else "Recovery Key / Encryption Password (6+ chars)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = backupAll, onCheckedChange = { backupAll = it })
            Text(if (lang == AppLanguage.ARABIC) "نسخ جميع الأنشطة المستقلة (${uiState.vaults.size})" else "Include all activity vaults (${uiState.vaults.size})")
        }
        Button(
            onClick = { exportLauncher.launch("daftar-backup-${System.currentTimeMillis()}.dftrbak") },
            enabled = backupPassword.length >= 6 && !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Backup, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (lang == AppLanguage.ARABIC) "حفظ نسخة مشفرة محلياً" else "Export Encrypted Backup")
        }
        OutlinedButton(onClick = onOpenRestore, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (lang == AppLanguage.ARABIC) "استعادة وفحص نسخة احتياطية" else "Verify & Restore Backup")
        }
    }
}

@Composable
fun SecurityScreen(lang: AppLanguage, appViewModel: AppViewModel, snapshot: WorkspaceSnapshot) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "الحماية وسجل التدقيق" else "Security & Immutable Audit Log",
            subtitle = if (lang == AppLanguage.ARABIC) "Android Keystore + SQLCipher + سجل غير قابل للتعديل" else "Hardware Keystore + SQLCipher AES-256",
            badge = if (appViewModel.container.isHardwareKeystoreActive) "KEYSTORE" else "ENCRYPTED",
        )
        OfflineEncryptionBadge(lang, appViewModel.container.isHardwareKeystoreActive)
        Text("${if (lang == AppLanguage.ARABIC) "أحداث التدقيق المسجلة:" else "Audit Events:"} ${snapshot.auditLogs.size}", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.auditLogs) { log ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${log.actionType} • ${log.userName}", fontWeight = FontWeight.Bold, color = if (log.isSecurityEvent) GoldPrimary else MaterialTheme.colorScheme.onSurface)
                        Text(log.summary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndUsersScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    vm: WorkspaceViewModel,
    snapshot: WorkspaceSnapshot,
    onSwitchVault: () -> Unit,
) {
    val lang = uiState.language
    var newUserName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserPin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ACCOUNTANT) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "الإعدادات والمستخدمون والصلاحيات" else "Settings, Users & RBAC",
            subtitle = "${snapshot.profile?.nameAr ?: ""} • ${snapshot.currency}",
        )
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { appViewModel.toggleLanguage() }, modifier = Modifier.weight(1f)) {
                Text(if (lang == AppLanguage.ARABIC) "Switch to English (LTR)" else "التبديل للعربية (RTL)")
            }
            OutlinedButton(onClick = { appViewModel.toggleTheme() }, modifier = Modifier.weight(1f)) {
                Text(if (uiState.themeMode == AppThemeMode.LIGHT) (if (lang == AppLanguage.ARABIC) "الوضع الليلي" else "Dark Mode") else (if (lang == AppLanguage.ARABIC) "الوضع النهاري" else "Light Mode"))
            }
        }

        Text(if (lang == AppLanguage.ARABIC) "إدارة الأنشطة المستقلة (أرشفة / تبديل):" else "Manage Isolated Vaults:", style = MaterialTheme.typography.titleMedium)
        uiState.vaults.forEach { v ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${v.nameAr} (${v.baseCurrency})", fontWeight = FontWeight.Bold)
                        Text(if (v.isArchived) "مؤرشف" else "نشط", style = MaterialTheme.typography.labelMedium)
                    }
                    Row {
                        TextButton(onClick = { appViewModel.toggleArchiveVault(v) }) {
                            Text(if (v.isArchived) (if (lang == AppLanguage.ARABIC) "استرجاع" else "Unarchive") else (if (lang == AppLanguage.ARABIC) "أرشفة" else "Archive"))
                        }
                        TextButton(onClick = onSwitchVault) { Text(if (lang == AppLanguage.ARABIC) "دخول" else "Open") }
                    }
                }
            }
        }

        HorizontalDivider()
        Text(if (lang == AppLanguage.ARABIC) "إضافة مستخدم للنشاط الحالي (RBAC):" else "Add Vault User (RBAC):", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(newUserName, { newUserName = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الاسم الكامل" else "Full Name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newUsername, { newUsername = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم المستخدم" else "Username") }, modifier = Modifier.weight(1f))
            OutlinedTextField(newUserPin, { newUserPin = it.filter { c -> c.isDigit() } }, label = { Text("PIN (4-12)") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UserRole.entries.take(4).forEach { r ->
                FilterChip(selected = selectedRole == r, onClick = { selectedRole = r }, label = { Text(r.label.get(lang)) })
            }
        }
        Button(onClick = {
            if (newUserName.isNotBlank() && newUserPin.length >= 4) {
                vm.addUser(newUserName, newUsername.ifBlank { newUserName }, selectedRole, newUserPin)
                newUserName = ""; newUsername = ""; newUserPin = ""
            }
        }) { Text(if (lang == AppLanguage.ARABIC) "حفظ المستخدم بصلاحياته" else "Save User") }

        snapshot.users.forEach { u ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(u.fullName, fontWeight = FontWeight.Bold)
                    Text(UserRole.fromId(u.roleId).label.get(lang))
                }
            }
        }
    }
}
