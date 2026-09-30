package com.d2m.app.domain.repository

import com.d2m.app.data.model.CustomFieldSelection
import com.russhwolf.settings.Settings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Remembers the last-used "Custom" field selection on ShareLinksScreen.kt,
 * mirroring d2m_web's lib/customFieldsCache.js -- "The setting should be
 * stored in cache", reported directly. Same Settings-backed local-cache
 * pattern as ArchiveKeyStore/ParentContactsStore (createSettings(), see
 * AppModule.kt). One shared value, not per-link -- this is a creator's
 * general field-selection preference, not something tied to any one
 * link's identity.
 */
class ShareLinkFieldsCache(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "d2m_share_link_custom_fields"

    fun load(): CustomFieldSelection? {
        val raw = settings.getStringOrNull(key) ?: return null
        return runCatching { json.decodeFromString<CustomFieldSelection>(raw) }.getOrNull()
    }

    fun save(selection: CustomFieldSelection) {
        settings.putString(key, json.encodeToString(selection))
    }
}
