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

/** Mirrors app/schemas.py's PanchangamElementOut exactly -- one of tithi/nakshatra/yoga/karana. */
@Serializable
data class PanchangamElementOut(
    val name: String,
    val index: Int,
    val endsAt: String, // ISO datetime, local (IST) display
    val pada: Int? = null, // nakshatra only
)

/** Mirrors app/schemas.py's PanchangamWindowOut exactly -- rahu kalam / yamagandam / gulika kalam / abhijit & brahma muhurta. */
@Serializable
data class PanchangamWindowOut(
    val start: String,
    val end: String,
)

/** Mirrors app/schemas.py's TamilDateOut exactly -- Tamil solar calendar for the day. */
@Serializable
data class TamilDateOut(
    val year: String, // 60-year cycle name, e.g. "Parabhava"
    val month: String, // e.g. "Aani"
    val date: Int, // day-of-month within that Tamil month, 1-indexed
)

/**
 * Mirrors app/schemas.py's PanchangamDayOut exactly -- previously flattened
 * tithi/nakshatra/yoga/karana into plain strings (they're actually nested
 * PanchangamElementOut objects) and rahuKalam/yamagandam/gulika into plain
 * strings (actually nested PanchangamWindowOut {start,end} objects), was
 * missing several required fields entirely (vaara, weekday, sunrise, sunset,
 * moonSign, sunSign, abhijitMuhurta, brahmaMuhurta, tamil), and had a
 * `gulika`/`muhurtas`/`tamilCalendarDate` that don't exist on the backend at
 * all. tithi/nakshatra/yoga/karana being required non-Optional nested
 * objects meant this crashed decode on every load of this screen.
 */
@Serializable
data class PanchangamDayOut(
    val date: String, // YYYY-MM-DD
    val vaara: String, // Sanskrit weekday name
    val weekday: String,
    val sunrise: String,
    val sunset: String,
    val moonSign: String,
    val sunSign: String,
    val tithi: PanchangamElementOut,
    val nakshatra: PanchangamElementOut,
    val yoga: PanchangamElementOut,
    val karana: PanchangamElementOut,
    val rahuKalam: PanchangamWindowOut,
    val yamagandam: PanchangamWindowOut,
    val gulikaKalam: PanchangamWindowOut,
    val abhijitMuhurta: PanchangamWindowOut,
    val brahmaMuhurta: PanchangamWindowOut,
    val isChandrashtamam: Boolean,
    val isMuhoortham: Boolean,
    val tamil: TamilDateOut,
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
