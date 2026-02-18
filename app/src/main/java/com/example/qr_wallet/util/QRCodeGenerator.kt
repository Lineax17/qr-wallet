package com.example.qr_wallet.util

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utility object for generating QR code bitmaps from content strings.
 */
object QRCodeGenerator {

    private const val TAG = "QRCodeGenerator"

    /**
     * Generates a QR code bitmap from the given content.
     *
     * @param content The text/URL to encode in the QR code
     * @param size The width and height of the generated bitmap in pixels
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    fun generate(content: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: WriterException) {
            Log.e(TAG, "Error generating QR code", e)
            null
        }
    }
}

