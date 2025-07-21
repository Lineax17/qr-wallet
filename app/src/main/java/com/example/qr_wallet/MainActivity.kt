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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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

/**
 * Main activity for the QR Wallet application.
 *
 * This activity serves as the entry point for the QR Wallet application, providing
 * a comprehensive interface for managing QR codes. The application allows users to:
 * - Scan QR codes using the device camera
 * - Store QR codes locally with custom names
 * - Display QR codes in both normal and fullscreen modes
 * - Edit QR code names inline
 * - Delete single or multiple QR codes
 * - Reorder QR codes in the list
 * - View app version information
 *
 * The activity follows MVVM-like patterns with Jetpack Compose for the UI layer
 * and maintains state using mutableStateListOf for reactive updates. All data
 * is stored locally using JSON file persistence through the CodesWriter class.
 *
 * Key features:
 * - Camera permission management for QR code scanning
 * - Portrait-oriented QR code scanning with custom capture activity
 * - Selection mode for bulk operations (delete, reorder)
 * - Automatic QR code naming with sequential numbering
 * - Material Design 3 UI with alternating list item backgrounds
 * - App migration support for version updates
 * - Comprehensive error handling and logging
 *
 * @author QR Wallet Development Team
 * @version 1.0
 * @since API level 24
 */
class MainActivity : ComponentActivity() {

    /**
     * Reactive list of QR codes displayed in the UI.
     * Uses mutableStateListOf for automatic recomposition when items are added,
     * removed, or modified. Each QRCodeData object contains:
     * - id: Unique identifier (UUID)
     * - content: The actual QR code data/URL
     * - name: User-friendly display name
     * - timestamp: Creation time in milliseconds
     */
    private val qrCodes = mutableStateListOf<QRCodeData>()

    /**
     * File-based persistence manager for QR codes.
     * Handles JSON serialization/deserialization and provides methods for
     * saving, loading, and updating QR code data. Thread-safe operations
     * are ensured through coroutines and proper context switching.
     */
    private lateinit var codesWriter: CodesWriter

    /**
     * App migration manager for handling version updates.
     * Responsible for detecting version changes and performing necessary
     * data migrations or updates. Ensures backward compatibility when
     * app structure or data formats change between versions.
     */
    private lateinit var appMigration: AppMigration

    /**
     * Activity result launcher for requesting camera permission.
     *
     * This launcher handles the runtime permission request for camera access,
     * which is required for QR code scanning functionality. Upon permission
     * grant, it automatically initiates the QR scanning process. If permission
     * is denied, the action is logged for debugging purposes.
     *
     * @see android.Manifest.permission.CAMERA
     */
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Log.d("Camera", "Permission denied")
        }
    }

    /**
     * Activity result launcher for QR code scanning.
     *
     * This launcher handles the QR code scanning process using the ZXing library.
     * When a QR code is successfully scanned, it automatically creates a new
     * QRCodeData entry with an auto-generated name and adds it to the list.
     * Duplicate detection is performed based on content to prevent redundant entries.
     *
     * The scanning process:
     * 1. Launches custom capture activity (MyCaptureActivity)
     * 2. Receives scanned content in result.contents
     * 3. Generates unique name based on existing QR codes count
     * 4. Creates new QRCodeData object with UUID
     * 5. Adds to reactive list if not duplicate
     *
     * @see com.journeyapps.barcodescanner.ScanContract
     * @see MyCaptureActivity
     */
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

    /**
     * Called when the activity is first created.
     *
     * This method performs the complete initialization sequence for the application:
     *
     * 1. **Dependency Initialization**: Creates CodesWriter and AppMigration instances
     *    with proper context binding for file system access.
     *
     * 2. **Migration Check**: Automatically detects if app has been updated and
     *    performs any necessary data migrations to maintain compatibility.
     *
     * 3. **UI Setup**: Enables edge-to-edge display and initializes Jetpack Compose
     *    UI with proper theming and state management.
     *
     * 4. **Data Loading**: Uses LaunchedEffect to asynchronously load saved QR codes
     *    from persistent storage on first composition.
     *
     * The method ensures proper error handling throughout the initialization process
     * and logs relevant information for debugging purposes.
     *
     * @param savedInstanceState Bundle containing activity's previously saved state,
     *                          or null if this is a fresh start
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize core dependencies with application context for proper lifecycle management
        codesWriter = CodesWriter(this)
        appMigration = AppMigration(this)

        // Perform version-based migration check to ensure data compatibility
        // This handles scenarios where app structure changes between versions
        appMigration.checkAndMigrateIfNeeded()

        // Enable modern Android UI with edge-to-edge display
        enableEdgeToEdge()

        setContent {
            // Asynchronously load persisted QR codes on initial composition
            // Uses LaunchedEffect to ensure this only runs once per activity lifecycle
            LaunchedEffect(Unit) {
                val savedCodes = codesWriter.loadQRCodes()
                qrCodes.clear()
                qrCodes.addAll(savedCodes)
            }

            // Apply consistent Material Design 3 theming throughout the app
            QrwalletTheme {
                QRWalletApp(
                    qrCodes = qrCodes,
                    onAddClick = { checkCameraPermissionAndOpen() },
                    onRenameCode = { id, newName ->
                        lifecycleScope.launch {
                            // Update both persistent storage and reactive UI state
                            codesWriter.updateQRCodeName(id, newName, qrCodes.toList())
                            val index = qrCodes.indexOfFirst { it.id == id }
                            if (index != -1) {
                                qrCodes[index] = qrCodes[index].copy(name = newName)
                            }
                        }
                    },
                    onDeleteCodes = { idsToDelete ->
                        lifecycleScope.launch {
                            // Remove from both storage and UI state atomically
                            val updatedCodes = qrCodes.filter { it.id !in idsToDelete }
                            codesWriter.saveQRCodes(updatedCodes)
                            qrCodes.clear()
                            qrCodes.addAll(updatedCodes)
                        }
                    },
                    onReorderCodes = { fromIndex, toIndex ->
                        lifecycleScope.launch {
                            // Perform list reordering and persist changes
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

    /**
     * Checks camera permission status and initiates QR code scanning.
     *
     * This method implements the Android 6.0+ runtime permission model for camera access.
     * It first checks if the CAMERA permission is already granted, and if so, immediately
     * opens the camera for QR scanning. If permission is not granted, it launches the
     * permission request flow through the registered ActivityResultLauncher.
     *
     * Permission flow:
     * 1. Check current permission status using ContextCompat.checkSelfPermission
     * 2. If granted: Immediately call openCamera()
     * 3. If not granted: Launch permission request dialog
     * 4. Handle result in requestCameraPermission callback
     *
     * This method is typically called when the user taps the floating action button
     * to add a new QR code via scanning.
     *
     * @see android.Manifest.permission.CAMERA
     * @see requestCameraPermission
     * @see openCamera
     */
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

    /**
     * Configures and launches the QR code scanning interface.
     *
     * This method creates a customized ScanOptions configuration optimized for
     * QR code scanning in portrait orientation. The configuration includes:
     *
     * **Scan Options Configuration:**
     * - Custom prompt text for user guidance
     * - Audio feedback (beep) on successful scan
     * - Portrait orientation lock for consistent UX
     * - Barcode image capture enabled for debugging
     * - Custom capture activity for enhanced control
     *
     * **Technical Details:**
     * - Uses ZXing library's ScanContract for lifecycle-aware scanning
     * - Employs MyCaptureActivity for consistent portrait behavior
     * - Results are handled by scanQRCodeLauncher callback
     * - Scanning session automatically terminates after successful capture
     *
     * The method ensures that the scanning interface follows Material Design
     * guidelines and provides clear visual feedback to users.
     *
     * @see ScanOptions
     * @see MyCaptureActivity
     * @see scanQRCodeLauncher
     */
    private fun openCamera() {
        val options = ScanOptions().apply {
            setPrompt("Scan QR Code")           // User-friendly instruction text
            setBeepEnabled(true)                // Audio feedback for accessibility
            setOrientationLocked(true)          // Maintain portrait orientation
            setBarcodeImageEnabled(true)        // Enable image capture for debugging
            setCaptureActivity(MyCaptureActivity::class.java)  // Custom capture implementation
        }
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
                            // Move buttons - only enabled when exactly one item is selected
                            IconButton(
                                onClick = {
                                    if (selectedCodes.size == 1) {
                                        val selectedId = selectedCodes.first()
                                        val index = qrCodes.indexOfFirst { it.id == selectedId }
                                        if (index > 0) {
                                            onReorderCodes(index, index - 1)
                                        }
                                    }
                                },
                                enabled = selectedCodes.size == 1 && qrCodes.indexOfFirst { it.id == selectedCodes.first() } > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move selected item up",
                                    tint = if (selectedCodes.size == 1 && qrCodes.indexOfFirst { it.id == selectedCodes.first() } > 0)
                                          MaterialTheme.colorScheme.onSurfaceVariant
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedCodes.size == 1) {
                                        val selectedId = selectedCodes.first()
                                        val index = qrCodes.indexOfFirst { it.id == selectedId }
                                        if (index < qrCodes.size - 1) {
                                            onReorderCodes(index, index + 1)
                                        }
                                    }
                                },
                                enabled = selectedCodes.size == 1 && qrCodes.indexOfFirst { it.id == selectedCodes.first() } < qrCodes.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move selected item down",
                                    tint = if (selectedCodes.size == 1 && qrCodes.indexOfFirst { it.id == selectedCodes.first() } < qrCodes.size - 1)
                                          MaterialTheme.colorScheme.onSurfaceVariant
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }

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
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(qrCodes, key = { _, code -> code.id }) { index, code ->
            val isDragging = draggedIndex == index

            QRCodeItem(
                code = code,
                index = index,
                totalItems = qrCodes.size,
                isSelectionMode = isSelectionMode,
                isSelected = code.id in selectedCodes,
                isAlternate = index % 2 == 1,
                onSelectionChange = { isSelected ->
                    onSelectionChange(
                        if (isSelected) {
                            selectedCodes + code.id
                        } else {
                            selectedCodes - code.id
                        }
                    )
                },
                onLongPress = {
                    if (!isSelectionMode) {
                        onLongPress(code.id)
                    }
                },
                onRename = onRename,
                onMoveUp = {
                    if (index > 0) {
                        onReorder(index, index - 1)
                    }
                },
                onMoveDown = {
                    if (index < qrCodes.size - 1) {
                        onReorder(index, index + 1)
                    }
                },
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeItem(
    code: QRCodeData,
    index: Int = 0, // New parameter for item index
    totalItems: Int = 1, // New parameter for total number of items
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isAlternate: Boolean = false, // New parameter for alternating background
    isDragging: Boolean = false, // New parameter to indicate dragging state
    dragOffset: Float = 0f, // New parameter for drag offset
    onSelectionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onRename: (String, String) -> Unit,
    onMoveUp: () -> Unit = {}, // New parameter for move up action
    onMoveDown: () -> Unit = {}, // New parameter for move down action
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    var showFullscreenQR by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(code.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Determine card color based on state - improved logic with dragging effect
    val cardColor = when {
        isDragging -> MaterialTheme.colorScheme.tertiaryContainer // Special color for dragging
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isAlternate -> MaterialTheme.colorScheme.surfaceContainer // Better visibility
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp) // Reduced padding for better visibility
            .offset(y = dragOffset.dp) // Apply drag offset
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
            defaultElevation = if (isDragging) 8.dp else if (isAlternate) 2.dp else 1.dp // Higher elevation when dragging
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

    // Fullscreen QR Code Dialog
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
                    .padding(16.dp), // Extra padding for better centering
                contentAlignment = Alignment.Center
            ) {
                val qrBitmap = remember(code.content) {
                    generateQRCode(code.content, 500) // Larger size for fullscreen display
                }
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full size QR Code for ${code.name}",
                        modifier = Modifier.size(500.dp) // Larger fullscreen display
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
