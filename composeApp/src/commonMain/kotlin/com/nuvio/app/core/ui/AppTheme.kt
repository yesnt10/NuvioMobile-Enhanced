package com.nuvio.app.core.ui

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_amber
import nuvio.composeapp.generated.resources.theme_amethyst
import nuvio.composeapp.generated.resources.theme_blossom
import nuvio.composeapp.generated.resources.theme_custom
import nuvio.composeapp.generated.resources.theme_crimson
import nuvio.composeapp.generated.resources.theme_emerald
import nuvio.composeapp.generated.resources.theme_ocean
import nuvio.composeapp.generated.resources.theme_lagoon
import nuvio.composeapp.generated.resources.theme_messenger
import nuvio.composeapp.generated.resources.theme_rose
import nuvio.composeapp.generated.resources.theme_violet
import nuvio.composeapp.generated.resources.theme_white
import nuvio.composeapp.generated.resources.theme_sunset
import org.jetbrains.compose.resources.StringResource

enum class AppTheme {
    GOLD,
    JADE,
    ROSE_GOLD,
    ARCTIC_BLUE,
    GRAPHITE,
    CRIMSON,
    OCEAN,
    VIOLET,
    EMERALD,
    AMBER,
    ROSE,
    MESSENGER,
    AMETHYST,
    BLOSSOM,
    LAGOON,
    SUNSET,
    CUSTOM,
    WHITE,
}

val AppTheme.isEnhanced: Boolean
    get() = this == AppTheme.MESSENGER ||
        this == AppTheme.AMETHYST ||
        this == AppTheme.BLOSSOM ||
        this == AppTheme.LAGOON ||
        this == AppTheme.SUNSET ||
        this == AppTheme.CUSTOM

val AppTheme.labelRes: StringResource
    get() = when (this) {
        AppTheme.GOLD -> Res.string.theme_gold
        AppTheme.JADE -> Res.string.theme_jade
        AppTheme.ROSE_GOLD -> Res.string.theme_rose_gold
        AppTheme.ARCTIC_BLUE -> Res.string.theme_arctic_blue
        AppTheme.GRAPHITE -> Res.string.theme_graphite
        AppTheme.CRIMSON -> Res.string.theme_crimson
        AppTheme.OCEAN -> Res.string.theme_ocean
        AppTheme.VIOLET -> Res.string.theme_violet
        AppTheme.EMERALD -> Res.string.theme_emerald
        AppTheme.AMBER -> Res.string.theme_amber
        AppTheme.ROSE -> Res.string.theme_rose
        AppTheme.MESSENGER -> Res.string.theme_messenger
        AppTheme.AMETHYST -> Res.string.theme_amethyst
        AppTheme.BLOSSOM -> Res.string.theme_blossom
        AppTheme.LAGOON -> Res.string.theme_lagoon
        AppTheme.SUNSET -> Res.string.theme_sunset
        AppTheme.CUSTOM -> Res.string.theme_custom
        AppTheme.WHITE -> Res.string.theme_white
    }
