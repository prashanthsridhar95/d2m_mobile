package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m

/**
 * Loading state shaped like the checklist itself (control row, summary strip, task rows -- plus the filter panel on
 * tablets), so nothing jumps when the real content arrives.
 */
@Composable
fun WeddingSkeleton(modifier: Modifier = Modifier) {
    val loading = LocalStrings.current.wedding.loading
    BoxWithConstraints(modifier.fillMaxSize().semantics { contentDescription = loading }) {
        val wide = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Column(
                    Modifier.width(300.dp).fillMaxHeight().padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                        .border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    D2MSkeleton(width = 90.dp, height = 20.dp)
                    repeat(4) { g ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            D2MSkeleton(width = 80.dp, height = 10.dp)
                            repeat(if (g == 3) 2 else 4) { D2MSkeleton(height = 14.dp) }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        D2MSkeleton(Modifier.weight(1f), height = 38.dp)
                        if (!wide) D2MSkeleton(width = 92.dp, height = 36.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        D2MSkeleton(Modifier.weight(1f), height = 38.dp)
                        D2MSkeleton(width = 104.dp, height = 36.dp)
                        D2MSkeleton(width = 70.dp, height = 36.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        D2MSkeleton(width = 62.dp, height = 16.dp)
                        D2MSkeleton(Modifier.weight(1f), height = 6.dp, radius = 99.dp)
                        D2MSkeleton(width = 110.dp, height = 12.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { D2MSkeleton(width = if (it == 3) 84.dp else 96.dp, height = 28.dp, radius = 99.dp) }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    D2MSkeleton(height = 38.dp, radius = 0.dp)
                    repeat(7) { i ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            D2MSkeleton(width = 24.dp, height = 24.dp, radius = 99.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                D2MSkeleton(Modifier.fillMaxWidth(if (i % 3 == 0) 0.9f else if (i % 3 == 1) 0.7f else 0.8f), height = 14.dp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    D2MSkeleton(width = 92.dp, height = 22.dp, radius = 99.dp)
                                    D2MSkeleton(width = 60.dp, height = 22.dp, radius = 99.dp)
                                    D2MSkeleton(width = 54.dp, height = 12.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
