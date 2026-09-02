package com.nuvio.app.core.sync

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification

internal actual object AppForegroundMonitor {
    actual fun events(): Flow<Unit> = callbackFlow {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            trySend(Unit)
        }

        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
}
