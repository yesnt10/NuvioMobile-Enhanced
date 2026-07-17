package com.nuvio.app.core.build

object AppFeaturePolicy {
    val pluginsEnabled: Boolean = true
    val supportersContributorsPageEnabled: Boolean = true
    val inAppUpdaterEnabled: Boolean = true
    val personalMediaAddonCopyEnabled: Boolean = true
    val heroTrailerPlaybackSupported: Boolean = true
    val imdbRatingLogoEnabled: Boolean = true
    val p2pEnabled: Boolean = false
    val accountDeletionEnabled: Boolean = true
    val trailerPlaybackMode = TrailerPlaybackMode.IN_APP
}

enum class TrailerPlaybackMode {
    IN_APP, EXTERNAL
}
