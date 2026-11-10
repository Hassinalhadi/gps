package com.suseoaa.locationspoofer.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.suseoaa.locationspoofer.data.model.CatchLogEntry
import java.util.concurrent.CopyOnWriteArrayList

class SpooferProvider : ContentProvider() {

    companion object {
        const val TARGET_PACKAGE = "com.logistics.rider.hungerstation"
        var isActive = false
        var latitude = 0.0      // GCJ-02 / WGS-84
        var longitude = 0.0     // GCJ-02 / WGS-84
        var wifiJson = "[]"
        var cellJson = "[]"
        var bluetoothJson = "[]"
        var simMode = "STILL"
        var simBearing = 0f
        var startTimestamp = 0L
        var routeJson = "[]"
        var isRouteMode = false
        var enableJitter = true
        var enableCatchLog = true

        private const val MAX_MEMORY_LOGS = 500
        val memoryCatchLogs = CopyOnWriteArrayList<CatchLogEntry>()

        fun addCatchLog(entry: CatchLogEntry) {
            if (entry.packageName.substringBefore(":") != TARGET_PACKAGE) return
            memoryCatchLogs.add(0, entry)
            while (memoryCatchLogs.size > MAX_MEMORY_LOGS) {
                try {
                    memoryCatchLogs.removeAt(memoryCatchLogs.lastIndex)
                } catch (_: Exception) {
                    break
                }
            }
        }

        fun clearMemoryLogs() {
            memoryCatchLogs.clear()
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        if (uri.path == "/logs") {
            val cursor = MatrixCursor(
                arrayOf("id", "timestamp", "packageName", "category", "title", "detail", "lat", "lng", "hitCount")
            )
            for (log in memoryCatchLogs) {
                cursor.addRow(
                    arrayOf<Any?>(
                        log.id,
                        log.timestamp,
                        log.packageName,
                        log.category,
                        log.title,
                        log.detail,
                        log.lat,
                        log.lng,
                        log.hitCount
                    )
                )
            }
            return cursor
        }

        val cursor = MatrixCursor(
            arrayOf(
                "active", "lat", "lng", "wifi_json", "cell_json", "bluetooth_json",
                "sim_mode", "sim_bearing", "start_timestamp",
                "route_json", "is_route_mode", "enable_catch_log"
            )
        )
        cursor.addRow(
            arrayOf(
                if (isActive) 1 else 0,
                latitude,
                longitude,
                wifiJson,
                cellJson,
                bluetoothJson,
                simMode,
                simBearing,
                startTimestamp,
                routeJson,
                if (isRouteMode) 1 else 0,
                if (enableCatchLog) 1 else 0
            )
        )
        return cursor
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values != null) {
            val pkg = values.getAsString("packageName") ?: TARGET_PACKAGE
            if (pkg.substringBefore(":") == TARGET_PACKAGE) {
                val entry = CatchLogEntry(
                    id = values.getAsLong("id") ?: System.currentTimeMillis(),
                    timestamp = values.getAsLong("timestamp") ?: System.currentTimeMillis(),
                    packageName = TARGET_PACKAGE,
                    category = values.getAsString("category") ?: "LOCATION",
                    title = values.getAsString("title") ?: "",
                    detail = values.getAsString("detail") ?: "",
                    lat = values.getAsDouble("lat"),
                    lng = values.getAsDouble("lng"),
                    hitCount = values.getAsInteger("hitCount") ?: 1
                )
                addCatchLog(entry)
            }
        }
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        clearMemoryLogs()
        return 1
    }

    override fun update(
        uri: Uri, values: ContentValues?,
        selection: String?, selectionArgs: Array<out String>?
    ): Int = 0
}
