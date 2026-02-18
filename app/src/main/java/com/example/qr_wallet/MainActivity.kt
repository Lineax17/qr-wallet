package com.example.qr_wallet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.qr_wallet.data.local.CodesWriter
import com.example.qr_wallet.data.migration.AppMigration
import com.example.qr_wallet.data.model.QRCodeData
import com.example.qr_wallet.ui.components.QRWalletScreen
import com.example.qr_wallet.ui.theme.QrwalletTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/**
 * Main activity for the QR Wallet application.
 *
 * Handles app lifecycle, camera permissions, QR code scanning,
 * and coordinates data persistence with the UI layer.
 */
class MainActivity : ComponentActivity() {

    private val qrCodes = mutableStateListOf<QRCodeData>()
    private lateinit var codesWriter: CodesWriter
    private lateinit var appMigration: AppMigration

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else Log.d(TAG, "Camera permission denied")
    }

    private val scanQRCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            lifecycleScope.launch {
                val newCode = codesWriter.addQRCode(content, qrCodes.toList())
                if (qrCodes.none { it.content == newCode.content }) {
                    qrCodes.add(newCode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        codesWriter = CodesWriter(this)
        appMigration = AppMigration(this)
        appMigration.checkAndMigrateIfNeeded()

        enableEdgeToEdge()

        setContent {
            LaunchedEffect(Unit) {
                val savedCodes = codesWriter.loadQRCodes()
                qrCodes.clear()
                qrCodes.addAll(savedCodes)
            }

            QrwalletTheme {
                QRWalletScreen(
                    qrCodes = qrCodes,
                    onAddClick = ::checkCameraPermissionAndOpen,
                    onRenameCode = ::handleRename,
                    onDeleteCodes = ::handleDelete,
                    onReorderCodes = ::handleReorder,
                    appVersion = appMigration.getCurrentVersion()
                )
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val options = ScanOptions().apply {
            setPrompt("Scan QR Code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setBarcodeImageEnabled(true)
            setCaptureActivity(MyCaptureActivity::class.java)
        }
        scanQRCodeLauncher.launch(options)
    }

    private fun handleRename(id: String, newName: String) {
        lifecycleScope.launch {
            codesWriter.updateQRCodeName(id, newName, qrCodes.toList())
            val index = qrCodes.indexOfFirst { it.id == id }
            if (index != -1) {
                qrCodes[index] = qrCodes[index].copy(name = newName)
            }
        }
    }

    private fun handleDelete(idsToDelete: Set<String>) {
        lifecycleScope.launch {
            val updatedCodes = qrCodes.filter { it.id !in idsToDelete }
            codesWriter.saveQRCodes(updatedCodes)
            qrCodes.clear()
            qrCodes.addAll(updatedCodes)
        }
    }

    private fun handleReorder(fromIndex: Int, toIndex: Int) {
        lifecycleScope.launch {
            val newList = qrCodes.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            codesWriter.saveQRCodes(newList)
            qrCodes.clear()
            qrCodes.addAll(newList)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
