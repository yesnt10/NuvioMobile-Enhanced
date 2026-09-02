package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
import platform.darwin.dispatch_get_main_queue
import kotlin.math.roundToInt

@Composable
internal actual fun rememberPlayerDeviceStatus(): PlayerDeviceStatus {
    val device = UIDevice.currentDevice
    val previousBatteryMonitoring = remember {
        val previous = device.batteryMonitoringEnabled
        device.batteryMonitoringEnabled = true
        previous
    }
    var networkType by remember {
        mutableStateOf(PlayerDeviceNetworkType.Unknown)
    }
    val initialStatus = remember {
        readPlayerDeviceStatus(fallbackBatteryPercent = null)
    }
    var lastKnownBatteryPercent by remember {
        mutableStateOf(initialStatus.batteryPercent)
    }
    var status by remember {
        mutableStateOf(initialStatus)
    }

    fun refreshStatusFromDevice() {
        val nextStatus = readPlayerDeviceStatus(fallbackBatteryPercent = lastKnownBatteryPercent)
        nextStatus.batteryPercent?.let { percent ->
            lastKnownBatteryPercent = percent
        }
        status = nextStatus
    }

    DisposableEffect(Unit) {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            networkType = when {
                nw_path_get_status(path) != nw_path_status_satisfied -> PlayerDeviceNetworkType.Offline
                nw_path_uses_interface_type(path, nw_interface_type_wifi) -> PlayerDeviceNetworkType.Wifi
                nw_path_uses_interface_type(path, nw_interface_type_cellular) -> PlayerDeviceNetworkType.Cellular
                else -> PlayerDeviceNetworkType.Unknown
            }
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        onDispose {
            nw_path_monitor_cancel(monitor)
        }
    }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val levelObserver = center.addObserverForName(
            name = UIDeviceBatteryLevelDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            refreshStatusFromDevice()
        }
        val stateObserver = center.addObserverForName(
            name = UIDeviceBatteryStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            refreshStatusFromDevice()
        }
        val foregroundObserver = center.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            refreshStatusFromDevice()
        }
        onDispose {
            center.removeObserver(levelObserver)
            center.removeObserver(stateObserver)
            center.removeObserver(foregroundObserver)
            device.batteryMonitoringEnabled = previousBatteryMonitoring
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshStatusFromDevice()
            delay(PlayerDeviceStatusRefreshMs)
        }
    }

    return status.copy(networkType = networkType)
}

private fun readPlayerDeviceStatus(fallbackBatteryPercent: Int?): PlayerDeviceStatus {
    val device = UIDevice.currentDevice
    if (!device.batteryMonitoringEnabled) {
        device.batteryMonitoringEnabled = true
    }
    val uidDevicePercent = device.batteryLevel
        .takeIf { level -> level >= 0f }
        ?.let { level -> (level * 100f).roundToInt().coerceIn(0, 100) }
    val batteryState = device.batteryState.toString()

    return PlayerDeviceStatus(
        timeLabel = PlayerDeviceDateFormatter.formatter.stringFromDate(NSDate()),
        currentTimeMillis = (NSDate().timeIntervalSince1970 * 1_000.0).toLong(),
        batteryPercent = uidDevicePercent ?: fallbackBatteryPercent,
        batteryCharging = isIosBatteryCharging(batteryState),
        networkType = PlayerDeviceNetworkType.Unknown,
    )
}

private fun isIosBatteryCharging(state: String): Boolean =
    state == IosBatteryStateChargingValue ||
        state == IosBatteryStateFullValue ||
        state.endsWith("Charging") ||
        state.endsWith("Full")

private object PlayerDeviceDateFormatter {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }
}

private const val PlayerDeviceStatusRefreshMs = 1_000L
private const val IosBatteryStateChargingValue = "2"
private const val IosBatteryStateFullValue = "3"
