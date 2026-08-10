package com.d2m.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * chartJson holds the D1 (Rasi) + D9 (Navamsa) placement dicts, keyed by planet
 * abbreviation -> sign index, plus dasha-at-birth. Deliberately kept as a loose
 * JsonObject rather than fully-typed fields -- mirrors the backend's own
 * "chart_json blob" shape (app/models/astrology.py's AstrologyChart.chart_json),
 * parsed into ChartData by AstrologyChartView at render time (see
 * ui/components/AstrologyChartView.kt for the D1/D9 grid renderer and
 * ChartData's typed parse of this blob).
 */
@Serializable
data class ChartOut(
    val profileId: String,
    val chartJson: JsonObject,
    val ayanamsaUsed: String,
    val computedAt: String,
)

@Serializable
data class PanchangamDayOut(
    val date: String,
    val tithi: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val rahuKalam: String? = null,
    val yamagandam: String? = null,
    val gulika: String? = null,
    val muhurtas: List<String> = emptyList(),
    val tamilCalendarDate: String? = null,
    val isChandrashtamam: Boolean = false,
    val isMuhoortham: Boolean = false,
)

@Serializable
data class PanchangamMonthDayEntry(
    val date: String,
    val tithi: String,
    val nakshatra: String,
    val isChandrashtamam: Boolean = false,
    val isMuhoortham: Boolean = false,
)

@Serializable
data class PanchangamMonthOut(
    val year: Int,
    val month: Int,
    val days: List<PanchangamMonthDayEntry>,
)
