package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.mutedText

/** Mirrors components/EmptyState.jsx. */
@Composable
fun D2MEmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))
        }
    }
}

/** Mirrors components/ErrorBanner.jsx -- the shared inline error surface used in every screen's catch block. */
@Composable
fun D2MErrorBanner(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val danger = LocalD2MStatusPalette.current.danger
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(danger.bg, RoundedCornerShape(D2MRadius.md))
            .padding(14.dp),
    ) {
        Text(message, color = danger.fg, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Retry", color = danger.fg) }
        }
    }
}
