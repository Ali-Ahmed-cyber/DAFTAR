package com.daftar.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object RecoveryKeyGenerator {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    data class GeneratedRecoveryKey(
        val plainKeyOnce: String,
        val digestBase64: String,
        val saltBase64: String,
    )

    fun generate(): GeneratedRecoveryKey {
        val rng = SecureRandom()
        val groups = (1..4).map {
            buildString {
                repeat(4) {
                    append(ALPHABET[rng.nextInt(ALPHABET.length)])
                }
            }
        }
        val plainKey = "DFTR-${groups.joinToString("-")}"
        val salt = ByteArray(16).also { rng.nextBytes(it) }
        val digest = hashWithSalt(plainKey, salt)
        return GeneratedRecoveryKey(
            plainKeyOnce = plainKey,
            digestBase64 = Base64.getEncoder().encodeToString(digest),
            saltBase64 = Base64.getEncoder().encodeToString(salt),
        )
    }

    fun verify(candidateKey: String, storedDigestBase64: String, storedSaltBase64: String): Boolean {
        return try {
            val normalized = candidateKey.trim().uppercase()
            val salt = Base64.getDecoder().decode(storedSaltBase64)
            val expected = Base64.getDecoder().decode(storedDigestBase64)
            val actual = hashWithSalt(normalized, salt)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Throwable) {
            false
        }
    }

    fun deriveAesKeyFromRecoveryKey(recoveryKey: String, salt: ByteArray): ByteArray {
        val raw = recoveryKey.trim()
        val normalized = if (raw.startsWith("DFTR-", ignoreCase = true)) raw.uppercase() else raw
        val spec = PBEKeySpec(normalized.toCharArray(), salt, 65_536, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun hashWithSalt(key: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(key.toByteArray(Charsets.UTF_8))
    }
}
