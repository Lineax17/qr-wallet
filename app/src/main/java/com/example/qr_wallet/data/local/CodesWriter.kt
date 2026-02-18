package com.example.qr_wallet.data.local

import android.content.Context
import com.example.qr_wallet.data.model.QRCodeData
import com.example.qr_wallet.data.model.QRCodeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Handles local file-based persistence for QR codes using JSON serialization.
 * All I/O operations are performed on Dispatchers.IO for thread safety.
 */
class CodesWriter(private val context: Context) {

    private val fileName = "qr_codes.json"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun getFile(): File = File(context.filesDir, fileName)

    /**
     * Saves the list of QR codes to local storage.
     */
    suspend fun saveQRCodes(codes: List<QRCodeData>) {
        withContext(Dispatchers.IO) {
            try {
                val qrCodeList = QRCodeList(codes)
                val jsonString = json.encodeToString(qrCodeList)
                getFile().writeText(jsonString)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Loads all QR codes from local storage.
     */
    suspend fun loadQRCodes(): List<QRCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile()
                if (!file.exists()) return@withContext emptyList()

                val jsonString = file.readText()
                val qrCodeList = json.decodeFromString<QRCodeList>(jsonString)
                qrCodeList.codes
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Adds a new QR code with automatic name generation.
     * Returns existing code if content already exists (duplicate prevention).
     */
    suspend fun addQRCode(content: String, existingCodes: List<QRCodeData>): QRCodeData {
        return withContext(Dispatchers.IO) {
            try {
                // Prevent duplicates
                existingCodes.find { it.content == content }?.let { return@withContext it }

                val newCode = QRCodeData(
                    id = generateId(),
                    content = content,
                    name = "QR Code ${existingCodes.size + 1}"
                )

                saveQRCodes(existingCodes + newCode)
                newCode
            } catch (e: Exception) {
                e.printStackTrace()
                QRCodeData("", content, "QR Code", System.currentTimeMillis())
            }
        }
    }

    /**
     * Updates the name of a QR code by ID.
     */
    suspend fun updateQRCodeName(id: String, newName: String, allCodes: List<QRCodeData>) {
        withContext(Dispatchers.IO) {
            try {
                val updatedCodes = allCodes.map { code ->
                    if (code.id == id) code.copy(name = newName) else code
                }
                saveQRCodes(updatedCodes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateId(): String = "qr_${System.currentTimeMillis()}_${(1000..9999).random()}"
}

