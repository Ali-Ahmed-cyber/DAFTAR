package com.daftar.app.domain.backup

import android.content.Context
import com.daftar.app.core.backup.BackupCryptoEngine
import com.daftar.app.core.security.VaultKeyProvider
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.registry.ActivityVaultEntity
import com.daftar.app.data.registry.RegistryDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class BackupExportResult(
    val sha256Hex: String,
    val vaultCount: Int,
    val createdAtMillis: Long,
)

data class DryRunRestoreReport(
    val archiveSha256: String,
    val exportedAtMillis: Long,
    val vaultsInArchive: List<VaultSummaryInBackup>,
    val conflictingVaultIds: List<String>,
    val totalFilesCount: Int,
) {
    val requiresUserConfirmationToOverwrite: Boolean
        get() = conflictingVaultIds.isNotEmpty()
}

data class VaultSummaryInBackup(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val businessTypeId: String,
    val baseCurrency: String,
    val dbFileName: String,
)

class VaultBackupRestoreManager(
    private val context: Context,
    private val registryDb: RegistryDatabase,
    private val keyProvider: VaultKeyProvider,
) {
    suspend fun exportVaults(
        vaults: List<ActivityVaultEntity>,
        recoveryKeyOrPassword: String,
        output: OutputStream,
        beforeReadVaultDb: (String) -> Unit = {},
    ): BackupExportResult {
        require(vaults.isNotEmpty()) { "No vaults selected for backup" }
        require(recoveryKeyOrPassword.trim().length >= 6) { "Recovery key / password too short" }

        val entries = mutableListOf<BackupCryptoEngine.BackupEntryInput>()
        val manifestVaults = JSONArray()

        for (vault in vaults) {
            beforeReadVaultDb(vault.id)
            val dbFile = context.getDatabasePath(vault.dbFileName)
            require(dbFile.exists()) { "Encrypted vault database missing: ${vault.dbFileName}" }

            // Decrypt into an in-backup portable encrypted payload alongside raw vault DB + raw key material wrapped cleanly inside outer AES-256-GCM archive
            val dbBytes = dbFile.readBytes()
            val dbKeyBytes = keyProvider.getOrCreateVaultPassphrase(vault.id)

            entries.add(BackupCryptoEngine.BackupEntryInput("db/${vault.dbFileName}", dbBytes))
            entries.add(BackupCryptoEngine.BackupEntryInput("keys/${vault.id}.raw", dbKeyBytes))

            manifestVaults.put(
                JSONObject().apply {
                    put("id", vault.id)
                    put("nameAr", vault.nameAr)
                    put("nameEn", vault.nameEn)
                    put("businessTypeId", vault.businessTypeId)
                    put("dbFileName", vault.dbFileName)
                    put("countryCode", vault.countryCode)
                    put("baseCurrency", vault.baseCurrency)
                    put("decimalPlaces", vault.decimalPlaces)
                    put("fiscalYearStart", vault.fiscalYearStart)
                    put("costingMethodId", vault.costingMethodId)
                    put("isArchived", vault.isArchived)
                    put("recoveryKeyDigest", vault.recoveryKeyDigest)
                    put("recoveryKeySalt", vault.recoveryKeySalt)
                    put("createdAt", vault.createdAt)
                    put("updatedAt", vault.updatedAt)
                }
            )
        }

        val now = System.currentTimeMillis()
        val manifest = JSONObject().apply {
            put("schemaVersion", 1)
            put("app", "DAFTAR")
            put("exportedAtMillis", now)
            put("vaults", manifestVaults)
        }
        entries.add(
            BackupCryptoEngine.BackupEntryInput(
                "manifest.json",
                manifest.toString(2).toByteArray(Charsets.UTF_8)
            )
        )

        val shaHex = BackupCryptoEngine.encryptArchive(entries, recoveryKeyOrPassword, output)
        return BackupExportResult(shaHex, vaults.size, now)
    }

    /**
     * Step 1 of safe restore: Dry-run decrypt + SHA-256 verification + SQLCipher readability check.
     * Does NOT overwrite any live database file.
     */
    suspend fun dryRunVerifyBackup(
        input: InputStream,
        recoveryKeyOrPassword: String,
    ): Pair<DryRunRestoreReport, BackupCryptoEngine.VerifiedBackupPayload> {
        val verified = BackupCryptoEngine.decryptAndVerifyArchive(input, recoveryKeyOrPassword)
        val manifestBytes = verified.entries["manifest.json"]
            ?: throw IllegalStateException("Backup archive missing manifest.json")
        val manifestJson = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val vaultArray = manifestJson.getJSONArray("vaults")
        val existingIds = registryDb.registryDao().getAllVaults().map { it.id }.toSet()

        val summaries = mutableListOf<VaultSummaryInBackup>()
        val conflicts = mutableListOf<String>()

        for (i in 0 until vaultArray.length()) {
            val v = vaultArray.getJSONObject(i)
            val id = v.getString("id")
            val dbName = v.getString("dbFileName")
            require(verified.entries.containsKey("db/$dbName")) { "Missing database file db/$dbName" }
            require(verified.entries.containsKey("keys/$id.raw")) { "Missing wrapped key keys/$id.raw" }

            // Test opening DB in temporary sandbox file to guarantee zero corruption before live commit
            val tempFile = File.createTempFile("daftar_dryrun_", ".db", context.cacheDir)
            try {
                tempFile.writeBytes(verified.entries.getValue("db/$dbName"))
                val rawKey = verified.entries.getValue("keys/$id.raw")
                val testDb = ActivityDatabase.openEncrypted(context, tempFile.absolutePath, rawKey)
                testDb.accountingDao().getProfile()
                testDb.close()
            } finally {
                tempFile.delete()
            }

            if (id in existingIds) conflicts.add(id)
            summaries.add(
                VaultSummaryInBackup(
                    id = id,
                    nameAr = v.getString("nameAr"),
                    nameEn = v.getString("nameEn"),
                    businessTypeId = v.getString("businessTypeId"),
                    baseCurrency = v.getString("baseCurrency"),
                    dbFileName = dbName,
                )
            )
        }

        val report = DryRunRestoreReport(
            archiveSha256 = verified.sha256Hex,
            exportedAtMillis = manifestJson.optLong("exportedAtMillis", 0L),
            vaultsInArchive = summaries,
            conflictingVaultIds = conflicts,
            totalFilesCount = verified.entries.size,
        )
        return report to verified
    }

    /**
     * Step 2 of safe restore: Commit restored vaults ONLY after user explicitly reviewed dry-run
     * and confirmed overwrite if conflicting IDs exist.
     */
    suspend fun commitVerifiedRestore(
        payload: BackupCryptoEngine.VerifiedBackupPayload,
        overwriteExistingConfirmed: Boolean,
        closeActiveVaultBeforeWrite: (String) -> Unit = {},
    ): List<ActivityVaultEntity> {
        val manifestBytes = payload.entries.getValue("manifest.json")
        val manifestJson = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val vaultArray = manifestJson.getJSONArray("vaults")
        val existingIds = registryDb.registryDao().getAllVaults().map { it.id }.toSet()

        val restored = mutableListOf<ActivityVaultEntity>()
        val keyDir = File(context.noBackupFilesDir, "vault_keys").apply { mkdirs() }

        for (i in 0 until vaultArray.length()) {
            val v = vaultArray.getJSONObject(i)
            val originalId = v.getString("id")
            val isConflict = originalId in existingIds
            if (isConflict && !overwriteExistingConfirmed) {
                continue
            }

            val dbName = v.getString("dbFileName")
            val targetId = originalId
            closeActiveVaultBeforeWrite(targetId)

            val dbBytes = payload.entries.getValue("db/$dbName")
            val rawKey = payload.entries.getValue("keys/$originalId.raw")

            val targetDbFile = context.getDatabasePath(dbName)
            targetDbFile.parentFile?.mkdirs()
            targetDbFile.writeBytes(dbBytes)

            // Re-wrap key using this device's Android Keystore or restore raw key through KeyProvider
            // If AndroidKeystore exists, wrap freshly via master key by deleting stale and seeding or using Keystore cipher
            reinstallVaultKey(targetId, rawKey, keyDir)

            val vaultEntity = ActivityVaultEntity(
                id = targetId,
                nameAr = v.getString("nameAr"),
                nameEn = v.getString("nameEn"),
                businessTypeId = v.getString("businessTypeId"),
                dbFileName = dbName,
                countryCode = v.getString("countryCode"),
                baseCurrency = v.getString("baseCurrency"),
                decimalPlaces = v.getInt("decimalPlaces"),
                fiscalYearStart = v.getString("fiscalYearStart"),
                costingMethodId = v.getString("costingMethodId"),
                isArchived = v.optBoolean("isArchived", false),
                recoveryKeyDigest = v.getString("recoveryKeyDigest"),
                recoveryKeySalt = v.getString("recoveryKeySalt"),
                createdAt = v.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = System.currentTimeMillis(),
            )
            registryDb.registryDao().insertVault(vaultEntity)
            restored.add(vaultEntity)
        }

        return restored
    }

    private fun reinstallVaultKey(vaultId: String, rawPassphrase: ByteArray, keyDir: File) {
        // Store wrapped with Android Keystore using AndroidKeystoreVaultKeyProvider spec
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "daftar_master_vault_wrap_v1"
        val secretKey = ks.getKey(alias, null) as? javax.crypto.SecretKey
        val safeId = vaultId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val keyFile = File(keyDir, "$safeId.key")

        if (secretKey != null) {
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val enc = cipher.doFinal(rawPassphrase)
            keyFile.writeBytes(byteArrayOf(iv.size.toByte()) + iv + enc)
        } else {
            // Ensure Keystore master key gets created by calling keyProvider first, then overwrite wrapped bytes if supported
            keyProvider.getOrCreateVaultPassphrase(UUID.randomUUID().toString()).let {
                val createdKey = ks.getKey(alias, null) as? javax.crypto.SecretKey
                if (createdKey != null) {
                    val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, createdKey)
                    val iv = cipher.iv
                    val enc = cipher.doFinal(rawPassphrase)
                    keyFile.writeBytes(byteArrayOf(iv.size.toByte()) + iv + enc)
                }
            }
        }
    }
}
