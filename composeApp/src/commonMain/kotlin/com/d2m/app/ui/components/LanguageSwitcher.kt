package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.strings.AppLocale
import com.d2m.app.ui.strings.LocaleStore
import com.d2m.app.ui.theme.mutedText

/**
 * Two pieces for one setting, same split d2m_web's LanguageSwitcher.jsx
 * uses: a compact control for LoginScreen (available before any identity
 * exists) and a fuller labeled one for SettingsScreen's account group.
 * Both just flip [LocaleStore]'s value -- every screen already reads
 * [com.d2m.app.ui.strings.LocalStrings] which is re-derived from it in
 * App.kt's root composable, so no separate recomposition plumbing is
 * needed here.
 */
@Composable
fun LanguageSwitcherCompact(localeStore: LocaleStore, modifier: Modifier = Modifier) {
    val locale by localeStore.locale.collectAsState()
    D2MSegmented(
        options = AppLocale.entries.map { it.nativeName },
        selectedIndex = AppLocale.entries.indexOf(locale),
        onSelect = { i -> localeStore.setLocale(AppLocale.entries[i]) },
        modifier = modifier,
        small = true,
    )
}

@Composable
fun LanguageSettingControl(localeStore: LocaleStore, hint: String, modifier: Modifier = Modifier) {
    val locale by localeStore.locale.collectAsState()
    Column(modifier) {
        D2MSegmented(
            options = AppLocale.entries.map { it.nativeName },
            selectedIndex = AppLocale.entries.indexOf(locale),
            onSelect = { i -> localeStore.setLocale(AppLocale.entries[i]) },
        )
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = mutedText(0.55f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
