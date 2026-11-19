package com.daftar.app.core.backup

import com.daftar.app.core.security.RecoveryKeyGenerator
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoEngine {
    private val MAGIC = "DFTRBK01".toByteArray(Charsets.US_ASCII)
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val HASH_LEN = 32

    data class BackupEntryInput(val name: String, val bytes: ByteArray)
    data class VerifiedBackupPayload(val entries: Map<String, ByteArray>, val sha256Hex: String)

    fun encryptArchive(
        files: List<BackupEntryInput>,
        recoveryKeyOrPassword: String,
        output: OutputStream,
    ): String {
        val zipBytes = createZipBytes(files)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(zipBytes)
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val aesKey = RecoveryKeyGenerator.deriveAesKeyFromRecoveryKey(recoveryKeyOrPassword, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(zipBytes)

        output.write(MAGIC)
        output.write(salt)
        output.write(iv)
        output.write(sha256)
        output.write(encrypted)
        output.flush()

        return sha256.joinToString("") { "%02x".format(it) }
    }

    fun decryptAndVerifyArchive(
        input: InputStream,
        recoveryKeyOrPassword: String,
    ): VerifiedBackupPayload {
        val allBytes = input.readBytes()
        val headerLen = MAGIC.size + SALT_LEN + IV_LEN + HASH_LEN
        require(allBytes.size > headerLen) { "Corrupted or truncated backup archive" }

        val magic = allBytes.copyOfRange(0, MAGIC.size)
        require(magic.contentEquals(MAGIC)) { "Invalid backup file format header" }

        var offset = MAGIC.size
        val salt = allBytes.copyOfRange(offset, offset + SALT_LEN); offset += SALT_LEN
        val iv = allBytes.copyOfRange(offset, offset + IV_LEN); offset += IV_LEN
        val expectedHash = allBytes.copyOfRange(offset, offset + HASH_LEN); offset += HASH_LEN
        val ciphertext = allBytes.copyOfRange(offset, allBytes.size)

        val aesKey = RecoveryKeyGenerator.deriveAesKeyFromRecoveryKey(recoveryKeyOrPassword, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        val plainZip = try {
            cipher.doFinal(ciphertext)
        } catch (e: Throwable) {
            throw SecurityException("Wrong recovery key / password or tampered backup archive", e)
        }

        val actualHash = MessageDigest.getInstance("SHA-256").digest(plainZip)
        require(MessageDigest.isEqual(expectedHash, actualHash)) {
            "SHA-256 integrity mismatch in backup archive"
        }

        val entries = readZipBytes(plainZip)
        require(entries.isNotEmpty()) { "Backup archive contains no files" }
        return VerifiedBackupPayload(
            entries = entries,
            sha256Hex = actualHash.joinToString("") { "%02x".format(it) },
        )
    }

    private fun createZipBytes(files: List<BackupEntryInput>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for (f in files) {
                zos.putNextEntry(ZipEntry(f.name))
                zos.write(f.bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun readZipBytes(zipBytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    result[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }
}
