package com.daftar.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.model.InventoryCostingMethod
import com.daftar.app.core.security.BiometricHelper
import com.daftar.app.data.registry.ActivityVaultEntity
import com.daftar.app.ui.components.*
import com.daftar.app.ui.theme.*
import com.daftar.app.ui.viewmodel.AppUiState
import com.daftar.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    uiState: AppUiState,
    onNavigateToLogin: () -> Unit,
    onNavigateToBusinessType: () -> Unit,
) {
    LaunchedEffect(uiState.vaults.size) {
        delay(800)
        if (uiState.vaults.any { !it.isArchived }) {
            onNavigateToLogin()
        } else {
            onNavigateToBusinessType()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NavyClassic,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DaftarGeometricLogo(size = 112.dp, variant = DaftarLogoVariant.NIGHT)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "دفتر | DAFTAR",
                style = MaterialTheme.typography.displaySmall,
                color = GoldLight,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.language == AppLanguage.ARABIC) "المحاسب الذكي المستقل" else "Smart Offline Accounting Vault",
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White,
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun BusinessTypeScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    onContinue: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val lang = uiState.language
    Scaffold(
        topBar = {
            DaftarTopBar(
                title = if (lang == AppLanguage.ARABIC) "اختيار نوع النشاط" else "Select Business Sector",
                subtitle = if (lang == AppLanguage.ARABIC) "تُفعّل الوحدات المحاسبية والمتخصصة تلقائياً" else "Sector-specific modules enable automatically",
                actions = {
                    TextButton(onClick = { appViewModel.toggleLanguage() }) {
                        Text(if (lang == AppLanguage.ARABIC) "EN" else "عربي")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SectionHeaderCard(
                title = if (lang == AppLanguage.ARABIC) "أنشئ حافظة نشاط مستقلة" else "Create an Isolated Activity Vault",
                subtitle = if (lang == AppLanguage.ARABIC) "لكل نشاط قاعدة SQLCipher ومفتاح تشفير وعملة خاصة" else "Each business gets its own SQLCipher DB, Keystore key & currency",
                badge = if (lang == AppLanguage.ARABIC) "يعمل دون إنترنت" else "100% Offline",
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(BusinessType.entries) { type ->
                    val selected = uiState.selectedBusinessType == type
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appViewModel.selectBusinessType(type) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) GoldPrimary else MaterialTheme.colorScheme.outline,
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) GoldSoft else MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = type.title.get(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) NavyClassic else MaterialTheme.colorScheme.onSurface,
                                )
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = type.summary.get(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) NavyDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (lang == AppLanguage.ARABIC) "متابعة إنشاء النشاط" else "Continue Activity Setup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRestoreBackup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (lang == AppLanguage.ARABIC) "استعادة من نسخة احتياطية مشفرة" else "Restore Encrypted Backup")
            }
        }
    }
}

@Composable
fun CreateActivityScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    val lang = uiState.language
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("SD") }
    var baseCurrency by remember { mutableStateOf("SDG") }
    var decimals by remember { mutableStateOf("2") }
    var fiscalYearStart by remember { mutableStateOf("2026-01-01") }
    var costing by remember { mutableStateOf(InventoryCostingMethod.WEIGHTED_AVERAGE) }
    var adminName by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            DaftarTopBar(
                title = if (lang == AppLanguage.ARABIC) "إنشاء نشاط مستقل" else "Create Activity Vault",
                subtitle = uiState.selectedBusinessType.title.get(lang),
                canNavigateBack = true,
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { appViewModel.clearMessages() }) }

            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text(if (lang == AppLanguage.ARABIC) "اسم النشاط بالعربية *" else "Activity Name (Arabic) *") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(if (lang == AppLanguage.ARABIC) "الاسم بالإنجليزية (اختياري)" else "Activity Name (English)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it.uppercase().take(4) },
                    label = { Text(if (lang == AppLanguage.ARABIC) "الدولة (افتراضي SD)" else "Country (SD)") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = baseCurrency,
                    onValueChange = { baseCurrency = it.uppercase().take(5) },
                    label = { Text(if (lang == AppLanguage.ARABIC) "العملة الأساسية (SDG)" else "Base Currency (SDG)") },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = decimals,
                    onValueChange = { decimals = it.filter { c -> c.isDigit() }.take(1) },
                    label = { Text(if (lang == AppLanguage.ARABIC) "المنازل العشرية" else "Decimals") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = fiscalYearStart,
                    onValueChange = { fiscalYearStart = it },
                    label = { Text(if (lang == AppLanguage.ARABIC) "بداية السنة المالية" else "Fiscal Year Start") },
                    modifier = Modifier.weight(1.5f),
                )
            }

            Text(
                text = if (lang == AppLanguage.ARABIC) "طريقة تقييم المخزون:" else "Inventory Costing Method:",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InventoryCostingMethod.entries.forEach { method ->
                    FilterChip(
                        selected = costing == method,
                        onClick = { costing = method },
                        label = { Text(method.label.get(lang)) },
                    )
                }
            }

            HorizontalDivider()
            OutlinedTextField(
                value = adminName,
                onValueChange = { adminName = it },
                label = { Text(if (lang == AppLanguage.ARABIC) "اسم مالك النشاط *" else "Owner Full Name *") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = adminPin,
                onValueChange = { adminPin = it.filter { c -> c.isDigit() }.take(12) },
                label = { Text(if (lang == AppLanguage.ARABIC) "رمز PIN للدخول (4-12 رقماً) *" else "Owner PIN (4-12 digits) *") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    appViewModel.createNewActivity(
                        nameAr = nameAr.ifBlank { "نشاط جديد" },
                        nameEn = nameEn,
                        businessType = uiState.selectedBusinessType,
                        countryCode = countryCode.ifBlank { "SD" },
                        baseCurrency = baseCurrency.ifBlank { "SDG" },
                        decimalPlaces = decimals.toIntOrNull()?.coerceIn(0, 4) ?: 2,
                        fiscalYearStart = fiscalYearStart.ifBlank { "2026-01-01" },
                        costingMethodId = costing.id,
                        adminFullName = adminName.ifBlank { "المالك" },
                        adminPin = adminPin,
                        onCreated = onCreated,
                    )
                },
                enabled = !uiState.isBusy && adminPin.length >= 4 && nameAr.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (lang == AppLanguage.ARABIC) "إنشاء قاعدة النشاط المشفرة" else "Create Encrypted Activity DB")
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    onCreateNewActivity: () -> Unit,
    onRestoreBackup: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val activeVaults = remember(uiState.vaults) { uiState.vaults.filter { !it.isArchived } }
    var selectedVault by remember(activeVaults) { mutableStateOf(activeVaults.firstOrNull()) }
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedVault?.id) {
        val vault = selectedVault ?: return@LaunchedEffect
        appViewModel.selectVaultForLogin(vault) {}
    }

    Scaffold(
        topBar = {
            DaftarTopBar(
                title = if (lang == AppLanguage.ARABIC) "تسجيل الدخول الآمن" else "Secure Vault Unlock",
                subtitle = if (lang == AppLanguage.ARABIC) "اختر النشاط المستقل وأدخل PIN" else "Select isolated vault & unlock",
                actions = {
                    TextButton(onClick = { appViewModel.toggleLanguage() }) {
                        Text(if (lang == AppLanguage.ARABIC) "EN" else "عربي")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DaftarGeometricLogo(size = 56.dp, variant = DaftarLogoVariant.DAY)
                OfflineEncryptionBadge(lang, appViewModel.container.isHardwareKeystoreActive)
            }

            uiState.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { appViewModel.clearMessages() }) }

            if (activeVaults.isEmpty()) {
                EmptyStatePanel(
                    title = if (lang == AppLanguage.ARABIC) "لا توجد أنشطة نشطة" else "No active vaults",
                    description = if (lang == AppLanguage.ARABIC) "أنشئ نشاطاً جديداً أو استعد نسخة احتياطية" else "Create a new activity or restore backup",
                )
            } else {
                Text(
                    text = if (lang == AppLanguage.ARABIC) "الأنشطة المحاسبية المستقلة:" else "Isolated Business Vaults:",
                    style = MaterialTheme.typography.titleMedium,
                )
                activeVaults.forEach { vault ->
                    val isSel = selectedVault?.id == vault.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVault = vault },
                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = if (isSel) GoldSoft else MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (lang == AppLanguage.ARABIC) vault.nameAr else vault.nameEn, style = MaterialTheme.typography.titleMedium, color = if (isSel) NavyClassic else MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${BusinessType.fromId(vault.businessTypeId).title.get(lang)} • ${vault.baseCurrency}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSel) NavyDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isSel) Icon(Icons.Default.LockOpen, contentDescription = null, tint = GoldPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text(if (lang == AppLanguage.ARABIC) "رمز الدخول PIN" else "Access PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { appViewModel.unlockWithPin(pinInput, onAuthenticated) },
                    enabled = selectedVault != null && pinInput.length >= 4 && !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (lang == AppLanguage.ARABIC) "فتح النشاط المحاسبي" else "Unlock Vault")
                }

                if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
                    OutlinedButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = if (lang == AppLanguage.ARABIC) "فتح دفتر بالبصمة" else "Unlock DAFTAR",
                                subtitle = selectedVault?.nameAr ?: "DAFTAR Vault",
                                cancelText = if (lang == AppLanguage.ARABIC) "استخدام PIN" else "Use PIN",
                                onSuccess = { appViewModel.unlockWithBiometric(onAuthenticated) },
                                onError = { _ -> },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (lang == AppLanguage.ARABIC) "الدخول ببصمة الإصبع" else "Biometric Unlock")
                    }
                }

                var showRecoveryReset by remember { mutableStateOf(false) }
                var recKeyInput by remember { mutableStateOf("") }
                var newPinInput by remember { mutableStateOf("") }
                TextButton(onClick = { showRecoveryReset = !showRecoveryReset }) {
                    Text(if (lang == AppLanguage.ARABIC) "نسيت PIN؟ استعادة بمفتاح الاسترداد" else "Forgot PIN? Use Recovery Key")
                }
                if (showRecoveryReset) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = recKeyInput,
                                onValueChange = { recKeyInput = it },
                                label = { Text("DFTR-XXXX-XXXX-XXXX-XXXX") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it.filter { c -> c.isDigit() }.take(12) },
                                label = { Text(if (lang == AppLanguage.ARABIC) "رمز PIN الجديد (4-12 رقماً)" else "New PIN (4-12 digits)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = { appViewModel.recoverPinWithRecoveryKey(recKeyInput, newPinInput, onAuthenticated) },
                                enabled = recKeyInput.isNotBlank() && newPinInput.length >= 4 && !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (lang == AppLanguage.ARABIC) "تعيين PIN جديد والدخول" else "Reset PIN & Unlock")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCreateNewActivity, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == AppLanguage.ARABIC) "نشاط جديد" else "New Vault")
                }
                OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == AppLanguage.ARABIC) "استعادة" else "Restore")
                }
            }
        }
    }
}

@Composable
fun RestoreScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    onBack: () -> Unit,
    onRestored: () -> Unit,
) {
    val lang = uiState.language
    var recoveryKey by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) selectedUri = uri
    }

    Scaffold(
        topBar = {
            DaftarTopBar(
                title = if (lang == AppLanguage.ARABIC) "استعادة نسخة احتياطية مشفرة" else "Encrypted Vault Restore",
                subtitle = if (lang == AppLanguage.ARABIC) "فحص السلامة والتشغيل التجريبي قبل الاستبدال" else "Dry-run integrity check before commit",
                canNavigateBack = true,
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { appViewModel.clearMessages() }) }
            uiState.successMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { appViewModel.clearMessages() }) }

            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    selectedUri?.lastPathSegment
                        ?: if (lang == AppLanguage.ARABIC) "اختيار ملف النسخة (.dftrbak)" else "Select Backup Archive (.dftrbak)"
                )
            }

            OutlinedTextField(
                value = recoveryKey,
                onValueChange = { recoveryKey = it },
                label = { Text(if (lang == AppLanguage.ARABIC) "مفتاح الاسترداد أو كلمة مرور النسخة" else "Recovery Key / Backup Password") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    appViewModel.dryRunRestoreFromUri(uri, recoveryKey)
                },
                enabled = selectedUri != null && recoveryKey.length >= 6 && !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (lang == AppLanguage.ARABIC) "فك التشفير وفحص السلامة (اختبار تجريبي)" else "Decrypt & Verify (Dry-Run)")
            }

            val report = uiState.pendingRestoreReport
            if (report != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "نتيجة الفحص التجريبي: سليم ١٠٠٪" else "Dry-Run Verification Passed",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldProfit,
                        )
                        Text("SHA-256: ${report.archiveSha256.take(24)}...", style = MaterialTheme.typography.labelMedium)
                        report.vaultsInArchive.forEach { v ->
                            Text("• ${v.nameAr} (${v.baseCurrency})", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (report.requiresUserConfirmationToOverwrite) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "تحذير: يوجد تعارض مع نشاط حالي بنفس المعرّف وسيتم استبداله بموافقتك." else "Conflict detected: existing vault(s) will be overwritten upon confirmation.",
                                color = CrimsonError,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    appViewModel.confirmCommitRestore(
                                        overwriteExistingConfirmed = report.requiresUserConfirmationToOverwrite,
                                        onRestored = onRestored,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (lang == AppLanguage.ARABIC) "تأكيد الاستعادة" else "Confirm Restore")
                            }
                            OutlinedButton(
                                onClick = { appViewModel.cancelPendingRestore() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (lang == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
