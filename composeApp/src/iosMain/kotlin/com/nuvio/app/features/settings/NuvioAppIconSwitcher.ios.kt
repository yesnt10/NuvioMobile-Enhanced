package com.nuvio.app.features.settings

internal actual object NuvioAppIconSwitcher {
    actual fun apply(iconId: String): Boolean {
        return false
    }

    actual fun closeAfterApply() = Unit
}
