package com.nuvio.app.features.player

internal data class PlayerWallClockSnapshot(
    val currentTime: String,
    val endTime: String,
)

internal expect object PlayerWallClock {
    fun snapshotForRemaining(remainingMs: Long): PlayerWallClockSnapshot
}
