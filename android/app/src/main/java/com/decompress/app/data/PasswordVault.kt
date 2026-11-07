package com.decompress.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 密码库：按压缩包文件名哈希索引，AES-GCM + Android Keystore 加密。 */
object PasswordVault {
    private const val PREFS = "zippest-password-vault"
    private const val ALIAS = "zippest-password-vault-key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun load(context: Context, archiveName: String): String? = runCatching {
        val stored = preferences(context).getString(KEY_VAULT, null) ?: return null
        val json = JSONObject(stored)
        val encrypted = json.optString(keyOf(archiveName)).takeIf { it.isNotEmpty() }
            ?: return null
        decrypt(encrypted)
    }.getOrNull()

    fun save(context: Context, archiveName: String, password: String) {
        if (password.isEmpty()) return
        runCatching {
            val prefs = preferences(context)
            val json = prefs.getString(KEY_VAULT, null)
                ?.let { runCatching { JSONObject(it) }.getOrNull() }
                ?: JSONObject()
            json.put(keyOf(archiveName), encrypt(password))
            prefs.edit().putString(KEY_VAULT, json.toString()).apply()
        }
    }

    fun clear(context: Context) {
        preferences(context).edit().remove(KEY_VAULT).apply()
    }

    private fun preferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keyOf(name: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(name.lowercase().toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(
            cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    private fun decrypt(payload: String): String {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(128, bytes.copyOfRange(0, 12))
        )
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private const val KEY_VAULT = "vault"
}
