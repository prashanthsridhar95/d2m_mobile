package com.d2m.app.data.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Direct Kotlin port of d2m_web/src/auth/DevIdentity.jsx: a plain local
 * key-value store (there, localStorage; here, multiplatform-settings) that
 * remembers { role, sponsorId, primaryId, childPrimaryId }. This is not a
 * real session/auth layer -- there isn't one on the backend either (see
 * ApiClient.kt's doc comment and the mobile plan §1/§7). "Logging in" here
 * means the same thing it means on web: paste/confirm an existing sponsorId
 * or primaryId obtained from onboarding or an admin lookup, not a real
 * credential check.
 *
 * Deliberately kept swappable: every screen reads identity through this one
 * class, so replacing the storage/verification mechanism with real
 * token-based auth later is a change to this file and ApiClient's auth
 * header handling, not to every screen that currently reads a raw id.
 */
enum class D2MRole { PARENT, CHILD }

@Serializable
data class Identity(
    val role: D2MRole? = null,
    val sponsorId: String? = null,
    val primaryId: String? = null,
    val childPrimaryId: String? = null,
)

class IdentityStore(private val settings: Settings) {
    private val _identity = MutableStateFlow(load())
    val identity: StateFlow<Identity> = _identity.asStateFlow()

    private fun load(): Identity = Identity(
        role = settings[KEY_ROLE, ""].takeIf { it.isNotEmpty() }?.let { D2MRole.valueOf(it) },
        sponsorId = settings[KEY_SPONSOR_ID, ""].takeIf { it.isNotEmpty() },
        primaryId = settings[KEY_PRIMARY_ID, ""].takeIf { it.isNotEmpty() },
        childPrimaryId = settings[KEY_CHILD_PRIMARY_ID, ""].takeIf { it.isNotEmpty() },
    )

    fun setParent(sponsorId: String, childPrimaryId: String? = null) {
        settings.putString(KEY_ROLE, D2MRole.PARENT.name)
        settings.putString(KEY_SPONSOR_ID, sponsorId)
        settings.remove(KEY_PRIMARY_ID)
        childPrimaryId?.let { settings.putString(KEY_CHILD_PRIMARY_ID, it) }
        _identity.value = load()
    }

    fun setChild(primaryId: String) {
        settings.putString(KEY_ROLE, D2MRole.CHILD.name)
        settings.putString(KEY_PRIMARY_ID, primaryId)
        settings.remove(KEY_SPONSOR_ID)
        _identity.value = load()
    }

    fun updateChildPrimaryId(childPrimaryId: String) {
        settings.putString(KEY_CHILD_PRIMARY_ID, childPrimaryId)
        _identity.value = load()
    }

    fun clear() {
        settings.remove(KEY_ROLE)
        settings.remove(KEY_SPONSOR_ID)
        settings.remove(KEY_PRIMARY_ID)
        settings.remove(KEY_CHILD_PRIMARY_ID)
        _identity.value = load()
    }

    private companion object {
        const val KEY_ROLE = "d2m_role"
        const val KEY_SPONSOR_ID = "d2m_sponsor_id"
        const val KEY_PRIMARY_ID = "d2m_primary_id"
        const val KEY_CHILD_PRIMARY_ID = "d2m_child_primary_id"
    }
}

expect fun createSettings(): Settings
