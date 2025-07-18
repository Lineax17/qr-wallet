// domain/usecase/GetQRCodesUseCase.kt
package com.example.qr_wallet.domain.usecase

import com.example.qr_wallet.data.model.QRCode
import com.example.qr_wallet.data.repository.QRCodeRepo
import kotlinx.coroutines.flow.Flow

class GetQRCodesUseCase(private val repository: QRCodeRepo) {
    operator fun invoke(): Flow<List<QRCode>> {
        return repository.qrCodes
    }
}