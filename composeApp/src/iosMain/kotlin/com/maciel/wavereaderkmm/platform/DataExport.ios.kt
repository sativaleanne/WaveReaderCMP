package com.maciel.wavereaderkmm.platform

import com.maciel.wavereaderkmm.model.HistoryRecord
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun timestamp(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyyMMdd_HHmmss"
    return formatter.stringFromDate(NSDate())
}

// ─────────────────────────────────────────────────────────────────────────────
// File writer
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private fun saveFile(bytes: ByteArray, fileName: String): NSURL {
    val tempDir = NSTemporaryDirectory()
    val filePath = tempDir + fileName

    val saved = bytes.usePinned { pinned ->
        val nsData = NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        nsData.writeToFile(filePath, atomically = true)
    }

    if (!saved) throw Exception("File write failed for $fileName")

    return NSURL.fileURLWithPath(filePath)
}

// ─────────────────────────────────────────────────────────────────────────────
// Share Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
fun shareFile(fileUrl: NSURL) {
    val activityVC = UIActivityViewController(
        activityItems = listOf(fileUrl),
        applicationActivities = null
    )

    @Suppress("DEPRECATION")
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return

    activityVC.popoverPresentationController?.apply {
        sourceView = rootVC.view
        sourceRect = rootVC.view.bounds.let {
            platform.CoreGraphics.CGRectMake(
                it.useContents { size.width / 2.0 },
                it.useContents { size.height / 2.0 },
                0.0,
                0.0
            )
        }
        permittedArrowDirections = platform.UIKit.UIPopoverArrowDirectionAny
    }

    rootVC.presentViewController(activityVC, animated = true, completion = null)
}

// ─────────────────────────────────────────────────────────────────────────────
// expect implementations
// ─────────────────────────────────────────────────────────────────────────────

actual suspend fun exportToCsv(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val csvContent = buildString {
            appendLine("Record ID,Timestamp,Location,Latitude,Longitude,Height,Period,Direction,Time")
            data.forEach { record ->
                val lat = record.lat?.toString() ?: ""
                val lon = record.lon?.toString() ?: ""
                val timestamp = record.timestamp.replace(",", ";")
                val location  = record.location.replace(",", ";")
                record.dataPoints.forEach { point ->
                    appendLine(
                        "${record.id},$timestamp,$location,$lat,$lon," +
                                "${point.height},${point.period},${point.direction},${point.time}"
                    )
                }
            }
        }

        // Write on IO thread — coroutine resumes on main after this returns
        val fileUrl = withContext(Dispatchers.IO) {
            saveFile(csvContent.encodeToByteArray(), "wave_export_${timestamp()}.csv")
        }

        // Back on main thread — safe to call UIKit
        shareFile(fileUrl)
        onSuccess("wave_export_${timestamp()}.csv")

    } catch (e: Exception) {
        onFailure("CSV export failed: ${e.message}")
    }
}

actual suspend fun exportToJson(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val jsonContent = buildString {
            appendLine("[")
            data.forEachIndexed { recordIndex, record ->
                record.dataPoints.forEachIndexed { pointIndex, point ->
                    appendLine("  {")
                    appendLine("    \"recordId\": \"${record.id}\",")
                    appendLine("    \"timestamp\": \"${record.timestamp}\",")
                    appendLine("    \"location\": \"${record.location}\",")
                    if (record.lat != null) appendLine("    \"lat\": ${record.lat},")
                    if (record.lon != null) appendLine("    \"lon\": ${record.lon},")
                    appendLine("    \"height\": ${point.height},")
                    appendLine("    \"period\": ${point.period},")
                    appendLine("    \"direction\": ${point.direction},")
                    appendLine("    \"time\": ${point.time}")
                    val isLast = recordIndex == data.lastIndex &&
                            pointIndex == record.dataPoints.lastIndex
                    appendLine(if (isLast) "  }" else "  },")
                }
            }
            appendLine("]")
        }

        // Write on IO thread — coroutine resumes on main after this returns
        val fileUrl = withContext(Dispatchers.IO) {
            saveFile(jsonContent.encodeToByteArray(), "wave_export_${timestamp()}.json")
        }

        // Back on main thread — safe to call UIKit
        shareFile(fileUrl)
        onSuccess("wave_export_${timestamp()}.json")

    } catch (e: Exception) {
        onFailure("JSON export failed: ${e.message}")
    }
}
