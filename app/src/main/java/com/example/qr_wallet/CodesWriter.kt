package com.example.qr_wallet

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException

@Serializable
data class QRCodeData(
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class QRCodeList(
    val codes: List<QRCodeData> = emptyList()
)

class CodesWriter(private val context: Context) {
    private val fileName = "qr_codes.json"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun getFile(): File {
        return File(context.filesDir, fileName)
    }

    suspend fun saveQRCodes(codes: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                val qrCodeDataList = codes.map { QRCodeData(it) }
                val qrCodeList = QRCodeList(qrCodeDataList)
                val jsonString = json.encodeToString(qrCodeList)
                getFile().writeText(jsonString)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadQRCodes(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile()
                if (!file.exists()) {
                    return@withContext emptyList<String>()
                }
                val jsonString = file.readText()
                val qrCodeList = json.decodeFromString<QRCodeList>(jsonString)
                qrCodeList.codes.map { it.content }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList<String>()
            }
        }
    }

    suspend fun addQRCode(code: String) {
        withContext(Dispatchers.IO) {
            try {
                val existingCodes = loadQRCodes().toMutableList()
                // Verhindere Duplikate
                if (!existingCodes.contains(code)) {
                    existingCodes.add(code)
                    saveQRCodes(existingCodes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
