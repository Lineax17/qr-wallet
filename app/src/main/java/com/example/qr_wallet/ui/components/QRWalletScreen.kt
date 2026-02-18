package com.example.qr_wallet.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.qr_wallet.data.model.QRCodeData

/**
 * Main screen composable containing the app's primary UI structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRWalletScreen(
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
            QRWalletTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCodes.size,
                qrCodes = qrCodes,
                selectedCodes = selectedCodes,
                onCancelSelection = {
                    isSelectionMode = false
                    selectedCodes = setOf()
                },
                onMoveUp = {
                    if (selectedCodes.size == 1) {
                        val selectedId = selectedCodes.first()
                        val index = qrCodes.indexOfFirst { it.id == selectedId }
                        if (index > 0) onReorderCodes(index, index - 1)
                    }
                },
                onMoveDown = {
                    if (selectedCodes.size == 1) {
                        val selectedId = selectedCodes.first()
                        val index = qrCodes.indexOfFirst { it.id == selectedId }
                        if (index < qrCodes.size - 1) onReorderCodes(index, index + 1)
                    }
                },
                onDeleteClick = { showDeleteDialog = true },
                onInfoClick = { showVersionInfo = true }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add QR Code")
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
                modifier = Modifier.padding(innerPadding)
            )
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                itemCount = selectedCodes.size,
                onConfirm = {
                    onDeleteCodes(selectedCodes)
                    selectedCodes = setOf()
                    isSelectionMode = false
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        if (showVersionInfo) {
            VersionInfoDialog(
                appVersion = appVersion,
                onDismiss = { showVersionInfo = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QRWalletTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    qrCodes: List<QRCodeData>,
    selectedCodes: Set<String>,
    onCancelSelection: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeleteClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val canMoveUp = selectedCodes.size == 1 &&
            qrCodes.indexOfFirst { it.id == selectedCodes.first() } > 0
    val canMoveDown = selectedCodes.size == 1 &&
            qrCodes.indexOfFirst { it.id == selectedCodes.first() } < qrCodes.size - 1

    TopAppBar(
        title = { Text(if (isSelectionMode) "$selectedCount selected" else "QR Wallet") },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCancelSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                }
            }
        },
        actions = {
            if (isSelectionMode && selectedCodes.isNotEmpty()) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                }
            } else if (!isSelectionMode) {
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "App Info")
                }
            }
        }
    )
}

