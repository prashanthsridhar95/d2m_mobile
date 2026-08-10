package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * South Indian D1 (Rasi) / D9 (Navamsa) chart renderer -- the one design
 * system component the mobile plan flagged as needing real work rather than
 * a mechanical port (see plan §5): the web app renders this as a native
 * HTML <table> with rowSpan/colSpan for the merged center cell, which
 * Compose has no direct equivalent for, so this is a custom Canvas grid
 * instead, using Compose's own multiplatform TextMeasurer for text (not
 * platform nativeCanvas -- that type differs between Android's
 * android.graphics.Canvas and iOS/Desktop's Skia canvas, which would break
 * the "one UI codebase" point of this whole app).
 *
 * Fixed layout (signs never move -- South Indian style, unlike North Indian
 * where houses are fixed and signs rotate): reading the 4x4 grid row by row,
 * the 12 outer cells are Meenam/Mesham/Rishabam/Mithunam (top),
 * Katakam (right side, row 2) / Simmam (right side, row 3),
 * Dhanusu/Viruchigam/Thulam/Kanni (bottom, reversed),
 * Kumbam (left side, row 3) / Makaram (left side, row 2) --
 * i.e. counter-clockwise from top-left starting at Pisces. The center 2x2
 * is merged into one info cell (Rasi/Nakshatra/Pada/Dasha summary), matching
 * both the existing web AstrologyChart.jsx and the app-ui.prashanthsridhar.com
 * reference's rendering of the same chart.
 *
 * chartJson shape (see ChartOut in data/model/Astrology.kt): this parser
 * expects `{ placements: { "Su": 0, "Mo": 3, ... }, ascendant: 2 }` per
 * division (sign indices 0=Mesham..11=Meenam) -- confirm this exact shape
 * against a real backend response before shipping; the blob is intentionally
 * loose on the backend side (app/models/astrology.py's own doc comment), so
 * this parsing layer is the seam to adjust if the real shape differs.
 */
private val RASI_NAMES = listOf(
    "Mesham", "Rishabam", "Mithunam", "Katakam", "Simmam", "Kanni",
    "Thulam", "Viruchigam", "Dhanusu", "Makaram", "Kumbam", "Meenam",
)

// Grid cell index (0..15, row-major in a 4x4) -> sign index (0=Mesham..11=Meenam),
// null for the 4 merged-center cells.
private val CELL_TO_SIGN: Map<Int, Int?> = mapOf(
    0 to 11, 1 to 0, 2 to 1, 3 to 2,        // row 1: Meenam Mesham Rishabam Mithunam
    4 to 10, 5 to null, 6 to null, 7 to 3,  // row 2: Kumbam . . Katakam
    8 to 9, 9 to null, 10 to null, 11 to 4, // row 3: Makaram . . Simmam
    12 to 8, 13 to 7, 14 to 6, 15 to 5,     // row 4: Dhanusu Viruchigam Thulam Kanni
)

private val PLANET_ABBR = mapOf(
    "Su" to "Su", "Mo" to "Mo", "Ma" to "Ma", "Me" to "Me", "Ju" to "Ju",
    "Ve" to "Ve", "Sa" to "Sa", "Ra" to "Ra", "Ke" to "Ke", "As" to "Asc",
)

data class ChartData(
    val placementsBySign: Map<Int, List<String>>, // sign index -> planet abbrs in that sign
    val ascendantSign: Int?,
)

fun parseChartData(division: JsonObject?): ChartData {
    if (division == null) return ChartData(emptyMap(), null)
    val placements = division["placements"]?.jsonObject
    val bySign = mutableMapOf<Int, MutableList<String>>()
    placements?.entries?.forEach { (planet, signEl) ->
        val signIndex = signEl.jsonPrimitive.intOrNull ?: return@forEach
        val label = PLANET_ABBR[planet] ?: planet
        bySign.getOrPut(signIndex) { mutableListOf() }.add(label)
    }
    val asc = division["ascendant"]?.jsonPrimitive?.intOrNull
    return ChartData(bySign, asc)
}

@Composable
fun AstrologyChartView(
    d1: JsonObject?,
    d9: JsonObject?,
    nakshatra: String? = null,
    pada: Int? = null,
    dashaLord: String? = null,
    dashaRemaining: String? = null,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val d1Data = remember(d1) { parseChartData(d1) }
    val d9Data = remember(d9) { parseChartData(d9) }
    val summary = listOfNotNull(
        d1Data.ascendantSign?.let { "Rasi: ${RASI_NAMES[it]}" },
        nakshatra?.let { "Nakshatra: $it${pada?.let { p -> ", Pada $p" } ?: ""}" },
        dashaLord?.let { "Dasha: $it${dashaRemaining?.let { r -> " — $r" } ?: ""}" },
    ).joinToString(" · ")

    if (stacked) {
        Column(modifier = modifier) {
            SingleChartGrid("RASI (D1)", d1Data, summary)
            Spacer(Modifier.padding(top = 16.dp))
            SingleChartGrid("NAVAMSA (D9)", d9Data, summary = null)
        }
    } else {
        Row(modifier = modifier) {
            SingleChartGrid("RASI (D1)", d1Data, summary, modifier = Modifier.weight(1f))
            SingleChartGrid("NAVAMSA (D9)", d9Data, summary = null, modifier = Modifier.weight(1f).padding(start = 12.dp))
        }
    }
}

@Composable
private fun SingleChartGrid(title: String, data: ChartData, summary: String?, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 6.dp),
        ) {
            drawChartGrid(this, textMeasurer, data, title, summary)
        }
    }
}

private fun drawChartGrid(scope: DrawScope, textMeasurer: TextMeasurer, data: ChartData, centerLabel: String, summary: String?) {
    val cell = scope.size.minDimension / 4f
    val gridColor = Color(0xFF8A5A3B)
    val textColor = Color(0xFF201D1A)

    scope.drawRect(
        color = gridColor,
        topLeft = Offset.Zero,
        size = Size(cell * 4, cell * 4),
        style = Stroke(width = 2f),
    )
    scope.drawRect(
        color = gridColor,
        topLeft = Offset(cell, cell),
        size = Size(cell * 2, cell * 2),
        style = Stroke(width = 1.5f),
    )
    for (i in 1..3) {
        scope.drawLine(gridColor, Offset(i * cell, 0f), Offset(i * cell, cell), strokeWidth = 1f)
        scope.drawLine(gridColor, Offset(i * cell, 3 * cell), Offset(i * cell, 4 * cell), strokeWidth = 1f)
        scope.drawLine(gridColor, Offset(0f, i * cell), Offset(cell, i * cell), strokeWidth = 1f)
        scope.drawLine(gridColor, Offset(3 * cell, i * cell), Offset(4 * cell, i * cell), strokeWidth = 1f)
    }

    val signStyle = TextStyle(fontSize = 9.sp, color = textColor)
    val planetStyle = TextStyle(fontSize = 9.sp, color = textColor, fontWeight = FontWeight.Bold)
    val centerStyle = TextStyle(fontSize = 9.sp, color = textColor, textAlign = TextAlign.Center)

    for (index in 0..15) {
        val signIndex = CELL_TO_SIGN[index] ?: continue
        val row = index / 4
        val col = index % 4
        val x = col * cell + 3f
        var y = row * cell + 3f

        val signResult = textMeasurer.measure(RASI_NAMES[signIndex].take(6), signStyle)
        scope.drawText(signResult, topLeft = Offset(x, y))
        y += signResult.size.height + 2f

        data.placementsBySign[signIndex].orEmpty().forEach { p ->
            val planetResult = textMeasurer.measure(p, planetStyle)
            scope.drawText(planetResult, topLeft = Offset(x, y))
            y += planetResult.size.height + 1f
        }
    }

    val centerText = summary?.takeIf { it.isNotBlank() } ?: centerLabel
    val centerResult = textMeasurer.measure(
        text = centerText,
        style = centerStyle,
        constraints = androidx.compose.ui.unit.Constraints(maxWidth = (cell * 2 - 12f).toInt().coerceAtLeast(1)),
    )
    scope.drawText(
        centerResult,
        topLeft = Offset(
            cell * 2 - centerResult.size.width / 2f,
            cell * 2 - centerResult.size.height / 2f,
        ),
    )
}
