package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/schemas.py + app/models/identity.py in d2m_core_engine, field for
 * field. No client-side validation is added beyond what the backend itself
 * enforces (mostly none -- ExtendedBioDataIn in particular is deliberately
 * permissive, see that class's own doc comment below), matching the backend's
 * own "the frontend decides who's allowed to call this" posture (there is no
 * server-side RBAC today -- see IdentityStore's doc comment for how this
 * client authenticates, i.e. doesn't).
 */

/** Mirrors app/schemas.py::MeOut -- GET /me resolves the caller's own
 * WedLock-verified bearer token straight to a D2M account (role +
 * sponsor_id/primary_id), so LoginScreen.kt can skip its manual
 * "I'm a parent / I'm the child, paste your id" step for the common case.
 * See that screen's own doc comment. */
@Serializable
data class MeOut(
    val role: String, // "parent" | "child"
    val sponsorId: String? = null,
    val primaryId: String? = null,
    val name: String,
)

@Serializable
data class SponsorCreateRequest(
    val name: String,
    val contactInfo: String,
    val relationshipToChild: String? = null,
    val ownReligion: String,
    val ownCasteCommunity: String,
    val acceptReligions: List<String>,
    val hardCasteCommunity: Boolean,
    val minAge: Int,
    val maxAge: Int,
    val acceptLocations: List<String> = emptyList(),
    val maritalStatusFilter: List<String> = emptyList(),
    val rankedCommunityList: List<String> = emptyList(),
    val childName: String,
    val childGender: String,
    val childSeekingGender: String,
    val childDob: String,
    val childTob: String,
    val childBirthPlace: String,
    val childBirthLat: Double,
    val childBirthLon: Double,
    val childBirthTzOffsetHours: Double,
)

@Serializable
data class SponsorCreateResponse(
    val sponsorId: String,
    val inviteToken: String,
    val inviteExpiresAt: String,
)

/**
 * Mirrors app/schemas.py's SponsorProfileOut exactly. Used to omit
 * contactInfo (the backend response didn't have it, so decoding this
 * after a "I'm a parent" login would've crashed with the same
 * MissingFieldException class as the SuggestionOut bug this file was
 * fixed alongside) -- contactInfo is back now that
 * onboarding_service.get_sponsor_profile actually returns it, added
 * specifically to power the Settings screen's account header (name +
 * contact + account id), same as PrimaryProfileOut.contactInfo already
 * does for the child role.
 */
@Serializable
data class SponsorProfileOut(
    val sponsorId: String,
    val name: String,
    val contactInfo: String,
    val childName: String? = null,
    val childClaimed: Boolean,
)

@Serializable
data class InviteRedeemRequest(
    val token: String,
    val name: String,
    val contactInfo: String,
    val gender: String,
    val seekingGender: String,
    val maritalStatus: String,
    val location: String,
)

@Serializable
data class InviteRedeemResponse(
    val primaryId: String,
    val sponsorId: String,
)

@Serializable
data class ChildPreferencesRequest(
    val minAge: Int,
    val maxAge: Int,
    val minHeightCm: Int? = null,
    val maxHeightCm: Int? = null,
    val educationPref: List<String> = emptyList(),
    val professionPref: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val hobbiesPref: List<String> = emptyList(),
    val lifestyleTags: List<String> = emptyList(),
    val relationshipGoal: String,
)

@Serializable
data class ChildPreferencesResponse(
    val primaryId: String,
    val profileCompleted: Boolean,
    val pipelineTriggered: Boolean,
)

@Serializable
data class PrimaryProfileOut(
    val primaryId: String,
    val name: String,
    val contactInfo: String,
    val gender: String,
    val seekingGender: String,
    val maritalStatus: String,
    val location: String,
    val profileCompleted: Boolean,
    // Round-2 addition: shown as locked/disabled fields in Basic data --
    // already fed at onboarding, never editable post-hoc (see
    // PrimaryBasicDataIn -- these two are deliberately absent from it).
    val dob: String? = null,
    val tob: String? = null,
    // "Hide name" manual override + founding-member badge (reported
    // directly) -- see app/models/identity.py's Primary.hide_name_override/
    // founding_member on the backend. Defaulted so an older cached response
    // shape still deserializes.
    val hideNameOverride: Boolean = false,
    val isFoundingMember: Boolean = false,
)

@Serializable
data class HideNameIn(val hidden: Boolean)

@Serializable
data class HideNameOut(val primaryId: String, val hideNameOverride: Boolean)

@Serializable
data class PrimaryBasicDataIn(
    val name: String,
    val contactInfo: String,
    val gender: String,
    val seekingGender: String,
    val maritalStatus: String,
    val location: String,
)

@Serializable
data class PrimaryBasicDataOut(
    val primaryId: String,
    val name: String,
    val contactInfo: String,
    val gender: String,
    val seekingGender: String,
    val maritalStatus: String,
    val location: String,
)

@Serializable
data class PrimarySponsorOut(
    val sponsorId: String,
)

/** GET /primaries/search?code= result -- "provide an ID for each profile
 * for easy search & finding" (reported directly). Minimal preview card,
 * not a full candidate/match shape -- a short_id lookup isn't the
 * matching pipeline's output, just a direct lookup by a human-shared
 * code; ParentBrowseScreen navigates straight to ProfileDetailScreen on
 * a hit, which fetches the real candidate card itself. */
@Serializable
data class ShortIdSearchResultOut(
    val primaryId: String,
    val shortId: String,
    val name: String,
    val photoUrl: String? = null,
)

@Serializable
data class LocationPreferenceIn(
    val acceptLocations: List<String>,
    val locationIsHardFilter: Boolean,
)

@Serializable
data class LocationPreferenceOut(
    val sponsorId: String,
    val acceptLocations: List<String>,
    val locationIsHardFilter: Boolean,
)

@Serializable
data class PhotoOut(
    val photoId: String,
    val url: String,
    val sortOrder: Int,
)

/**
 * Mirrors ExtendedBioData / ExtendedBioDataIn / ExtendedBioDataOut exactly,
 * round-2 field set (see d2m_core_engine's own migration c5e9a3f7b1d4):
 * monthly income (amount+currency) instead of an annual-income band, four
 * sibling counts instead of a free-text summary, mother_tongue/complexion/
 * sect/gothram/other_languages backed by closed picklists (app/taxonomy.py,
 * mirrored client-side in ui/components/Taxonomy.kt), profile_managed_by
 * removed entirely. Every field nullable, matching ExtendedBioDataIn's
 * deliberately permissive validation (there is none -- this endpoint doubles
 * as an import/transcription tool, per that schema's own backend doc comment).
 */
@Serializable
data class ExtendedBioDataIn(
    // Basic & personal
    val heightCm: Int? = null,
    val complexion: String? = null,
    val motherTongue: String? = null,
    val otherLanguages: List<String>? = null,
    val bodyType: String? = null,
    // Religious & astrological
    val religion: String? = null,
    val casteCommunity: String? = null,
    val sect: String? = null,
    val gothram: String? = null,
    val horoscopeMatchPreference: String? = null,
    // Education & career
    val highestEducation: String? = null,
    val institution: String? = null,
    val occupationTitle: String? = null,
    val employer: String? = null,
    val employmentSector: String? = null,
    val monthlyIncomeAmount: Int? = null,
    val monthlyIncomeCurrency: String? = null,
    // Family background
    val fatherName: String? = null,
    val fatherOccupation: String? = null,
    val motherName: String? = null,
    val motherOccupation: String? = null,
    val elderBrothersCount: Int? = null,
    val youngerBrothersCount: Int? = null,
    val elderSistersCount: Int? = null,
    val youngerSistersCount: Int? = null,
    val nativity: String? = null,
    val familyType: String? = null,
    val familyValues: String? = null,
    val financialStatus: String? = null,
    // Location & contact
    val citizenshipStatus: String? = null,
)

@Serializable
data class ExtendedBioDataOut(
    val primaryId: String,
    val heightCm: Int? = null,
    val complexion: String? = null,
    val motherTongue: String? = null,
    val otherLanguages: List<String>? = null,
    val bodyType: String? = null,
    val religion: String? = null,
    val casteCommunity: String? = null,
    val sect: String? = null,
    val gothram: String? = null,
    val horoscopeMatchPreference: String? = null,
    val highestEducation: String? = null,
    val institution: String? = null,
    val occupationTitle: String? = null,
    val employer: String? = null,
    val employmentSector: String? = null,
    val monthlyIncomeAmount: Int? = null,
    val monthlyIncomeCurrency: String? = null,
    val fatherName: String? = null,
    val fatherOccupation: String? = null,
    val motherName: String? = null,
    val motherOccupation: String? = null,
    val elderBrothersCount: Int? = null,
    val youngerBrothersCount: Int? = null,
    val elderSistersCount: Int? = null,
    val youngerSistersCount: Int? = null,
    val nativity: String? = null,
    val familyType: String? = null,
    val familyValues: String? = null,
    val financialStatus: String? = null,
    val citizenshipStatus: String? = null,
    val updatedAt: String? = null,
)

/** One {prompt, answer} card -- see AboutMeDataIn's doc comment. */
@Serializable
data class AboutMePromptEntry(
    val prompt: String,
    val answer: String,
)

/**
 * Mirrors AboutMeData / AboutMeDataIn / AboutMeDataOut exactly -- the
 * Tinder/Bumble-style "About Me" tab, distinct from ExtendedBioData above
 * and from ChildPreferencesRequest's hobbies/lifestyleTags/relationshipGoal
 * (those already back the Preferences tab and matching_service's scoring;
 * About Me doesn't re-ask the same questions under new names). Unlike
 * ExtendedBioDataIn, the backend DOES validate this schema's closed-set
 * fields -- see app/schemas.py::AboutMeDataIn's own doc comment.
 */
@Serializable
data class AboutMeDataIn(
    val fitnessRoutine: String? = null,
    val sleepSchedule: String? = null,
    val pets: String? = null,
    val socialEnergy: String? = null,
    val aboutPrompts: List<AboutMePromptEntry> = emptyList(),
    val partnerQualities: List<String> = emptyList(),
    val whatMattersMost: String? = null,
    val careerAfterMarriage: String? = null,
    val livingArrangement: String? = null,
    val openToRelocation: String? = null,
    val favoriteCuisine: String? = null,
    val dreamDestination: String? = null,
    val loveLanguage: String? = null,
)

@Serializable
data class AboutMeDataOut(
    val primaryId: String,
    val fitnessRoutine: String? = null,
    val sleepSchedule: String? = null,
    val pets: String? = null,
    val socialEnergy: String? = null,
    val aboutPrompts: List<AboutMePromptEntry> = emptyList(),
    val partnerQualities: List<String> = emptyList(),
    val whatMattersMost: String? = null,
    val careerAfterMarriage: String? = null,
    val livingArrangement: String? = null,
    val openToRelocation: String? = null,
    val favoriteCuisine: String? = null,
    val dreamDestination: String? = null,
    val loveLanguage: String? = null,
    val updatedAt: String? = null,
)

/** Generic FastAPI error body shape: `{"detail": ...}`. */
@Serializable
data class ApiErrorBody(
    val detail: String? = null,
)
