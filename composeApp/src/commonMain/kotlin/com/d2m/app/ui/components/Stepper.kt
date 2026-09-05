package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/**
 * Wizard progress, as the Registration comp's horizontal stepper:
 * numbered circles plus labels sitting on a hairline, with the current
 * step underlined in maroon and completed ones showing a tick.
 *
 * Replaces a LinearProgressIndicator plus a "Question 3 of 5" caption.
 * That pair told you how far along you were but never what the steps
 * actually were, so every screen of the form arrived as a surprise -- and
 * a filling bar reads as "something is loading" more than as "you are
 * three of five steps in". Naming the steps is the whole point of the
 * comp's version.
 *
 * Scrolls sideways rather than wrapping: four or five step labels don't
 * fit across a phone, and wrapping would silently double the header's
 * height and push the first field below the fold.
 *
 * `onStepClick` is optional -- passed, completed steps become tappable
 * (going back to a finished step is safe and expected); never the
 * upcoming ones, since a wizard's validation runs forward.
 */
@Composable
fun D2MStepper(
    steps: List<String>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
        ) {
            steps.forEachIndexed { i, label ->
                val isActive = i == activeIndex
                val isDone = i < activeIndex
                val stepClick = onStepClick?.takeIf { isDone }
                Column(
                    modifier = Modifier
                        .then(if (stepClick != null) Modifier.clickable { stepClick(i) } else Modifier)
                        .padding(end = 18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        StepMark(index = i, isActive = isActive, isDone = isDone)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isActive -> d2m.ink
                                isDone -> d2m.meta
                                else -> d2m.label
                            },
                            maxLines = 1,
                        )
                    }
                    // The active step's 2dp maroon rule sits where the
                    // row's own hairline runs, so the two read as one line
                    // with a coloured segment rather than a double border.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(D2MStroke.emphasis)
                            .background(if (isActive) d2m.accent else Color.Transparent),
                    )
                }
            }
        }
        D2MDivider(soft = false)
    }
}

@Composable
private fun StepMark(index: Int, isActive: Boolean, isDone: Boolean) {
    val tick = d2m.accent
    Box(
        Modifier
            .size(22.dp)
            .background(if (isActive) d2m.accent else Color.Transparent, CircleShape)
            .border(D2MStroke.hairline, if (isActive || isDone) d2m.accent else d2m.borderStrong, CircleShape)
            .then(
                if (isDone) {
                    Modifier.drawBehind {
                        val w = size.width
                        val h = size.height
                        drawLine(tick, Offset(w * 0.28f, h * 0.52f), Offset(w * 0.45f, h * 0.70f), w * 0.10f, StrokeCap.Round)
                        drawLine(tick, Offset(w * 0.45f, h * 0.70f), Offset(w * 0.74f, h * 0.32f), w * 0.10f, StrokeCap.Round)
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isDone) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) d2m.accentOn else d2m.label,
            )
        }
    }
}
