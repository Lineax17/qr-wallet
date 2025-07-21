package com.example.qr_wallet

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.qr_wallet.ui.theme.QrwalletTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val qrCodes = mutableStateListOf<QRCodeData>()
    private lateinit var codesWriter: CodesWriter

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Log.d("Camera", "Berechtigung verweigert")
        }
    }

    private val scanQRCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            lifecycleScope.launch {
                val newCode = codesWriter.addQRCode(result.contents, qrCodes.toList())
                if (!qrCodes.any { it.content == newCode.content }) {
                    qrCodes.add(newCode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        codesWriter = CodesWriter(this)
        enableEdgeToEdge()
        setContent {
            // Lade gespeicherte QR-Codes beim Start
            LaunchedEffect(Unit) {
                val savedCodes = codesWriter.loadQRCodes()
                qrCodes.clear()
                qrCodes.addAll(savedCodes)
            }

            QrwalletTheme {
                QRWalletApp(
                    qrCodes = qrCodes,
                    onAddClick = { checkCameraPermissionAndOpen() },
                    onRenameCode = { id, newName ->
                        lifecycleScope.launch {
                            codesWriter.updateQRCodeName(id, newName, qrCodes.toList())
                            val index = qrCodes.indexOfFirst { it.id == id }
                            if (index != -1) {
                                qrCodes[index] = qrCodes[index].copy(name = newName)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val options = ScanOptions()
        options.setPrompt("QR-Code scannen")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)  // Orientierung sperren
        options.setBarcodeImageEnabled(true)
        options.setCaptureActivity(MyCaptureActivity::class.java)  // Benutzerdefinierte Activity verwenden
        scanQRCodeLauncher.launch(options)
    }
}

@Composable
fun QRWalletApp(
    qrCodes: List<QRCodeData>,
    onAddClick: () -> Unit,
    onRenameCode: (String, String) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "QR-Code hinzufügen"
                )
            }
        }
    ) { innerPadding ->
        if (qrCodes.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(qrCodes) { code ->
                    QRCodeItem(
                        code = code,
                        onRename = { id, newName -> onRenameCode(id, newName) }
                    )
                }
            }
        }
    }
}

@Composable
fun QRCodeItem(
    code: QRCodeData,
    onRename: (String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(code.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name ändern") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onRename(code.id, newName)
                                isEditing = false
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            newName = code.name // Reset bei Abbruch
                            isEditing = false
                            keyboardController?.hide()
                        }
                    ) {
                        Text("Abbrechen")
                    }
                } else {
                    Text(
                        text = code.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Namen bearbeiten"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showQRCode) {
                // QR-Code anzeigen
                val qrBitmap = remember(code.content) { generateQRCode(code.content) }
                qrBitmap?.let { bitmap ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR-Code für ${code.name}",
                                modifier = Modifier.size(280.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tippe den Button um den QR-Code zu verstecken",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Button(
                onClick = { showQRCode = !showQRCode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showQRCode) "QR-Code verstecken" else "QR-Code anzeigen")
            }
        }
    }
}

fun generateQRCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Noch keine QR-Codes gescannt. Drücke + um zu starten.")
    }
}