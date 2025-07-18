package com.example.qr_wallet.data.repository

import com.example.qr_wallet.data.model.QRCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class QRCodeRepo {
    private val _qrCodes = MutableStateFlow<List<QRCode>>(emptyList())
    val qrCodes: Flow<List<QRCode>> = _qrCodes.asStateFlow()

    fun addQRCode(qrCode: QRCode) {
        _qrCodes.value = _qrCodes.value + qrCode
    }

    fun renameQRCode(id: String, newName: String) {
        _qrCodes.value = _qrCodes.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
    }
}