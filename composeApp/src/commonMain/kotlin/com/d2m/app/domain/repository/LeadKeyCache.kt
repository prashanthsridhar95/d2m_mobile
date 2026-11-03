package com.d2m.app.domain.repository

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class LeadKeyMap(val byCode: Map<String, String> = emptyMap())

/**
 * Per-code lead_key cache for the public share-link viewer (ProfileLinkScreen.kt)
 * -- mirrors d2m_web's leadKeyCache.js (localStorage, keyed by code). Lets a
 * RETURNING anonymous visitor skip the identify gate a second time -- "if
 * the same user opens the link again... it should know", reported
 * directly. Same Settings-backed local-cache pattern as
 * ShareLinkFieldsCache/ArchiveKeyStore (createSettings(), see AppModule.kt).
 */
class LeadKeyCache(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "d2m_lead_keys"

    private fun loadMap(): LeadKeyMap =
        settings.getStringOrNull(key)?.let { raw -> runCatching { json.decodeFromString<LeadKeyMap>(raw) }.getOrNull() } ?: LeadKeyMap()

    fun load(code: String): String? = loadMap().byCode[code]

    fun save(code: String, leadKey: String) {
        val updated = loadMap().copy(byCode = loadMap().byCode + (code to leadKey))
        settings.putString(key, json.encodeToString(updated))
    }
}
