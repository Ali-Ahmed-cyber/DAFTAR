package com.daftar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.AppThemeMode

private val DaftarLightScheme = lightColorScheme(
    primary = NavyClassic,
    onPrimary = Color.White,
    primaryContainer = GoldSoft,
    onPrimaryContainer = NavyClassic,
    secondary = GoldPrimary,
    onSecondary = NavyClassic,
    secondaryContainer = GoldLight,
    onSecondaryContainer = NavyClassic,
    tertiary = EmeraldProfit,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = CharcoalText,
    surface = SurfaceWhite,
    onSurface = CharcoalText,
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonError,
)

private val DaftarDarkScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = NavyClassic,
    primaryContainer = NavyElevated,
    onPrimaryContainer = GoldLight,
    secondary = GoldPrimary,
    onSecondary = NavyClassic,
    secondaryContainer = NavyDeep,
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldProfitDark,
    onTertiary = NavyClassic,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    error = CrimsonErrorDark,
    onError = DarkBackground,
    errorContainer = Color(0xFF3E1818),
    onErrorContainer = CrimsonErrorDark,
)

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

@Composable
fun DaftarTheme(
    language: AppLanguage = AppLanguage.ARABIC,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (themeMode == AppThemeMode.DARK) DaftarDarkScheme else DaftarLightScheme
    val layoutDir = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDir,
        LocalAppLanguage provides language,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DaftarTypography,
            content = content,
        )
    }
}
