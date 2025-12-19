package com.maciel.wavereaderkmm.platform

import com.maciel.wavereaderkmm.model.HistoryRecord
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

private fun timestamp(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyyMMdd_HHmmss"
    return formatter.stringFromDate(NSDate())
}

@OptIn(ExperimentalForeignApi::class)
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
                val location = record.location.replace(",", ";")

                record.dataPoints.forEach { point ->
                    appendLine("${record.id},$timestamp,$location,$lat,$lon,${point.height},${point.period},${point.direction},${point.time}")
                }
            }
        }

        // Save to Documents directory
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String

        val fileName = "wave_export_${timestamp()}.csv"
        val filePath = "$documentsPath/$fileName"

        NSString.create(string = csvContent).writeToFile(
            filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )

        onSuccess("Exported: $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}

@OptIn(ExperimentalForeignApi::class)
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

                    val isLast = (recordIndex == data.lastIndex && pointIndex == record.dataPoints.lastIndex)
                    appendLine(if (isLast) "  }" else "  },")
                }
            }
            appendLine("]")
        }

        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String

        val fileName = "wave_export_${timestamp()}.json"
        val filePath = "$documentsPath/$fileName"

        NSString.create(string = jsonContent).writeToFile(
            filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )

        onSuccess("Exported: $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}