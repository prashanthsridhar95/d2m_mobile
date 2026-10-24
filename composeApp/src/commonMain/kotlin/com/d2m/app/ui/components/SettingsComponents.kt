package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText

/** Corner radius shared by every grouped settings card. */
val SettingsCardRadius = 16.dp

/**
 * A grouped, inset settings block: an uppercase micro-label above a rounded
 * white card. Put [SettingsRow]/[SettingsSwitchRow] inside, separated by
 * [SettingsDivider]. [footer] is an optional explanatory line below the card.
 */
@Composable
fun SettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = d2m.label,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        SettingsCard(content = content)
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.labelMedium,
                color = mutedText(0.55f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
    }
}

/** The rounded white surface with a hairline border and no elevation. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(SettingsCardRadius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(D2MStroke.hairline, d2m.borderSoft, shape),
        content = content,
    )
}

/** Hairline rule inset to line up with the text column (past the icon chip). */
@Composable
fun SettingsDivider(inset: Dp = 62.dp) {
    HorizontalDivider(modifier = Modifier.padding(start = inset), thickness = D2MStroke.hairline, color = d2m.borderSoft)
}

/** Soft rounded square holding a row's leading icon. */
@Composable
fun SettingsIconChip(icon: ImageVector, destructive: Boolean = false) {
    val danger = LocalD2MStatusPalette.current.danger
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (destructive) danger.bg else d2m.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (destructive) danger.fg else d2m.accentStrong, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun RowText(title: String, subtitle: String?, destructive: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = if (destructive) LocalD2MStatusPalette.current.danger.fg else d2m.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * A navigation/action row: icon chip, title + subtitle, then [trailing]
 * (default: a chevron). 56dp minimum height, bounce press feedback.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = {
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText(0.45f))
    },
) {
    Row(
        modifier
            .fillMaxWidth()
            .bounceClickable(onClick = onClick, pressedScale = 0.985f, enforceMinimumTarget = false)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsIconChip(icon, destructive)
        RowText(title, subtitle, destructive, Modifier.weight(1f))
        trailing?.invoke()
    }
}

/**
 * A labelled on/off setting as ONE control: the whole row toggles and is
 * exposed to accessibility as a single switch named by its title.
 */
@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch) {
                haptics.light()
                onCheckedChange(it)
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsIconChip(icon)
        RowText(title, subtitle, false, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = d2m.accentOn,
                checkedTrackColor = d2m.accent,
                checkedBorderColor = d2m.accent,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = d2m.borderStrong,
                uncheckedBorderColor = d2m.borderStrong,
            ),
        )
    }
}
