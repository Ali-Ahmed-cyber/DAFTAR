package com.daftar.app.core.di

import android.content.Context
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.security.AndroidKeystoreVaultKeyProvider
import com.daftar.app.core.security.PinHasher
import com.daftar.app.core.security.RecoveryKeyGenerator
import com.daftar.app.core.security.VaultKeyProvider
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.AuditLogEntity
import com.daftar.app.data.activity.entities.UserEntity
import com.daftar.app.data.registry.ActivityVaultEntity
import com.daftar.app.data.registry.AppPreferencesEntity
import com.daftar.app.data.registry.RegistryDatabase
import com.daftar.app.domain.accounting.DoubleEntryEngine
import com.daftar.app.domain.accounting.StarterChartOfAccounts
import com.daftar.app.domain.assistant.LocalSmartAssistantEngine
import com.daftar.app.domain.audit.DiagnosticAuditEngine
import com.daftar.app.domain.backup.VaultBackupRestoreManager
import com.daftar.app.domain.commerce.CommerceTransactionEngine
import com.daftar.app.domain.inventory.InventoryValuationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ActiveVaultSession(
    val vault: ActivityVaultEntity,
    val db: ActivityDatabase,
    val authenticatedUser: UserEntity?,
    val doubleEntry: DoubleEntryEngine,
    val inventoryEngine: InventoryValuationEngine,
    val commerceEngine: CommerceTransactionEngine,
    val auditEngine: DiagnosticAuditEngine,
    val assistantEngine: LocalSmartAssistantEngine,
)

class DaftarAppContainer(
    private val context: Context,
    private val keyProvider: VaultKeyProvider = AndroidKeystoreVaultKeyProvider(context),
) {
    val registryDb: RegistryDatabase by lazy {
        val registryKey = keyProvider.getOrCreateVaultPassphrase("daftar_master_registry")
        RegistryDatabase.create(context, registryKey)
    }

    val backupManager: VaultBackupRestoreManager by lazy {
        VaultBackupRestoreManager(context, registryDb, keyProvider)
    }

    private val _session = MutableStateFlow<ActiveVaultSession?>(null)
    val session: StateFlow<ActiveVaultSession?> = _session.asStateFlow()

    val isHardwareKeystoreActive: Boolean
        get() = keyProvider.isHardwareKeystoreBacked()

    suspend fun createIsolatedActivity(
        nameAr: String,
        nameEn: String,
        businessType: BusinessType,
        countryCode: String = "SD",
        baseCurrency: String = "SDG",
        decimalPlaces: Int = 2,
        fiscalYearStart: String = "2026-01-01",
        costingMethodId: String = "wac",
        adminFullName: String,
        adminPin: String,
    ): Pair<ActivityVaultEntity, String> {
        val vaultId = UUID.randomUUID().toString()
        val shortId = vaultId.take(8)
        val dbFileName = "daftar_vault_${businessType.id}_$shortId.db"
        val recovery = RecoveryKeyGenerator.generate()
        val now = System.currentTimeMillis()

        val vault = ActivityVaultEntity(
            id = vaultId,
            nameAr = nameAr.trim(),
            nameEn = nameEn.ifBlank { nameAr }.trim(),
            businessTypeId = businessType.id,
            dbFileName = dbFileName,
            countryCode = countryCode,
            baseCurrency = baseCurrency,
            decimalPlaces = decimalPlaces,
            fiscalYearStart = fiscalYearStart,
            costingMethodId = costingMethodId,
            isArchived = false,
            recoveryKeyDigest = recovery.digestBase64,
            recoveryKeySalt = recovery.saltBase64,
            createdAt = now,
            updatedAt = now,
        )

        closeCurrentVault()

        val passphrase = keyProvider.getOrCreateVaultPassphrase(vault.id)
        val db = ActivityDatabase.openEncrypted(context, vault.dbFileName, passphrase)
        val ownerUser = StarterChartOfAccounts.initializeNewActivityDatabase(
            db = db,
            vaultId = vault.id,
            nameAr = vault.nameAr,
            nameEn = vault.nameEn,
            businessType = businessType,
            countryCode = countryCode,
            baseCurrency = baseCurrency,
            decimalPlaces = decimalPlaces,
            fiscalYearStart = fiscalYearStart,
            costingMethodId = costingMethodId,
            adminFullName = adminFullName,
            adminPin = adminPin,
        )

        registryDb.registryDao().insertVault(vault)
        val currentPrefs = registryDb.registryDao().getPreferences() ?: AppPreferencesEntity()
        registryDb.registryDao().upsertPreferences(currentPrefs.copy(lastActiveVaultId = vault.id))

        openVaultInternal(vault, db, ownerUser)
        return vault to recovery.plainKeyOnce
    }

    @Synchronized
    fun openVaultLocked(vault: ActivityVaultEntity): ActiveVaultSession {
        val current = _session.value
        if (current != null && current.vault.id == vault.id) {
            return current
        }
        closeCurrentVault()
        val passphrase = keyProvider.getOrCreateVaultPassphrase(vault.id)
        val db = ActivityDatabase.openEncrypted(context, vault.dbFileName, passphrase)
        return openVaultInternal(vault, db, null)
    }

    private fun openVaultInternal(
        vault: ActivityVaultEntity,
        db: ActivityDatabase,
        user: UserEntity?,
    ): ActiveVaultSession {
        val doubleEntry = DoubleEntryEngine(db)
        val inventoryEngine = InventoryValuationEngine(db, doubleEntry)
        val commerceEngine = CommerceTransactionEngine(db, doubleEntry, inventoryEngine)
        val auditEngine = DiagnosticAuditEngine(db)
        val assistantEngine = LocalSmartAssistantEngine(db, auditEngine)
        val newSession = ActiveVaultSession(
            vault = vault,
            db = db,
            authenticatedUser = user,
            doubleEntry = doubleEntry,
            inventoryEngine = inventoryEngine,
            commerceEngine = commerceEngine,
            auditEngine = auditEngine,
            assistantEngine = assistantEngine,
        )
        _session.value = newSession
        return newSession
    }

    suspend fun authenticatePin(pin: String): Result<UserEntity> {
        val current = _session.value ?: return Result.failure(IllegalStateException("No vault selected"))
        val govDao = current.db.governanceDao()
        val users = govDao.getActiveUsers()
        val now = System.currentTimeMillis()

        val matched = users.firstOrNull { user ->
            user.lockedUntilMillis <= now && PinHasher.verifyPin(pin, user.pinHash, user.pinSalt)
        }

        if (matched != null) {
            govDao.updateUser(matched.copy(failedAttempts = 0, lockedUntilMillis = 0L))
            govDao.appendAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    userId = matched.id,
                    userName = matched.fullName,
                    actionType = "LOGIN_SUCCESS",
                    entityType = "AUTH",
                    entityId = matched.id,
                    summary = "Authenticated via PIN (${matched.roleId})",
                    isSecurityEvent = true,
                )
            )
            _session.value = current.copy(authenticatedUser = matched)
            val prefs = registryDb.registryDao().getPreferences() ?: AppPreferencesEntity()
            registryDb.registryDao().upsertPreferences(prefs.copy(lastActiveVaultId = current.vault.id))
            return Result.success(matched)
        } else {
            val primary = users.firstOrNull()
            if (primary != null) {
                val fails = primary.failedAttempts + 1
                val lockUntil = if (fails >= 5) now + 60_000L else 0L
                govDao.updateUser(primary.copy(failedAttempts = fails, lockedUntilMillis = lockUntil))
                govDao.appendAuditLog(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = now,
                        userId = primary.id,
                        userName = primary.fullName,
                        actionType = "LOGIN_FAILED",
                        entityType = "AUTH",
                        entityId = primary.id,
                        summary = "Failed PIN attempt #$fails",
                        isSecurityEvent = true,
                    )
                )
            }
            return Result.failure(SecurityException("Invalid PIN or account temporarily locked"))
        }
    }

    suspend fun authenticateBiometric(): Result<UserEntity> {
        val current = _session.value ?: return Result.failure(IllegalStateException("No vault selected"))
        val users = current.db.governanceDao().getActiveUsers()
        val target = users.firstOrNull { it.biometricEnabled } ?: users.firstOrNull()
            ?: return Result.failure(IllegalStateException("No active users in vault"))
        current.db.governanceDao().appendAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                userId = target.id,
                userName = target.fullName,
                actionType = "LOGIN_BIOMETRIC",
                entityType = "AUTH",
                entityId = target.id,
                summary = "Authenticated via device biometric prompt",
                isSecurityEvent = true,
            )
        )
        _session.value = current.copy(authenticatedUser = target)
        return Result.success(target)
    }

    suspend fun recoverOwnerPinWithRecoveryKey(
        recoveryKey: String,
        newPin: String,
    ): Result<UserEntity> {
        val current = _session.value ?: return Result.failure(IllegalStateException("No vault selected"))
        val valid = RecoveryKeyGenerator.verify(
            candidateKey = recoveryKey,
            storedDigestBase64 = current.vault.recoveryKeyDigest,
            storedSaltBase64 = current.vault.recoveryKeySalt,
        )
        if (!valid) {
            return Result.failure(SecurityException("Invalid vault recovery key"))
        }
        val users = current.db.governanceDao().getActiveUsers()
        val owner = users.firstOrNull { it.roleId == "owner" } ?: users.firstOrNull()
            ?: return Result.failure(IllegalStateException("No owner user found"))
        val hashed = PinHasher.hashPin(newPin)
        val updated = owner.copy(
            pinHash = hashed.hashBase64,
            pinSalt = hashed.saltBase64,
            failedAttempts = 0,
            lockedUntilMillis = 0L,
        )
        current.db.governanceDao().updateUser(updated)
        current.db.governanceDao().appendAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                userId = updated.id,
                userName = updated.fullName,
                actionType = "PIN_RECOVERED_VIA_KEY",
                entityType = "AUTH",
                entityId = updated.id,
                summary = "Owner PIN reset offline using verified vault recovery key",
                isSecurityEvent = true,
            )
        )
        _session.value = current.copy(authenticatedUser = updated)
        return Result.success(updated)
    }

    fun lockCurrentSession() {
        val current = _session.value ?: return
        _session.value = current.copy(authenticatedUser = null)
    }

    @Synchronized
    fun closeCurrentVault() {
        val current = _session.value
        if (current != null) {
            try {
                if (current.db.isOpen) {
                    current.db.close()
                }
            } catch (_: Throwable) {
            }
            _session.value = null
        }
    }
}
