package com.example.qr_wallet.data.model

import kotlinx.serialization.Serializable

/**
 * Data model representing a stored QR code.
 *
 * @property id Unique identifier for the QR code
 * @property content The actual QR code content/URL
 * @property name User-friendly display name
 * @property timestamp Creation time in milliseconds
 */
@Serializable
data class QRCodeData(
    val id: String,
    val content: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Wrapper class for JSON serialization of QR code list.
 */
@Serializable
data class QRCodeList(
    val codes: List<QRCodeData> = emptyList()
)

