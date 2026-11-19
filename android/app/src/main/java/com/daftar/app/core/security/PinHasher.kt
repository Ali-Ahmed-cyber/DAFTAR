package com.daftar.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedPin(val hashBase64: String, val saltBase64: String)

    fun hashPin(pin: String): HashedPin {
        require(pin.length in 4..12 && pin.all { it.isDigit() }) {
            "PIN must be 4 to 12 numeric digits"
        }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = pbkdf2(pin.toCharArray(), salt)
        return HashedPin(
            hashBase64 = Base64.getEncoder().encodeToString(derived),
            saltBase64 = Base64.getEncoder().encodeToString(salt),
        )
    }

    fun verifyPin(pin: String, storedHashBase64: String, storedSaltBase64: String): Boolean {
        if (pin.isBlank() || storedHashBase64.isBlank() || storedSaltBase64.isBlank()) return false
        return try {
            val salt = Base64.getDecoder().decode(storedSaltBase64)
            val expected = Base64.getDecoder().decode(storedHashBase64)
            val actual = pbkdf2(pin.toCharArray(), salt)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Throwable) {
            false
        }
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
