package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PersistedSessionMetadata(
    val uid: String,
    val providerId: String,
    val lastSignInAtMillis: Long,
    val lastVerifiedAtMillis: Long
)

/**
 * Encrypts non-secret session metadata (UID, provider, sign-in timestamp, last verification
 * timestamp) with an Android Keystore AES-256-GCM key. Never stores passwords or refresh tokens.
 */
class SecureSessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(metadata: PersistedSessionMetadata) {
        val payload = JSONObject().apply {
            put("uid", metadata.uid)
            put("providerId", metadata.providerId)
            put("lastSignInAtMillis", metadata.lastSignInAtMillis)
            put("lastVerifiedAtMillis", metadata.lastVerifiedAtMillis)
        }.toString()

        val encoded = encryptWithKeystore(payload)
        prefs.edit().apply {
            if (encoded != null) {
                putString(KEY_ENCRYPTED_SESSION, encoded)
                remove(KEY_FALLBACK_SESSION)
            } else {
                // Unit/JVM test environments without AndroidKeyStore: keep only non-secret metadata
                putString(KEY_FALLBACK_SESSION, payload)
                remove(KEY_ENCRYPTED_SESSION)
            }
        }.commit()
    }

    fun read(expectedUid: String? = null): PersistedSessionMetadata? {
        val rawJson = prefs.getString(KEY_ENCRYPTED_SESSION, null)?.let { decryptWithKeystore(it) }
            ?: prefs.getString(KEY_FALLBACK_SESSION, null)
            ?: return null

        return try {
            val json = JSONObject(rawJson)
            val uid = json.optString("uid").takeIf { it.isNotBlank() } ?: return null
            if (!expectedUid.isNullOrBlank() && uid != expectedUid) return null
            PersistedSessionMetadata(
                uid = uid,
                providerId = json.optString("providerId", "firebase"),
                lastSignInAtMillis = json.optLong("lastSignInAtMillis", 0L),
                lastVerifiedAtMillis = json.optLong("lastVerifiedAtMillis", 0L)
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun isSessionExpiredByAge(
        metadata: PersistedSessionMetadata?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (metadata == null || metadata.lastSignInAtMillis <= 0L) return false
        return (nowMillis - metadata.lastSignInAtMillis) > SESSION_MAX_AGE_MS
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_SESSION)
            .remove(KEY_FALLBACK_SESSION)
            .commit()
    }

    private fun encryptWithKeystore(plainText: String): String? {
        return try {
            val key = getOrCreateSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(1 + iv.size + cipherBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decryptWithKeystore(encoded: String): String? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < 2) return null
            val ivLen = combined[0].toInt() and 0xFF
            if (combined.size <= 1 + ivLen) return null
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val cipherBytes = combined.copyOfRange(1 + ivLen, combined.size)
            val key = getOrCreateSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "marketzed_secure_session"
        private const val KEY_ENCRYPTED_SESSION = "session_meta_enc"
        private const val KEY_FALLBACK_SESSION = "session_meta_plain"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "marketzed_session_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        const val SESSION_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
