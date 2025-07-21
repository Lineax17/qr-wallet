package com.example.qr_wallet.data.migration

import android.content.Context
import android.util.Log
import com.example.qr_wallet.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppVersionInfo(
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val versionName: String = BuildConfig.VERSION_NAME,
    val lastMigrated: Long = System.currentTimeMillis()
)

class AppMigration(private val context: Context) {
    private val versionFile = File(context.filesDir, "app_version.json")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "AppMigration"
    }

    fun checkAndMigrateIfNeeded() {
        val currentVersionInfo = getCurrentVersionInfo()
        val newVersionInfo = AppVersionInfo(
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME
        )

        if (currentVersionInfo.versionCode < BuildConfig.VERSION_CODE) {
            Log.i(TAG, "Migration needed from version ${currentVersionInfo.versionCode} to ${BuildConfig.VERSION_CODE}")
            performMigration(currentVersionInfo.versionCode, BuildConfig.VERSION_CODE)
            saveVersionInfo(newVersionInfo)
            Log.i(TAG, "Migration completed successfully")
        } else if (currentVersionInfo.versionCode == BuildConfig.VERSION_CODE) {
            Log.d(TAG, "App is up to date (version ${BuildConfig.VERSION_CODE})")
        }
    }

    private fun getCurrentVersionInfo(): AppVersionInfo {
        return try {
            if (versionFile.exists()) {
                val content = versionFile.readText()
                json.decodeFromString<AppVersionInfo>(content)
            } else {
                // First installation - create default version
                AppVersionInfo()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading version info, using default", e)
            AppVersionInfo()
        }
    }

    private fun saveVersionInfo(versionInfo: AppVersionInfo) {
        try {
            val content = json.encodeToString(versionInfo)
            versionFile.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving version info", e)
        }
    }

    private fun performMigration(fromVersion: Int, toVersion: Int) {
        // Here specific migrations for different versions can be implemented

        when {
            fromVersion < 1 && toVersion >= 1 -> {
                // Migration for version 1.0.0
                migrateToVersion1()
            }
            // Future migrations can be added here
            // fromVersion < 2 && toVersion >= 2 -> migrateToVersion2()
        }
    }

    private fun migrateToVersion1() {
        // This migration ensures that the QR codes file exists and is correctly formatted
        val qrCodesFile = File(context.filesDir, "qr_codes.json")

        if (!qrCodesFile.exists()) {
            // Create empty QR codes file for new installations
            qrCodesFile.writeText("[]")
            Log.i(TAG, "Created empty QR codes file for new installation")
        } else {
            // Validate and repair existing QR codes file
            try {
                val content = qrCodesFile.readText()
                // Try to parse to check validity
                Json.decodeFromString<List<Map<String, String>>>(content)
                Log.i(TAG, "QR codes file is valid")
            } catch (e: Exception) {
                Log.w(TAG, "QR codes file is corrupted, creating backup and resetting", e)
                // Create backup of the corrupted file
                val backupFile = File(context.filesDir, "qr_codes_backup_${System.currentTimeMillis()}.json")
                qrCodesFile.copyTo(backupFile)
                // Create new empty file
                qrCodesFile.writeText("[]")
            }
        }
    }

    fun getCurrentVersion(): String = BuildConfig.VERSION_NAME
    fun getCurrentVersionCode(): Int = BuildConfig.VERSION_CODE
}
