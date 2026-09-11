package com.aktarjabed.inbusiness.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyProvider @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }

    fun getDatabasePassphrase(): String {
        return try {
            val existingPassphrase = sharedPreferences.getString(KEY_DATABASE_PASSPHRASE, null)

            if (existingPassphrase != null) {
                Log.d(TAG, "Retrieved existing database passphrase")
                existingPassphrase
            } else {
                Log.i(TAG, "Generating new database passphrase")
                generateAndStorePassphrase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve passphrase", e)
            throw SecurityException("Failed to retrieve or generate secure database passphrase", e)
        }
    }

    private fun generateAndStorePassphrase(): String {
        val passphrase = generateSecurePassphrase()

        try {
            sharedPreferences.edit()
                .putString(KEY_DATABASE_PASSPHRASE, passphrase)
                .apply()

            Log.i(TAG, "Database passphrase generated and stored successfully")
            return passphrase
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store passphrase", e)
            throw SecurityException("Failed to store database passphrase", e)
        }
    }

    private fun generateSecurePassphrase(): String {
        val random = SecureRandom()
        val passphraseBytes = ByteArray(32)
        random.nextBytes(passphraseBytes)

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.util.Base64.getEncoder().encodeToString(passphraseBytes)
        } else {
            android.util.Base64.encodeToString(passphraseBytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            throw SecurityException("Failed to initialize secure storage", e)
        }
    }

    companion object {
        private const val TAG = "KeyProvider"
        private const val PREFS_NAME = "inbusiness_secure_prefs"
        private const val KEY_DATABASE_PASSPHRASE = "db_passphrase"
    }
}