package com.suseoaa.locationspoofer.data.model

import kotlinx.serialization.Serializable

enum class CatchLogCategory {
    ALL,
    ORDER,
    LOCATION,
    SECURITY,
    ENV,
    SYSTEM
}

@Serializable
data class CatchLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "com.logistics.rider.hungerstation",
    val category: String = "LOCATION",
    val title: String = "",
    val detail: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val hitCount: Int = 1
)
