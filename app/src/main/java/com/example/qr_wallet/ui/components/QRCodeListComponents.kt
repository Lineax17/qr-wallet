package com.example.qr_wallet.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.qr_wallet.data.model.QRCodeData
import com.example.qr_wallet.util.QRCodeGenerator

/**
 * Scrollable list of QR code items with selection support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeList(
    qrCodes: List<QRCodeData>,
    isSelectionMode: Boolean,
    selectedCodes: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onLongPress: (String) -> Unit,
    onRename: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(qrCodes, key = { _, code -> code.id }) { index, code ->
            QRCodeItem(
                code = code,
                isSelectionMode = isSelectionMode,
                isSelected = code.id in selectedCodes,
                onSelectionChange = { isSelected ->
                    onSelectionChange(
                        if (isSelected) selectedCodes + code.id
                        else selectedCodes - code.id
                    )
                },
                onLongPress = {
                    if (!isSelectionMode) {
                        onLongPress(code.id)
                    }
                },
                onRename = onRename,
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 4.dp),
                isAlternate = index % 2 == 1
            )
        }
    }
}

/**
 * Individual QR code card with editing, display, and selection capabilities.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeItem(
    code: QRCodeData,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onRename: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isAlternate: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: Float = 0f
) {
    var isEditing by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    var showFullscreenQR by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(code.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val cardColor = when {
        isDragging -> MaterialTheme.colorScheme.tertiaryContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isAlternate -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .offset(y = dragOffset.dp)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectionChange(!isSelected)
                },
                onLongClick = {
                    if (!isSelectionMode) onLongPress()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else if (isAlternate) 2.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    EditingRow(
                        name = newName,
                        onNameChange = { newName = it },
                        onSave = {
                            if (newName.isNotBlank()) {
                                onRename(code.id, newName)
                                isEditing = false
                            }
                            keyboardController?.hide()
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    DisplayRow(
                        name = code.name,
                        isSelectionMode = isSelectionMode,
                        showQRCode = showQRCode,
                        onEditClick = {
                            newName = code.name
                            isEditing = true
                        },
                        onToggleQRCode = { showQRCode = !showQRCode },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (showQRCode && !isEditing && !isSelectionMode) {
                QRCodeDisplay(
                    content = code.content,
                    name = code.name,
                    onFullscreenClick = { showFullscreenQR = true }
                )
            }
        }
    }

    if (showFullscreenQR) {
        QRCodeFullscreenDialog(
            code = code,
            onDismiss = { showFullscreenQR = false }
        )
    }
}

@Composable
private fun EditingRow(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() })
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onSave) {
            Text("Save")
        }
    }
}

@Composable
private fun DisplayRow(
    name: String,
    isSelectionMode: Boolean,
    showQRCode: Boolean,
    onEditClick: () -> Unit,
    onToggleQRCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (!isSelectionMode) {
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename"
                    )
                }
                Button(onClick = onToggleQRCode) {
                    Text(if (showQRCode) "Hide" else "Show")
                }
            }
        }
    }
}

@Composable
private fun QRCodeDisplay(
    content: String,
    name: String,
    onFullscreenClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val qrBitmap = remember(content) {
            QRCodeGenerator.generate(content, 200)
        }
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code for $name",
                modifier = Modifier
                    .size(200.dp)
                    .clickable { onFullscreenClick() }
            )
        }
    }
}

/**
 * Empty state shown when no QR codes are stored.
 */
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

