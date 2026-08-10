package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Free Open-Meteo Geocoding API -- same provider d2m_web's lib/geocoding.js
 * uses for CityAutocomplete.jsx/CityChipPicker.jsx. Resolves lat/lon/timezone
 * on selection; a manual-coordinate fallback stays available in the
 * onboarding wizard for the "Null Island" (0,0) guard case, same as web.
 */
@Serializable
data class GeocodeResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    val timezone: String? = null,
)

@Serializable
private data class GeocodeResponse(val results: List<GeocodeResult>? = null)

data class CitySuggestion(val id: Long, val label: String, val lat: Double, val lon: Double, val timezone: String?)

class GeocodingApi(engineHttpClient: HttpClient) {
    private val client = engineHttpClient.config {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun searchCities(query: String, count: Int = 8): List<CitySuggestion> {
        if (query.trim().length < 2) return emptyList()
        val response: GeocodeResponse = client.get("https://geocoding-api.open-meteo.com/v1/search") {
            parameter("name", query)
            parameter("count", count)
            parameter("language", "en")
            parameter("format", "json")
        }.body()
        return (response.results ?: emptyList()).map {
            val labelParts = listOfNotNull(it.name, it.admin1, it.country).distinct()
            CitySuggestion(
                id = it.id,
                label = labelParts.joinToString(", "),
                lat = it.latitude,
                lon = it.longitude,
                timezone = it.timezone,
            )
        }
    }
}
