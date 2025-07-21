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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.qr_wallet.data.migration.AppMigration
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
    private lateinit var appMigration: AppMigration

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Log.d("Camera", "Permission denied")
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
        appMigration = AppMigration(this)

        // Prüfe und führe Migration durch falls nötig
        appMigration.checkAndMigrateIfNeeded()

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
                    },
                    onDeleteCodes = { idsToDelete ->
                        lifecycleScope.launch {
                            val updatedCodes = qrCodes.filter { it.id !in idsToDelete }
                            codesWriter.saveQRCodes(updatedCodes)
                            qrCodes.clear()
                            qrCodes.addAll(updatedCodes)
                        }
                    },
                    onReorderCodes = { fromIndex, toIndex ->
                        lifecycleScope.launch {
                            val newList = qrCodes.toMutableList()
                            val item = newList.removeAt(fromIndex)
                            newList.add(toIndex, item)
                            codesWriter.saveQRCodes(newList)
                            qrCodes.clear()
                            qrCodes.addAll(newList)
                        }
                    },
                    appVersion = appMigration.getCurrentVersion()
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
        options.setPrompt("Scan QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setBarcodeImageEnabled(true)
        options.setCaptureActivity(MyCaptureActivity::class.java)
        scanQRCodeLauncher.launch(options)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRWalletApp(
    qrCodes: List<QRCodeData>,
    onAddClick: () -> Unit,
    onRenameCode: (String, String) -> Unit,
    onDeleteCodes: (Set<String>) -> Unit,
    onReorderCodes: (Int, Int) -> Unit,
    appVersion: String
) {
    var showVersionInfo by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCodes by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectionMode) "${selectedCodes.size} selected" else "QR Wallet")
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedCodes = setOf()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        if (selectedCodes.isNotEmpty()) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        }
                    } else {
                        IconButton(onClick = { showVersionInfo = true }) {
                            Icon(Icons.Default.Info, contentDescription = "App Info")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add QR Code"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (qrCodes.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            QRCodeList(
                qrCodes = qrCodes,
                isSelectionMode = isSelectionMode,
                selectedCodes = selectedCodes,
                onSelectionChange = { selectedCodes = it },
                onLongPress = {
                    isSelectionMode = true
                    selectedCodes = setOf(it)
                },
                onRename = onRenameCode,
                onReorder = onReorderCodes,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete QR Codes") },
                text = { Text("Are you sure you want to delete ${selectedCodes.size} QR code(s)?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteCodes(selectedCodes)
                        selectedCodes = setOf()
                        isSelectionMode = false
                        showDeleteDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Version Info Dialog
        if (showVersionInfo) {
            AlertDialog(
                onDismissRequest = { showVersionInfo = false },
                title = { Text("App Information") },
                text = {
                    Column {
                        Text("QR Wallet")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Version: $appVersion")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("A simple and secure QR code wallet for storing your QR codes locally.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All data is stored locally on your device.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVersionInfo = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeList(
    qrCodes: List<QRCodeData>,
    isSelectionMode: Boolean,
    selectedCodes: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onLongPress: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(qrCodes, key = { _, code -> code.id }) { index, code ->
            QRCodeItem(
                code = code,
                isSelectionMode = isSelectionMode,
                isSelected = code.id in selectedCodes,
                isAlternate = index % 2 == 1, // Alternierender Hintergrund
                onSelectionChange = { isSelected ->
                    onSelectionChange(
                        if (isSelected) {
                            selectedCodes + code.id
                        } else {
                            selectedCodes - code.id
                        }
                    )
                },
                onLongPress = { onLongPress(code.id) },
                onRename = onRename,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeItem(
    code: QRCodeData,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isAlternate: Boolean = false, // Neuer Parameter für alternierenden Hintergrund
    onSelectionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onRename: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    var showFullscreenQR by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(code.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Bestimme die Kartenfarbe basierend auf Zustand - verbesserte Logik
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isAlternate -> MaterialTheme.colorScheme.surfaceContainer // Bessere Sichtbarkeit
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp) // Reduziert Padding für bessere Sichtbarkeit
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectionChange(!isSelected)
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onLongPress()
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAlternate) 2.dp else 1.dp // Subtile Elevation-Unterschiede
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChange
                    )
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newName.isNotBlank()) {
                                    onRename(code.id, newName)
                                    isEditing = false
                                }
                                keyboardController?.hide()
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onRename(code.id, newName)
                                isEditing = false
                            }
                            keyboardController?.hide()
                        }
                    ) {
                        Text("Save")
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = code.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isSelectionMode) {
                        Row {
                            IconButton(onClick = {
                                newName = code.name
                                isEditing = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename"
                                )
                            }

                            Button(onClick = { showQRCode = !showQRCode }) {
                                Text(if (showQRCode) "Hide" else "Show")
                            }
                        }
                    }

                    if (isSelectionMode) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showQRCode && !isEditing && !isSelectionMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val qrBitmap = remember(code.content) {
                        generateQRCode(code.content, 300)
                    }
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code for ${code.name}",
                            modifier = Modifier
                                .size(300.dp)
                                .clickable {
                                    showFullscreenQR = true
                                }
                        )
                    }
                }
            }
        }
    }

    // Vollbild-QR-Code Dialog
    if (showFullscreenQR) {
        QRCodeFullscreenDialog(
            code = code,
            onDismiss = { showFullscreenQR = false }
        )
    }
}

@Composable
fun QRCodeFullscreenDialog(
    code: QRCodeData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = code.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Extra Padding für bessere Zentrierung
                contentAlignment = Alignment.Center
            ) {
                val qrBitmap = remember(code.content) {
                    generateQRCode(code.content, 400) // Deutlich größer: 600dp statt 400dp
                }
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full size QR Code for ${code.name}",
                        modifier = Modifier.size(400.dp) // Größere Vollbildanzeige
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No QR codes yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap the + button to scan your first QR code",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        Log.e("QRCode", "Error generating QR code", e)
        null
    }
}
