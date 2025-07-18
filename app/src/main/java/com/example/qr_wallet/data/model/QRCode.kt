package com.example.qr_wallet.data.model

import java.util.UUID

data class QRCode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String
)
