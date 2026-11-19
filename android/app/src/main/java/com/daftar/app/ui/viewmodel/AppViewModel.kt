package com.daftar.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.core.backup.BackupCryptoEngine
import com.daftar.app.core.di.ActiveVaultSession
import com.daftar.app.core.di.DaftarAppContainer
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.AppThemeMode
import com.daftar.app.core.model.BusinessType
import com.daftar.app.data.registry.ActivityVaultEntity
import com.daftar.app.data.registry.AppPreferencesEntity
import com.daftar.app.domain.backup.DryRunRestoreReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val vaults: List<ActivityVaultEntity> = emptyList(),
    val selectedBusinessType: BusinessType = BusinessType.RETAIL,
    val generatedRecoveryKeyOnce: String? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingRestoreReport: DryRunRestoreReport? = null,
    val pendingRestorePayload: BackupCryptoEngine.VerifiedBackupPayload? = null,
)

class AppViewModel(
    application: Application,
    val container: DaftarAppContainer,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val session: StateFlow<ActiveVaultSession?> = container.session

    init {
        viewModelScope.launch(Dispatchers.IO) {
            container.registryDb.registryDao().observePreferences().collectLatest { prefs ->
                if (prefs != null) {
                    _uiState.update {
                        it.copy(
                            language = AppLanguage.fromCode(prefs.languageCode),
                            themeMode = AppThemeMode.fromId(prefs.themeModeId),
                        )
                    }
                } else {
                    container.registryDb.registryDao().upsertPreferences(AppPreferencesEntity())
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            container.registryDb.registryDao().observeAllVaults().collectLatest { vaults ->
                _uiState.update { it.copy(vaults = vaults) }
            }
        }
    }

    fun selectBusinessType(type: BusinessType) {
        _uiState.update { it.copy(selectedBusinessType = type) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun acknowledgeRecoveryKey() {
        _uiState.update { it.copy(generatedRecoveryKeyOnce = null) }
    }

    fun toggleLanguage() {
        viewModelScope.launch(Dispatchers.IO) {
            val next = if (_uiState.value.language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
            val cur = container.registryDb.registryDao().getPreferences() ?: AppPreferencesEntity()
            container.registryDb.registryDao().upsertPreferences(cur.copy(languageCode = next.code))
        }
    }

    fun toggleTheme() {
        viewModelScope.launch(Dispatchers.IO) {
            val next = if (_uiState.value.themeMode == AppThemeMode.LIGHT) AppThemeMode.DARK else AppThemeMode.LIGHT
            val cur = container.registryDb.registryDao().getPreferences() ?: AppPreferencesEntity()
            container.registryDb.registryDao().upsertPreferences(cur.copy(themeModeId = next.id))
        }
    }

    fun createNewActivity(
        nameAr: String,
        nameEn: String,
        businessType: BusinessType,
        countryCode: String,
        baseCurrency: String,
        decimalPlaces: Int,
        fiscalYearStart: String,
        costingMethodId: String,
        adminFullName: String,
        adminPin: String,
        onCreated: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val (_, recoveryPlain) = container.createIsolatedActivity(
                    nameAr = nameAr,
                    nameEn = nameEn,
                    businessType = businessType,
                    countryCode = countryCode,
                    baseCurrency = baseCurrency,
                    decimalPlaces = decimalPlaces,
                    fiscalYearStart = fiscalYearStart,
                    costingMethodId = costingMethodId,
                    adminFullName = adminFullName,
                    adminPin = adminPin,
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        generatedRecoveryKeyOnce = recoveryPlain,
                        successMessage = if (it.language == AppLanguage.ARABIC) "تم إنشاء النشاط المستقل وتشفيره" else "Isolated encrypted activity created",
                    )
                }
                viewModelScope.launch(Dispatchers.Main) { onCreated() }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isBusy = false, errorMessage = e.message ?: "Failed to create activity") }
            }
        }
    }

    fun selectVaultForLogin(vault: ActivityVaultEntity, onReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                container.openVaultLocked(vault)
                _uiState.update { it.copy(isBusy = false) }
                viewModelScope.launch(Dispatchers.Main) { onReady() }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isBusy = false, errorMessage = e.message ?: "Unable to unlock vault") }
            }
        }
    }

    fun unlockWithPin(pin: String, onAuthenticated: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = container.authenticatePin(pin)
            if (result.isSuccess) {
                _uiState.update { it.copy(isBusy = false) }
                viewModelScope.launch(Dispatchers.Main) { onAuthenticated() }
            } else {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = if (it.language == AppLanguage.ARABIC) "رمز PIN غير صحيح أو الحساب مقفل مؤقتاً" else "Invalid PIN or temporarily locked"
                    )
                }
            }
        }
    }

    fun unlockWithBiometric(onAuthenticated: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = container.authenticateBiometric()
            if (res.isSuccess) {
                viewModelScope.launch(Dispatchers.Main) { onAuthenticated() }
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message) }
            }
        }
    }

    fun recoverPinWithRecoveryKey(recoveryKey: String, newPin: String, onAuthenticated: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = container.recoverOwnerPinWithRecoveryKey(recoveryKey, newPin)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        successMessage = if (it.language == AppLanguage.ARABIC) "تم تعيين رمز PIN جديد بنجاح" else "New PIN configured via recovery key",
                    )
                }
                viewModelScope.launch(Dispatchers.Main) { onAuthenticated() }
            } else {
                _uiState.update {
                    it.copy(isBusy = false, errorMessage = res.exceptionOrNull()?.message ?: "Invalid recovery key")
                }
            }
        }
    }

    fun toggleArchiveVault(vault: ActivityVaultEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            container.registryDb.registryDao().setArchived(vault.id, !vault.isArchived, System.currentTimeMillis())
        }
    }

    fun exportBackupToUri(
        uri: Uri,
        vaultIds: Set<String>?,
        recoveryKeyOrPassword: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val allVaults = container.registryDb.registryDao().getAllVaults()
                val selected = if (vaultIds.isNullOrEmpty()) allVaults else allVaults.filter { it.id in vaultIds }
                val os = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: error("Cannot open destination URI")
                os.use { out ->
                    val res = container.backupManager.exportVaults(
                        vaults = selected,
                        recoveryKeyOrPassword = recoveryKeyOrPassword,
                        output = out,
                        beforeReadVaultDb = { id ->
                            if (container.session.value?.vault?.id == id) {
                                container.closeCurrentVault()
                            }
                        }
                    )
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            successMessage = if (it.language == AppLanguage.ARABIC) {
                                "تم حفظ النسخة المشفرة (${res.vaultCount} نشاط) | SHA-256: ${res.sha256Hex.take(12)}..."
                            } else {
                                "Encrypted backup saved (${res.vaultCount} vaults) | SHA-256: ${res.sha256Hex.take(12)}..."
                            }
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isBusy = false, errorMessage = e.message ?: "Backup export failed") }
            }
        }
    }

    fun dryRunRestoreFromUri(uri: Uri, recoveryKeyOrPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val ins = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: error("Cannot open backup file")
                ins.use { stream ->
                    val (report, payload) = container.backupManager.dryRunVerifyBackup(stream, recoveryKeyOrPassword)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestoreReport = report,
                            pendingRestorePayload = payload,
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isBusy = false, errorMessage = e.message ?: "Restore verification failed") }
            }
        }
    }

    fun confirmCommitRestore(overwriteExistingConfirmed: Boolean, onRestored: () -> Unit) {
        val payload = _uiState.value.pendingRestorePayload ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val restored = container.backupManager.commitVerifiedRestore(
                    payload = payload,
                    overwriteExistingConfirmed = overwriteExistingConfirmed,
                    closeActiveVaultBeforeWrite = { id ->
                        if (container.session.value?.vault?.id == id) {
                            container.closeCurrentVault()
                        }
                    }
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        pendingRestoreReport = null,
                        pendingRestorePayload = null,
                        successMessage = if (it.language == AppLanguage.ARABIC) {
                            "تمت استعادة ${restored.size} نشاط بنجاح"
                        } else {
                            "Restored ${restored.size} activity vault(s)"
                        }
                    )
                }
                viewModelScope.launch(Dispatchers.Main) { onRestored() }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isBusy = false, errorMessage = e.message ?: "Restore commit failed") }
            }
        }
    }

    fun cancelPendingRestore() {
        _uiState.update { it.copy(pendingRestoreReport = null, pendingRestorePayload = null) }
    }
}
