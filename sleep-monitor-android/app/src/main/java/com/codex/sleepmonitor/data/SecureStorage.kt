package com.codex.sleepmonitor.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecureStorage {
    fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)
        return MAGIC + byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        require(isEncrypted(bytes)) { "Not an encrypted payload." }
        val ivSize = bytes[MAGIC.size].toInt()
        val ivStart = MAGIC.size + 1
        val ivEnd = ivStart + ivSize
        val iv = bytes.copyOfRange(ivStart, ivEnd)
        val payload = bytes.copyOfRange(ivEnd, bytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        return cipher.doFinal(payload)
    }

    fun isEncrypted(bytes: ByteArray): Boolean {
        return bytes.size > MAGIC.size + 1 && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nightpulse_local_data_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val MAGIC = byteArrayOf(
            'N'.code.toByte(),
            'P'.code.toByte(),
            '1'.code.toByte(),
            'E'.code.toByte()
        )
    }
}
