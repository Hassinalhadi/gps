package com.suseoaa.locationspoofer.data.repository

import android.content.Context
import android.content.Intent
import com.suseoaa.locationspoofer.data.model.CatchLogEntry
import com.suseoaa.locationspoofer.data.model.RoutePoint
import com.suseoaa.locationspoofer.provider.SpooferProvider
import com.suseoaa.locationspoofer.service.SpoofingService
import com.suseoaa.locationspoofer.utils.ConfigManager
import com.suseoaa.locationspoofer.utils.LSPosedManager
import com.suseoaa.locationspoofer.utils.RootManager
import org.json.JSONArray
import org.json.JSONObject

class LocationRepository(
    private val configManager: ConfigManager,
    private val rootManager: RootManager,
    private val lsposedManager: LSPosedManager,
    private val settingsManager: com.suseoaa.locationspoofer.utils.SettingsManager,
    private val savedRouteDao: com.suseoaa.locationspoofer.data.db.SavedRouteDao
) {
    suspend fun checkRootAccess(): Boolean = rootManager.checkRootAccess()

    fun isModuleActive(): Boolean = lsposedManager.isModuleActive()

    suspend fun initCatchLogFile(): Boolean = rootManager.initCatchLogFile()

    suspend fun readCatchLogs(): List<CatchLogEntry> {
        val raw = rootManager.readCatchLogsRaw()
        val map = LinkedHashMap<Long, CatchLogEntry>()
        for (mem in SpooferProvider.memoryCatchLogs) {
            if (mem.packageName.substringBefore(":") == SpooferProvider.TARGET_PACKAGE) {
                map[mem.id] = mem
            }
        }
        if (raw.isNotBlank()) {
            raw.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        val obj = JSONObject(trimmed)
                        val pkg = obj.optString("packageName", SpooferProvider.TARGET_PACKAGE)
                        if (pkg.substringBefore(":") == SpooferProvider.TARGET_PACKAGE) {
                            val id = obj.optLong("id", obj.optLong("timestamp", System.currentTimeMillis()))
                            val entry = CatchLogEntry(
                                id = id,
                                timestamp = obj.optLong("timestamp", id),
                                packageName = SpooferProvider.TARGET_PACKAGE,
                                category = obj.optString("category", "LOCATION"),
                                title = obj.optString("title", ""),
                                detail = obj.optString("detail", ""),
                                lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null,
                                lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null,
                                hitCount = obj.optInt("hitCount", 1)
                            )
                            val existing = map[id]
                            if (existing == null || entry.hitCount >= existing.hitCount) {
                                map[id] = entry
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return map.values.sortedByDescending { it.timestamp }
    }

    suspend fun clearCatchLogs() {
        SpooferProvider.clearMemoryLogs()
        rootManager.clearCatchLogs()
    }

    suspend fun startSpoofing(
        context: Context,
        lat: Double,
        lng: Double,
        simMode: String,
        simBearing: Float,
        startTime: Long,
        routePoints: List<RoutePoint>,
        isRouteMode: Boolean,
        appCoordinateSystems: Map<String, String>,
        wifiJson: String = "[]",
        cellJson: String = "[]",
        bluetoothJson: String = "[]",
        mockWifi: Boolean = true,
        mockCell: Boolean = true,
        mockBluetooth: Boolean = true,
        enableJitter: Boolean = true,
        enableCatchLog: Boolean = settingsManager.enableCatchLog
    ) {
        SpooferProvider.isActive = true
        SpooferProvider.latitude = lat
        SpooferProvider.longitude = lng
        SpooferProvider.startTimestamp = startTime
        SpooferProvider.simMode = simMode
        SpooferProvider.simBearing = simBearing
        SpooferProvider.wifiJson = wifiJson
        SpooferProvider.cellJson = cellJson
        SpooferProvider.bluetoothJson = bluetoothJson
        SpooferProvider.routeJson = routePointsToJson(routePoints)
        SpooferProvider.isRouteMode = isRouteMode
        SpooferProvider.enableJitter = enableJitter
        SpooferProvider.enableCatchLog = enableCatchLog

        val alt = settingsManager.altitude.toDoubleOrNull() ?: 0.0
        val satCount = settingsManager.satelliteCount.toIntOrNull() ?: 20
        configManager.saveConfig(
            lat, lng, true, simMode, simBearing, startTime, routePoints, isRouteMode,
            SpooferProvider.wifiJson, appCoordinateSystems, SpooferProvider.cellJson,
            SpooferProvider.bluetoothJson, mockWifi, mockCell, mockBluetooth, enableJitter,
            alt, satCount, enableCatchLog
        )
        rootManager.grantMockLocation()

        context.startForegroundService(
            Intent(context, SpoofingService::class.java).apply {
                action = SpoofingService.ACTION_START
                putExtra(SpoofingService.EXTRA_LAT, lat)
                putExtra(SpoofingService.EXTRA_LNG, lng)
            }
        )
    }

    suspend fun stopSpoofing(context: Context) {
        SpooferProvider.isActive = false
        SpooferProvider.wifiJson = "[]"
        SpooferProvider.cellJson = "[]"
        SpooferProvider.bluetoothJson = "[]"
        SpooferProvider.routeJson = "[]"
        SpooferProvider.isRouteMode = false
        configManager.saveConfig(0.0, 0.0, false, enableCatchLog = settingsManager.enableCatchLog)
        context.startService(Intent(context, SpoofingService::class.java).apply {
            action = SpoofingService.ACTION_STOP
        })
        rootManager.revokeMockLocation()
    }

    suspend fun updateConfig(
        lat: Double,
        lng: Double,
        simMode: String,
        simBearing: Float,
        startTime: Long,
        routePoints: List<RoutePoint>,
        isRouteMode: Boolean,
        appCoordinateSystems: Map<String, String>,
        wifiJson: String = SpooferProvider.wifiJson,
        cellJson: String = SpooferProvider.cellJson,
        bluetoothJson: String = SpooferProvider.bluetoothJson,
        mockWifi: Boolean = true,
        mockCell: Boolean = true,
        mockBluetooth: Boolean = true,
        enableJitter: Boolean = true,
        enableCatchLog: Boolean = settingsManager.enableCatchLog
    ) {
        SpooferProvider.latitude = lat
        SpooferProvider.longitude = lng
        SpooferProvider.startTimestamp = startTime
        SpooferProvider.simMode = simMode
        SpooferProvider.simBearing = simBearing
        SpooferProvider.routeJson = routePointsToJson(routePoints)
        SpooferProvider.isRouteMode = isRouteMode
        SpooferProvider.wifiJson = wifiJson
        SpooferProvider.cellJson = cellJson
        SpooferProvider.bluetoothJson = bluetoothJson
        SpooferProvider.enableJitter = enableJitter
        SpooferProvider.enableCatchLog = enableCatchLog
        val alt = settingsManager.altitude.toDoubleOrNull() ?: 0.0
        val satCount = settingsManager.satelliteCount.toIntOrNull() ?: 20
        configManager.saveConfig(
            lat, lng, true, simMode, simBearing, startTime, routePoints, isRouteMode,
            SpooferProvider.wifiJson, appCoordinateSystems, SpooferProvider.cellJson,
            SpooferProvider.bluetoothJson, mockWifi, mockCell, mockBluetooth, enableJitter,
            alt, satCount, enableCatchLog
        )
    }

    suspend fun updateWifiJson(wifiJson: String, appCoordinateSystems: Map<String, String>) {
        SpooferProvider.wifiJson = wifiJson
        configManager.saveConfig(
            SpooferProvider.latitude,
            SpooferProvider.longitude,
            SpooferProvider.isActive,
            SpooferProvider.simMode,
            SpooferProvider.simBearing,
            startTimestamp = SpooferProvider.startTimestamp,
            wifiJson = wifiJson,
            appCoordinateSystems = appCoordinateSystems,
            cellJson = SpooferProvider.cellJson,
            bluetoothJson = SpooferProvider.bluetoothJson,
            altitude = settingsManager.altitude.toDoubleOrNull() ?: 0.0,
            satelliteCount = settingsManager.satelliteCount.toIntOrNull() ?: 20,
            enableCatchLog = settingsManager.enableCatchLog
        )
    }

    private fun routePointsToJson(points: List<RoutePoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(JSONObject().apply {
                put("lat", p.lat)
                put("lng", p.lng)
            })
        }
        return arr.toString()
    }

    fun getSavedRoutes(): kotlinx.coroutines.flow.Flow<List<com.suseoaa.locationspoofer.data.db.SavedRouteEntity>> {
        return savedRouteDao.getAllSavedRoutes()
    }

    suspend fun insertSavedRoute(name: String, points: List<RoutePoint>) {
        val pointsJson = routePointsToJson(points)
        savedRouteDao.insertSavedRoute(
            com.suseoaa.locationspoofer.data.db.SavedRouteEntity(
                name = name,
                pointsJson = pointsJson
            )
        )
    }

    suspend fun deleteSavedRoute(route: com.suseoaa.locationspoofer.data.db.SavedRouteEntity) {
        savedRouteDao.deleteSavedRoute(route)
    }
}
