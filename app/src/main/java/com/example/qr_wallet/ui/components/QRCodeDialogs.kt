package com.example.qr_wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.qr_wallet.data.model.QRCodeData
import com.example.qr_wallet.util.QRCodeGenerator

/**
 * Fullscreen dialog displaying a QR code in large format.
 */
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
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrBitmap = remember(code.content) {
                    QRCodeGenerator.generate(code.content, 500)
                }
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full size QR Code for ${code.name}",
                        modifier = Modifier.size(500.dp)
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

/**
 * Confirmation dialog for deleting QR codes.
 */
@Composable
fun DeleteConfirmationDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete QR Codes") },
        text = { Text("Are you sure you want to delete $itemCount QR code(s)?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog showing app version and information.
 */
@Composable
fun VersionInfoDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

