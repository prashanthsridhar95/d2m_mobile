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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.d2m.app.ui.theme.d2m

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
    "Sun" to "Su", "Moon" to "Mo", "Mars" to "Ma", "Mercury" to "Me", "Jupiter" to "Ju",
    "Venus" to "Ve", "Saturn" to "Sa", "Rahu" to "Ra", "Ketu" to "Ke", "Ascendant" to "Asc",
    // legacy two-letter keys
    "Su" to "Su", "Mo" to "Mo", "Ma" to "Ma", "Me" to "Me", "Ju" to "Ju",
    "Ve" to "Ve", "Sa" to "Sa", "Ra" to "Ra", "Ke" to "Ke", "As" to "Asc",
)

// Sign name -> index into RASI_NAMES. The backend writes English (or Sanskrit) names; same aliasing as the web chart's RASI_ALIASES.
private val SIGN_INDEX: Map<String, Int> = buildMap {
    val aliases = listOf(
        listOf("Mesham", "Mesha", "Aries"), listOf("Rishabam", "Vrishabha", "Taurus"),
        listOf("Mithunam", "Mithuna", "Gemini"), listOf("Katakam", "Kataka", "Cancer"),
        listOf("Simmam", "Simha", "Leo"), listOf("Kanni", "Kanya", "Virgo"),
        listOf("Thulam", "Tula", "Libra"), listOf("Viruchigam", "Vrischika", "Scorpio"),
        listOf("Dhanusu", "Dhanu", "Sagittarius"), listOf("Makaram", "Makara", "Capricorn"),
        listOf("Kumbam", "Kumbha", "Aquarius"), listOf("Meenam", "Meena", "Pisces"),
    )
    aliases.forEachIndexed { i, names -> names.forEach { put(it.lowercase(), i) } }
}

data class ChartData(
    val placementsBySign: Map<Int, List<String>>, // sign index -> planet abbrs in that sign (Asc included)
    val ascendantSign: Int?,
    val moonSign: Int? = null,
)

/**
 * Reads a chart division. The backend's real shape (see app/services/astrology_service.py and
 * astrology_charts.chart_json) is `{ "Sun": "Gemini", "Moon": "Pisces", ..., "Ascendant": "Gemini" }` --
 * planet -> sign NAME. The earlier `{ placements: { "Su": 3 }, ascendant: 2 }` shape (numeric sign indices) is still
 * accepted as a fallback. Before this read the real shape, every chart came out empty.
 */
fun parseChartData(division: JsonObject?): ChartData {
    if (division == null) return ChartData(emptyMap(), null)
    val bySign = mutableMapOf<Int, MutableList<String>>()
    var asc: Int? = null
    var moon: Int? = null

    fun signOf(el: kotlinx.serialization.json.JsonElement): Int? {
        val prim = el as? kotlinx.serialization.json.JsonPrimitive ?: return null
        return prim.intOrNull ?: SIGN_INDEX[prim.content.trim().lowercase()]
    }

    fun place(planet: String, signEl: kotlinx.serialization.json.JsonElement) {
        val sign = signOf(signEl) ?: return
        bySign.getOrPut(sign) { mutableListOf() }.add(PLANET_ABBR[planet] ?: planet.take(2))
        if (planet == "Ascendant" || planet == "As") asc = sign
        if (planet == "Moon" || planet == "Mo") moon = sign
    }

    val legacy = division["placements"] as? JsonObject
    if (legacy != null) {
        legacy.entries.forEach { (planet, el) -> place(planet, el) }
        division["ascendant"]?.let { el -> signOf(el)?.let { if (asc == null) { asc = it; bySign.getOrPut(it) { mutableListOf() }.add("Asc") } } }
    } else {
        division.entries.forEach { (planet, el) -> if (PLANET_ABBR.containsKey(planet)) place(planet, el) }
    }
    // Ascendant first in its cell, like the web chart's marker.
    asc?.let { a -> bySign[a]?.let { l -> if (l.remove("Asc")) l.add(0, "Asc") } }
    return ChartData(bySign, asc, moon)
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
    val d1Lines = listOfNotNull(
        d1Data.ascendantSign?.let { "Lagna: ${RASI_NAMES[it]}" },
        d1Data.moonSign?.let { "Rasi: ${RASI_NAMES[it]}" },
        nakshatra?.let { "Nakshatra: $it${pada?.let { p -> ", Pada $p" } ?: ""}" },
        dashaLord?.let { "Dasha: $it${dashaRemaining?.let { r -> " — $r" } ?: ""}" },
    )
    val d9Lines = listOfNotNull(d9Data.ascendantSign?.let { "Lagna: ${RASI_NAMES[it]}" }, d9Data.moonSign?.let { "Rasi: ${RASI_NAMES[it]}" })

    if (stacked) {
        Column(modifier = modifier) {
            SingleChartGrid("RASI (D1)", d1Data, d1Lines)
            Spacer(Modifier.padding(top = 20.dp))
            SingleChartGrid("NAVAMSA (D9)", d9Data, d9Lines)
        }
    } else {
        Row(modifier = modifier) {
            SingleChartGrid("RASI (D1)", d1Data, d1Lines, modifier = Modifier.weight(1f))
            SingleChartGrid("NAVAMSA (D9)", d9Data, d9Lines, modifier = Modifier.weight(1f).padding(start = 12.dp))
        }
    }
}

@Composable
private fun SingleChartGrid(title: String, data: ChartData, centerLines: List<String>, modifier: Modifier = Modifier) {
    // Colours are read here, in composition, and passed to the draw phase (CompositionLocals aren't available there).
    val palette = ChartPalette(
        line = d2m.goldStrong,
        ink = d2m.textPrimary,
        muted = d2m.meta,
        lagnaFill = d2m.goldSoft,
        lagnaInk = d2m.accentStrong,
        cellFill = MaterialTheme.colorScheme.surface,
    )
    val textMeasurer = rememberTextMeasurer()
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = d2m.meta)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 8.dp),
        ) {
            drawChartGrid(this, textMeasurer, data, title, centerLines, palette)
        }
    }
}

private class ChartPalette(
    val line: Color,
    val ink: Color,
    val muted: Color,
    val lagnaFill: Color,
    val lagnaInk: Color,
    val cellFill: Color,
)

private fun drawChartGrid(
    scope: DrawScope,
    textMeasurer: TextMeasurer,
    data: ChartData,
    centerLabel: String,
    centerLines: List<String>,
    pal: ChartPalette,
) {
    with(scope) {
        val side = size.minDimension
        val cell = side / 4f
        val pad = 6.dp.toPx()
        val thin = 1.dp.toPx()
        val thick = 1.5.dp.toPx()

        // Outer 12 cells: fill (the lagna cell is highlighted, like the web chart) ...
        for (index in 0..15) {
            val signIndex = CELL_TO_SIGN[index] ?: continue
            val topLeft = Offset((index % 4) * cell, (index / 4) * cell)
            drawRect(if (signIndex == data.ascendantSign) pal.lagnaFill else pal.cellFill, topLeft, Size(cell, cell))
        }
        // ... a corner slash on the lagna cell, the traditional South Indian marker.
        data.ascendantSign?.let { a ->
            val index = CELL_TO_SIGN.entries.firstOrNull { it.value == a }?.key
            if (index != null) {
                val x = (index % 4) * cell
                val y = (index / 4) * cell
                drawLine(pal.line, Offset(x, y), Offset(x + cell * 0.28f, y + cell * 0.28f), thick)
            }
        }

        // Grid rules: the 4x4 lattice minus the merged centre.
        drawRect(pal.line, Offset.Zero, Size(side, side), style = Stroke(thick))
        drawRect(pal.line, Offset(cell, cell), Size(cell * 2, cell * 2), style = Stroke(thick))
        for (i in 1..3) {
            drawLine(pal.line, Offset(i * cell, 0f), Offset(i * cell, cell), thin)
            drawLine(pal.line, Offset(i * cell, 3 * cell), Offset(i * cell, 4 * cell), thin)
            drawLine(pal.line, Offset(0f, i * cell), Offset(cell, i * cell), thin)
            drawLine(pal.line, Offset(3 * cell, i * cell), Offset(4 * cell, i * cell), thin)
        }

        val signStyle = TextStyle(fontSize = 10.sp, color = pal.muted)
        val planetStyle = TextStyle(fontSize = 16.sp, color = pal.ink, fontWeight = FontWeight.Bold)
        val lagnaStyle = TextStyle(fontSize = 16.sp, color = pal.lagnaInk, fontWeight = FontWeight.Bold)
        val gap = 6.dp.toPx()

        for (index in 0..15) {
            val signIndex = CELL_TO_SIGN[index] ?: continue
            val cellX = (index % 4) * cell
            val cellY = (index / 4) * cell

            // Sign name, small, top-left (the slash corner is top-left too, so nudge it right on the lagna cell).
            val nameX = cellX + pad + if (signIndex == data.ascendantSign) cell * 0.2f else 0f
            val signResult = textMeasurer.measure(RASI_NAMES[signIndex], signStyle)
            drawText(signResult, topLeft = Offset(nameX, cellY + pad - 1.dp.toPx()))

            // Planets: wrapped left to right, like words, instead of one per line -- four or five fit a cell.
            var x = cellX + pad
            var y = cellY + pad + signResult.size.height + 4.dp.toPx()
            val maxX = cellX + cell - pad
            data.placementsBySign[signIndex].orEmpty().forEach { p ->
                val r = textMeasurer.measure(p, if (p == "Asc") lagnaStyle else planetStyle)
                if (x + r.size.width > maxX && x > cellX + pad) {
                    x = cellX + pad
                    y += r.size.height + 2.dp.toPx()
                }
                if (y + r.size.height <= cellY + cell) drawText(r, topLeft = Offset(x, y))
                x += r.size.width + gap
            }
        }

        // Centre: the caption lines (or just the chart title), centred as a block.
        val centerStyle = TextStyle(fontSize = 13.sp, color = pal.ink, textAlign = TextAlign.Center)
        val maxW = (cell * 2 - 2 * pad).toInt().coerceAtLeast(1)
        val results = (centerLines.ifEmpty { listOf(centerLabel) }).map {
            textMeasurer.measure(it, centerStyle, constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxW))
        }
        val total = results.sumOf { it.size.height } + (results.size - 1) * 3.dp.toPx()
        var cy = cell * 2 - total / 2f
        results.forEach { r ->
            drawText(r, topLeft = Offset(cell * 2 - r.size.width / 2f, cy))
            cy += r.size.height + 3.dp.toPx()
        }
    }
}
