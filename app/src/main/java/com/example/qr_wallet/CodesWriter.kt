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
    val id: String,
    val content: String,
    val name: String,
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

    suspend fun loadQRCodes(): List<QRCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile()
                if (!file.exists()) {
                    return@withContext emptyList<QRCodeData>()
                }
                val jsonString = file.readText()
                val qrCodeList = json.decodeFromString<QRCodeList>(jsonString)
                qrCodeList.codes
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList<QRCodeData>()
            }
        }
    }

    suspend fun addQRCode(content: String, existingCodes: List<QRCodeData>): QRCodeData {
        return withContext(Dispatchers.IO) {
            try {
                // Verhindere Duplikate
                val existing = existingCodes.find { it.content == content }
                if (existing != null) {
                    return@withContext existing
                }

                // Erstelle neue QR-Code-Daten mit automatischem Namen
                val newId = generateId()
                val newName = "QR Code ${existingCodes.size + 1}"
                val newCode = QRCodeData(
                    id = newId,
                    content = content,
                    name = newName
                )

                val updatedCodes = existingCodes.toMutableList()
                updatedCodes.add(newCode)
                saveQRCodes(updatedCodes)

                newCode
            } catch (e: Exception) {
                e.printStackTrace()
                QRCodeData("", content, "QR Code", System.currentTimeMillis())
            }
        }
    }

    suspend fun updateQRCodeName(id: String, newName: String, allCodes: List<QRCodeData>) {
        withContext(Dispatchers.IO) {
            try {
                val updatedCodes = allCodes.map { code ->
                    if (code.id == id) {
                        code.copy(name = newName)
                    } else {
                        code
                    }
                }
                saveQRCodes(updatedCodes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateId(): String {
        return "qr_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
