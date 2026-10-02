package com.d2m.app.data.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Started as a direct Kotlin port of d2m_web/src/auth/DevIdentity.jsx: a
 * plain local key-value store (there, localStorage; here,
 * multiplatform-settings) remembering { role, sponsorId, primaryId,
 * childPrimaryId }, with no real credential check behind it.
 *
 * That swap has now happened here: this also holds wedlockAccessToken, the
 * real bearer JWT issued by WedLock IAM's POST /auth/login (see
 * data/network/WedLockApi.kt), obtained via LoginScreen.kt's real
 * register/login flow. ApiClient.kt reads it from here and attaches it as
 * `Authorization: Bearer <token>` on every request to d2m_core_engine --
 * see that file's doc comment. The sponsorId/primaryId/role fields remain:
 * d2m_core_engine has no "look up my Sponsor/Primary by WedLock account"
 * endpoint yet, so which D2M profile a logged-in WedLock account opens is
 * still resolved via LoginScreen's existing id-confirm step (now gated
 * behind a real password check rather than being the credential check
 * itself), and is set directly from the create/redeem response during
 * registration (see OnboardingWizardScreen.kt / ClaimFlowScreen.kt).
 *
 * Deliberately kept swappable: every screen reads identity through this one
 * class, so replacing the storage/verification mechanism is a change to
 * this file and ApiClient's auth header handling, not to every screen that
 * currently reads a raw id.
 */
enum class D2MRole { PARENT, CHILD }

@Serializable
data class Identity(
    val role: D2MRole? = null,
    val sponsorId: String? = null,
    val primaryId: String? = null,
    val childPrimaryId: String? = null,
    val wedlockAccessToken: String? = null,
)

class IdentityStore(private val settings: Settings) {
    private val _identity = MutableStateFlow(load())
    val identity: StateFlow<Identity> = _identity.asStateFlow()

    private fun load(): Identity = Identity(
        role = settings[KEY_ROLE, ""].takeIf { it.isNotEmpty() }?.let { D2MRole.valueOf(it) },
        sponsorId = settings[KEY_SPONSOR_ID, ""].takeIf { it.isNotEmpty() },
        primaryId = settings[KEY_PRIMARY_ID, ""].takeIf { it.isNotEmpty() },
        childPrimaryId = settings[KEY_CHILD_PRIMARY_ID, ""].takeIf { it.isNotEmpty() },
        wedlockAccessToken = settings[KEY_WEDLOCK_TOKEN, ""].takeIf { it.isNotEmpty() },
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

    /** Stores the WedLock access_token from POST /auth/login -- see WedLockApi.kt. Independent of role/sponsorId/primaryId, so setParent/setChild/clear below only touch it via clear(). */
    fun setWedlockAccessToken(token: String) {
        settings.putString(KEY_WEDLOCK_TOKEN, token)
        _identity.value = load()
    }

    fun clearWedlockAccessToken() {
        settings.remove(KEY_WEDLOCK_TOKEN)
        _identity.value = load()
    }

    fun clear() {
        settings.remove(KEY_ROLE)
        settings.remove(KEY_SPONSOR_ID)
        settings.remove(KEY_PRIMARY_ID)
        settings.remove(KEY_CHILD_PRIMARY_ID)
        settings.remove(KEY_WEDLOCK_TOKEN)
        _identity.value = load()
    }

    private companion object {
        const val KEY_ROLE = "d2m_role"
        const val KEY_SPONSOR_ID = "d2m_sponsor_id"
        const val KEY_PRIMARY_ID = "d2m_primary_id"
        const val KEY_CHILD_PRIMARY_ID = "d2m_child_primary_id"
        const val KEY_WEDLOCK_TOKEN = "d2m_wedlock_token"
    }
}

expect fun createSettings(): Settings
