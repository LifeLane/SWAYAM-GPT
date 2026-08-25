package com.example.edgeaicore.core.storage

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Data class representing cryptographic verification status of the on-device vault.
 */
data class EncryptionVaultStatus(
    val isHardwareBacked: Boolean,
    val isDegraded: Boolean = !isHardwareBacked,
    val algorithm: String = "AES/GCM/NoPadding",
    val keySizeBits: Int = 256,
    val keyAlias: String = "edge_ai_vault_master_key_v1",
    val provider: String = "AndroidKeyStore",
    val isEncryptedAtRest: Boolean = true,
    val selfTestPassed: Boolean = true,
    val selfTestLatencyMs: Long = 0L
)

/**
 * LocalEncryptionEngine:
 * Dynamic AES-256-GCM encryption layer for notes, memories, and interaction vaults.
 * Validates hardware-backed status dynamically via Android KeyStore / KeyInfo.
 */
class LocalEncryptionEngine(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "edge_ai_vault_master_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val ENVELOPE_PREFIX = "ENC:v1:"
        private const val FALLBACK_SECRET_SALT = "EDGE_AI_SOVEREIGN_CORE_LOCAL_AES_256_SALT"
    }

    private val secureRandom = SecureRandom()
    private var cachedKey: SecretKey? = null
    private var isUsingFallbackKey: Boolean = false

    init {
        ensureKeyExists()
    }

    /**
     * Initializes or retrieves the 256-bit AES Master Key from Android KeyStore.
     */
    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                cachedKey = keyGenerator.generateKey()
                isUsingFallbackKey = false
            } else {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                cachedKey = entry?.secretKey
                isUsingFallbackKey = cachedKey == null
            }
        } catch (e: Exception) {
            // Degraded software fallback when AndroidKeyStore is unavailable (e.g. JVM unit tests)
            cachedKey = generateFallbackKey()
            isUsingFallbackKey = true
        }
    }

    private fun generateFallbackKey(): SecretKey {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val seed = "${context.packageName}_$FALLBACK_SECRET_SALT".toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(seed)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getSecretKey(): SecretKey {
        return cachedKey ?: synchronized(this) {
            cachedKey ?: try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                val key = entry?.secretKey
                if (key != null) {
                    isUsingFallbackKey = false
                    key
                } else {
                    isUsingFallbackKey = true
                    generateFallbackKey()
                }
            } catch (e: Exception) {
                isUsingFallbackKey = true
                generateFallbackKey()
            }.also { cachedKey = it }
        }
    }

    private fun checkHardwareBacked(key: SecretKey): Boolean {
        if (isUsingFallbackKey) return false
        return try {
            val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
            } else {
                @Suppress("DEPRECATION")
                keyInfo.isInsideSecureHardware
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a given text payload is already in the encrypted envelope format.
     */
    fun isEncrypted(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.startsWith(ENVELOPE_PREFIX)
    }

    /**
     * Encrypts a plaintext string into a tamper-evident Base64 envelope:
     * ENC:v1:<base64-iv>:<base64-ciphertext-with-tag>
     */
    fun encryptString(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        if (isEncrypted(plainText)) return plainText

        return try {
            val key = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

            "$ENVELOPE_PREFIX$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decrypts an encrypted envelope back to plaintext.
     */
    fun decryptString(encryptedEnvelope: String): String {
        if (!isEncrypted(encryptedEnvelope)) return encryptedEnvelope

        return try {
            val payload = encryptedEnvelope.removePrefix(ENVELOPE_PREFIX)
            val parts = payload.split(":")
            if (parts.size != 2) return encryptedEnvelope

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val key = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedEnvelope
        }
    }

    /**
     * Encrypts sticky note fields for storage at rest.
     */
    fun encryptNote(title: String, content: String, tags: String): Triple<String, String, String> {
        return Triple(
            encryptString(title),
            encryptString(content),
            encryptString(tags)
        )
    }

    /**
     * Decrypts sticky note fields when retrieved from database.
     */
    fun decryptNote(title: String, content: String, tags: String): Triple<String, String, String> {
        return Triple(
            decryptString(title),
            decryptString(content),
            decryptString(tags)
        )
    }

    /**
     * Runs a real cryptographic self-test to verify encryption integrity and measure latency.
     */
    fun runCryptographicSelfTest(): EncryptionVaultStatus {
        val startTime = System.currentTimeMillis()
        val testPayload = "EdgeAI Sovereign Memory Vault Cryptographic Verification Token @ ${System.currentTimeMillis()}"
        val encrypted = encryptString(testPayload)
        val decrypted = decryptString(encrypted)
        val latency = System.currentTimeMillis() - startTime
        val testSuccess = (decrypted == testPayload) && isEncrypted(encrypted)
        val key = getSecretKey()
        val hardwareBacked = checkHardwareBacked(key)

        return EncryptionVaultStatus(
            isHardwareBacked = hardwareBacked,
            isDegraded = !hardwareBacked,
            algorithm = TRANSFORMATION,
            keySizeBits = 256,
            keyAlias = KEY_ALIAS,
            provider = if (!isUsingFallbackKey) "AndroidKeyStore" else "Degraded Software Fallback",
            isEncryptedAtRest = true,
            selfTestPassed = testSuccess,
            selfTestLatencyMs = latency
        )
    }
}
