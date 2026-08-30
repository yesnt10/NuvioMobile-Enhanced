package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_color_blue
import nuvio.composeapp.generated.resources.theme_color_cyan
import nuvio.composeapp.generated.resources.theme_color_green
import nuvio.composeapp.generated.resources.theme_color_pink
import nuvio.composeapp.generated.resources.theme_color_purple
import nuvio.composeapp.generated.resources.theme_color_red
import nuvio.composeapp.generated.resources.theme_color_white
import nuvio.composeapp.generated.resources.theme_color_yellow
import org.jetbrains.compose.resources.StringResource

enum class ThemeAccentColor(
    val color: Color,
    val labelRes: StringResource,
) {
    PINK(Color(0xFFFF5F9E), Res.string.theme_color_pink),
    PURPLE(Color(0xFF9B5CFF), Res.string.theme_color_purple),
    BLUE(Color(0xFF397CFF), Res.string.theme_color_blue),
    CYAN(Color(0xFF35D6E8), Res.string.theme_color_cyan),
    GREEN(Color(0xFF57D67A), Res.string.theme_color_green),
    YELLOW(Color(0xFFFFD447), Res.string.theme_color_yellow),
    RED(Color(0xFFFF5263), Res.string.theme_color_red),
    WHITE(Color(0xFFF4F7FF), Res.string.theme_color_white),
}

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

object ThemeColors {

    val Gold = ThemeColorPalette(
        secondary = Color(0xFFE8A91C),
        secondaryVariant = Color(0xFF9A6200),
        accentGradient = listOf(
            Color(0xFF8A5700),
            Color(0xFFE8A91C),
            Color(0xFFFFF1A8),
            Color(0xFFFFD45C),
            Color(0xFF9A6200),
        ),
        nativeAccentHex = "#FFD45C",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFD45C),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0E0B),
        backgroundElevated = Color(0xFF1D1A14),
        backgroundCard = Color(0xFF262116),
    )

    val Jade = ThemeColorPalette(
        secondary = Color(0xFF22D37C),
        secondaryVariant = Color(0xFF0BBF9A),
        accentGradient = listOf(Color(0xFF7BF08D), Color(0xFF22D37C), Color(0xFF0BBF9A)),
        nativeAccentHex = "#7BF08D",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFF7BF08D),
        focusBackground = Color(0xFF153A2C),
        background = Color(0xFF0B0F0D),
        backgroundElevated = Color(0xFF141D18),
        backgroundCard = Color(0xFF16251D),
    )

    val RoseGold = ThemeColorPalette(
        secondary = Color(0xFFEC70A9),
        secondaryVariant = Color(0xFFB75AFF),
        accentGradient = listOf(Color(0xFFB75AFF), Color(0xFFEC70A9), Color(0xFFFFB37A)),
        nativeAccentHex = "#FFB37A",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFB37A),
        focusBackground = Color(0xFF442037),
        background = Color(0xFF100C0F),
        backgroundElevated = Color(0xFF1F161D),
        backgroundCard = Color(0xFF281A24),
    )

    val ArcticBlue = ThemeColorPalette(
        secondary = Color(0xFF3185F5),
        secondaryVariant = Color(0xFF4D55E8),
        accentGradient = listOf(Color(0xFF4DE3FF), Color(0xFF3185F5), Color(0xFF4D55E8)),
        nativeAccentHex = "#4DE3FF",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF4DE3FF),
        focusBackground = Color(0xFF172844),
        background = Color(0xFF0B0E14),
        backgroundElevated = Color(0xFF141A24),
        backgroundCard = Color(0xFF161E2A),
    )

    val Graphite = ThemeColorPalette(
        secondary = Color(0xFFAAB2BE),
        secondaryVariant = Color(0xFF687381),
        accentGradient = listOf(Color(0xFFF3F5F7), Color(0xFFAAB2BE), Color(0xFF687381)),
        nativeAccentHex = "#F3F5F7",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFFFFFFFF),
        focusRing = Color(0xFFF3F5F7),
        focusBackground = Color(0xFF30343A),
        background = Color(0xFF0C0D0F),
        backgroundElevated = Color(0xFF17191D),
        backgroundCard = Color(0xFF20242A),
    )

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFE53935),
        secondaryVariant = Color(0xFFC62828),
        nativeAccentHex = "#E53935",
        focusRing = Color(0xFFFF5252),
        focusBackground = Color(0xFF3D1A1A),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1A),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF1E88E5),
        secondaryVariant = Color(0xFF1565C0),
        nativeAccentHex = "#1E88E5",
        focusRing = Color(0xFF42A5F5),
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFF8E24AA),
        secondaryVariant = Color(0xFF6A1B9A),
        nativeAccentHex = "#8E24AA",
        focusRing = Color(0xFFAB47BC),
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF43A047),
        secondaryVariant = Color(0xFF2E7D32),
        nativeAccentHex = "#43A047",
        focusRing = Color(0xFF66BB6A),
        focusBackground = Color(0xFF1A3D1E),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF1A241A),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFB8C00),
        secondaryVariant = Color(0xFFEF6C00),
        nativeAccentHex = "#FB8C00",
        focusRing = Color(0xFFFFA726),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFD81B60),
        secondaryVariant = Color(0xFFC2185B),
        nativeAccentHex = "#D81B60",
        focusRing = Color(0xFFEC407A),
        focusBackground = Color(0xFF3D1A2D),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1F),
    )

    val Messenger = ThemeColorPalette(
        secondary = Color(0xFF168AFF),
        secondaryVariant = Color(0xFF0072FF),
        nativeAccentHex = "#168AFF",
        focusRing = Color(0xFF38BDF8),
        focusBackground = Color(0xFF132B45),
        background = Color(0xFF090E15),
        backgroundElevated = Color(0xFF131A23),
        backgroundCard = Color(0xFF192432),
    )

    val Amethyst = ThemeColorPalette(
        secondary = Color(0xFF8B5CF6),
        secondaryVariant = Color(0xFF7C3AED),
        nativeAccentHex = "#8B5CF6",
        focusRing = Color(0xFFA855F7),
        focusBackground = Color(0xFF281A3D),
        background = Color(0xFF0F0B14),
        backgroundElevated = Color(0xFF1B1522),
        backgroundCard = Color(0xFF251B2F),
    )

    val Blossom = ThemeColorPalette(
        secondary = Color(0xFFEC4899),
        secondaryVariant = Color(0xFFDB2777),
        nativeAccentHex = "#EC4899",
        focusRing = Color(0xFFF472B6),
        focusBackground = Color(0xFF3D1830),
        background = Color(0xFF120A10),
        backgroundElevated = Color(0xFF21141D),
        backgroundCard = Color(0xFF2D1926),
    )

    val Lagoon = ThemeColorPalette(
        secondary = Color(0xFF14B8A6),
        secondaryVariant = Color(0xFF0891B2),
        nativeAccentHex = "#14B8A6",
        focusRing = Color(0xFF2DD4BF),
        focusBackground = Color(0xFF123A3A),
        background = Color(0xFF081110),
        backgroundElevated = Color(0xFF121E1D),
        backgroundCard = Color(0xFF182A28),
    )

    val Sunset = ThemeColorPalette(
        secondary = Color(0xFFF97316),
        secondaryVariant = Color(0xFFF43F5E),
        nativeAccentHex = "#F97316",
        focusRing = Color(0xFFFB7185),
        focusBackground = Color(0xFF402019),
        background = Color(0xFF120C09),
        backgroundElevated = Color(0xFF211713),
        backgroundCard = Color(0xFF2D1E18),
    )

    val Custom = ThemeColorPalette(
        secondary = ThemeAccentColor.PINK.color,
        secondaryVariant = ThemeAccentColor.CYAN.color,
        nativeAccentHex = "#FF5F9E",
        focusRing = ThemeAccentColor.CYAN.color,
        focusBackground = Color(0xFF2A1838),
        background = Color(0xFF0E0C12),
        backgroundElevated = Color(0xFF1A1720),
        backgroundCard = Color(0xFF231D2A),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    fun getColorPalette(
        theme: AppTheme,
        customFirst: Color = ThemeAccentColor.PINK.color,
        customSecond: Color = ThemeAccentColor.CYAN.color,
    ): ThemeColorPalette {
        val accessibleFirst = customFirst.toAccessibleAccent()
        val accessibleSecond = customSecond.toAccessibleAccent()
        return when (theme) {
            AppTheme.CRIMSON -> Crimson
            AppTheme.OCEAN -> Ocean
            AppTheme.VIOLET -> Violet
            AppTheme.EMERALD -> Emerald
            AppTheme.AMBER -> Amber
            AppTheme.ROSE -> Rose
            AppTheme.MESSENGER -> Messenger
            AppTheme.AMETHYST -> Amethyst
            AppTheme.BLOSSOM -> Blossom
            AppTheme.LAGOON -> Lagoon
            AppTheme.SUNSET -> Sunset
            AppTheme.CUSTOM -> Custom.copy(
                secondary = accessibleFirst,
                secondaryVariant = accessibleSecond,
                nativeAccentHex = accessibleFirst.toThemeHex(),
                onSecondary = Color.White,
                onSecondaryVariant = Color.White,
                focusRing = accessibleSecond,
            )
            AppTheme.WHITE -> White
        }
    }

    fun animatedColors(
        theme: AppTheme,
        customFirst: Color = ThemeAccentColor.PINK.color,
        customSecond: Color = ThemeAccentColor.CYAN.color,
    ): List<Color> = when (theme) {
        AppTheme.MESSENGER -> listOf(
            Color(0xFF006DFF),
            Color(0xFF168AFF),
            Color(0xFF00A8FF),
            Color(0xFF38BDF8),
            Color(0xFF5CC8FF),
            Color(0xFF8AD8FF),
        )
        AppTheme.AMETHYST -> listOf(
            Color(0xFF6D28D9),
            Color(0xFF7C3AED),
            Color(0xFF9333EA),
            Color(0xFFA855F7),
            Color(0xFFC084FC),
            Color(0xFFD8B4FE),
        )
        AppTheme.BLOSSOM -> listOf(
            Color(0xFFDB2777),
            Color(0xFFEC4899),
            Color(0xFFF472B6),
            Color(0xFFFDA4AF),
            Color(0xFFF43F7D),
            Color(0xFFFB7185),
        )
        AppTheme.LAGOON -> listOf(
            Color(0xFF0891B2),
            Color(0xFF06B6D4),
            Color(0xFF22D3EE),
            Color(0xFF14B8A6),
            Color(0xFF2DD4BF),
            Color(0xFF5EEAD4),
        )
        AppTheme.SUNSET -> listOf(
            Color(0xFFF43F5E),
            Color(0xFFFB7185),
            Color(0xFFFDA4AF),
            Color(0xFFF97316),
            Color(0xFFFB923C),
            Color(0xFFFDBA74),
        )
        AppTheme.CUSTOM -> listOf(
            customFirst,
            lerp(customFirst, Color.Black, 0.14f),
            lerp(customFirst, customSecond, 0.30f),
            customSecond,
            lerp(customSecond, Color.White, 0.12f),
            lerp(customSecond, customFirst, 0.28f),
        )
        else -> emptyList()
    }
}

internal fun Color.toThemeHex(): String {
    fun channel(value: Float): String =
        (value.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0').uppercase()
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}

internal fun Color.toAccessibleAccent(): Color {
    var adjusted = copy(alpha = 1f)
    repeat(12) {
        adjusted = when {
            adjusted.luminance() < AccessibleAccentMinLuminance -> lerp(adjusted, Color.White, 0.12f)
            adjusted.luminance() > AccessibleAccentMaxLuminance -> lerp(adjusted, Color.Black, 0.12f)
            else -> return adjusted
        }
    }
    return adjusted
}

internal fun String?.toThemeColor(fallback: Color): Color {
    val value = this?.trim().orEmpty()
    ThemeAccentColor.entries.firstOrNull { it.name == value }?.let { return it.color }
    val hex = value.removePrefix("#")
    if (hex.length != 6 || hex.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return fallback
    return runCatching {
        Color(
            red = hex.substring(0, 2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue = hex.substring(4, 6).toInt(16) / 255f,
            alpha = 1f,
        )
    }.getOrDefault(fallback)
}

private const val AccessibleAccentMinLuminance = 0.12f
private const val AccessibleAccentMaxLuminance = 0.24f
