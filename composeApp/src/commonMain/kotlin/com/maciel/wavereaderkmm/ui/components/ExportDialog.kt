package com.maciel.wavereaderkmm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Export Dialog Component
 *
 * Reusable dialog for exporting data in CSV or JSON format.
 * Handles both "export all" and "export selected" scenarios.
 */
@Composable
fun ExportDialog(
    isVisible: Boolean,
    recordCount: Int,
    exportType: String,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = exportType,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Count: $recordCount ${if (recordCount == 1) "record" else "records"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Button(
                    onClick = {
                        onExportCsv()
                        onDismiss()
                    },
                    enabled = recordCount > 0
                ) {
                    Text("Export CSV")
                }

                Button(
                    onClick = {
                        onExportJson()
                        onDismiss()
                    },
                    enabled = recordCount > 0
                ) {
                    Text("Export JSON")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper composable that shows appropriate export dialog based on selection mode
 *
 * This is the main component you'll use in HistoryScreen.
 * It automatically determines whether to export all or selected records.
 */
@Composable
fun SmartExportDialog(
    showDialog: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    allVisibleCount: Int,
    onDismiss: () -> Unit,
    onExportSelectedCsv: () -> Unit,
    onExportSelectedJson: () -> Unit,
    onExportAllCsv: () -> Unit,
    onExportAllJson: () -> Unit
) {
    // Determine what we're exporting based on context
    val (recordCount, exportType, onCsv, onJson) = when {
        isSelectionMode && selectedCount > 0 -> {
            // Export selected records
            Quadruple(
                selectedCount,
                "Selected records",
                onExportSelectedCsv,
                onExportSelectedJson
            )
        }
        else -> {
            // Export all visible records
            Quadruple(
                allVisibleCount,
                "All visible records",
                onExportAllCsv,
                onExportAllJson
            )
        }
    }

    ExportDialog(
        isVisible = showDialog,
        recordCount = recordCount,
        exportType = exportType,
        onDismiss = onDismiss,
        onExportCsv = onCsv,
        onExportJson = onJson
    )
}

/**
 * Helper data class for the SmartExportDialog
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)