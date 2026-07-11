package com.nuvio.app.features.player

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle

internal actual object PlayerWallClock {
    actual fun snapshotForRemaining(remainingMs: Long): PlayerWallClockSnapshot {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterNoStyle
            timeStyle = NSDateFormatterShortStyle
        }
        val now = NSDate()
        val end = NSDate(timeIntervalSinceNow = remainingMs.coerceAtLeast(0L).toDouble() / 1000.0)
        return PlayerWallClockSnapshot(
            currentTime = formatter.stringFromDate(now),
            endTime = formatter.stringFromDate(end),
        )
    }
}
