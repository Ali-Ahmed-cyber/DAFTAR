package com.daftar.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface VaultKeyProvider {
    fun getOrCreateVaultPassphrase(vaultId: String): ByteArray
    fun deleteVaultKey(vaultId: String)
    fun isHardwareKeystoreBacked(): Boolean
}

class AndroidKeystoreVaultKeyProvider(
    private val context: Context,
) : VaultKeyProvider {

    private val keyStoreAlias = "daftar_master_vault_wrap_v1"
    private val keyDir: File by lazy {
        File(context.noBackupFilesDir, "vault_keys").apply { mkdirs() }
    }

    override fun isHardwareKeystoreBacked(): Boolean {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ks.containsAlias(keyStoreAlias) || getOrCreateMasterKey() != null
        } catch (_: Throwable) {
            false
        }
    }

    @Synchronized
    override fun getOrCreateVaultPassphrase(vaultId: String): ByteArray {
        val safeId = vaultId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val keyFile = File(keyDir, "$safeId.key")
        val masterKey = getOrCreateMasterKey()

        if (keyFile.exists()) {
            val wrapped = keyFile.readBytes()
            return unwrapPassphrase(masterKey, wrapped)
        }

        val rawPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrapped = wrapPassphrase(masterKey, rawPassphrase)
        keyFile.writeBytes(wrapped)
        return rawPassphrase
    }

    @Synchronized
    override fun deleteVaultKey(vaultId: String) {
        val safeId = vaultId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val keyFile = File(keyDir, "$safeId.key")
        if (keyFile.exists()) {
            keyFile.writeBytes(ByteArray(keyFile.length().toInt()))
            keyFile.delete()
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(keyStoreAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun wrapPassphrase(masterKey: SecretKey, raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(raw)
        return byteArrayOf(iv.size.toByte()) + iv + ciphertext
    }

    private fun unwrapPassphrase(masterKey: SecretKey, packed: ByteArray): ByteArray {
        val ivLen = packed[0].toInt() and 0xFF
        val iv = packed.copyOfRange(1, 1 + ivLen)
        val ciphertext = packed.copyOfRange(1 + ivLen, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    companion object {
        fun toHexPassphrase(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
