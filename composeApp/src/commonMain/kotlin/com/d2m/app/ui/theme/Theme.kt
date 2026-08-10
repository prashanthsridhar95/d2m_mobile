package com.d2m.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val LocalD2MFlow = compositionLocalOf { D2MFlow.PARENT }
val LocalD2MStatusPalette = compositionLocalOf { LightStatusPalette }

private fun schemeFor(flow: D2MFlow, dark: Boolean): ColorScheme {
    val accent = when (flow) {
        D2MFlow.ENTRY -> if (dark) D2MAccents.EntryDark else D2MAccents.EntryLight
        D2MFlow.PARENT -> D2MAccents.Parent
        D2MFlow.CHILD -> if (dark) D2MAccents.ChildDark else D2MAccents.ChildLight
        D2MFlow.GUEST_SYSTEM -> if (dark) D2MAccents.GuestSystemDark else D2MAccents.GuestSystemLight
    }
    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            secondary = accent,
            background = Color(0xFF141210),
            surface = Color(0xFF1C1A17),
            onBackground = Color(0xFFF2EFEA),
            onSurface = Color(0xFFF2EFEA),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent,
            background = Color(0xFFFFFDFA),
            surface = Color.White,
            onBackground = Color(0xFF201D1A),
            onSurface = Color(0xFF201D1A),
        )
    }
}

/**
 * Root theme wrapper -- callers pick a flow per-shell (entry/parent/child/
 * guest_system), matching d2m_web's data-flow attribute switch. Dark mode
 * follows the system by default (ThemeProvider.jsx's "system" preference);
 * a future Settings screen can override this the same way the web app's
 * 3-way light/dark/system toggle does (not wired yet -- see plan §9).
 */
@Composable
fun D2MTheme(
    flow: D2MFlow,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = schemeFor(flow, darkTheme)
    val statusPalette = if (darkTheme) DarkStatusPalette else LightStatusPalette

    CompositionLocalProvider(
        LocalD2MFlow provides flow,
        LocalD2MStatusPalette provides statusPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = D2MTypography,
            content = content,
        )
    }
}

/** Alpha-over-onSurface equivalent of tokens.css's --text-muted-15/35/45/55 ladder. */
@Composable
fun mutedText(alpha: Float): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surface)
