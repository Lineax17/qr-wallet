// domain/usecase/AddQRCodeUseCase.kt
package com.example.qr_wallet.domain.usecase

import com.example.qr_wallet.data.model.QRCode
import com.example.qr_wallet.data.repository.QRCodeRepo

class AddQRCodeUseCase(private val repository: QRCodeRepo) {
    operator fun invoke(qrCode: QRCode) {
        repository.addQRCode(qrCode)
    }
}