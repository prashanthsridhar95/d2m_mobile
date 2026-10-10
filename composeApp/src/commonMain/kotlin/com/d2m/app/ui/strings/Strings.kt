package com.d2m.app.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Root aggregate of every per-screen string table below (LoginStrings.kt,
 * OnboardingStrings.kt, ...), one property per screen/feature grouping --
 * same namespace split d2m_web's i18n/locales/ *.json files already use
 * (login, settings, trust, ...), so the two clients' translated copy stays
 * conceptually aligned even though this is plain Kotlin, not i18next.
 *
 * Every one of [EnStrings]/[HiStrings]/[TaStrings] implements every nested
 * interface in full -- unlike i18next's t(key, englishDefault) fallback
 * pattern, there is no "missing key falls back to English" mechanism here,
 * so a translation gap is a compile error (a HiStrings/TaStrings object
 * missing an override), not a silent runtime fallback. That tradeoff is
 * deliberate: it is the thing that makes "AppStrings is fully localized"
 * a property the compiler verifies on every build, not a fact someone has
 * to remember to keep true.
 */
interface AppStrings {
    val common: CommonStrings
    val login: LoginStrings
    val onboarding: OnboardingStrings
    val childHome: ChildHomeStrings
    val discovery: DiscoveryStrings
    val matches: MatchesStrings
    val childProfileDialog: ChildProfileDialogStrings
    val parentHome: ParentHomeStrings
    val parentBrowse: ParentBrowseStrings
    val profileDetail: ProfileDetailStrings
    val parentMessages: ParentMessagesStrings
    val settings: SettingsStrings
    val notifications: NotificationsStrings
    val trust: TrustStrings
    val shareLinks: ShareLinksStrings
    val profileLink: ProfileLinkStrings
    val identityVerification: IdentityVerificationStrings
    val parity: ParityStrings
    val sharedComponents: SharedComponentsStrings
}

object EnStrings : AppStrings {
    override val common = CommonStringsEn
    override val login = LoginStringsEn
    override val onboarding = OnboardingStringsEn
    override val childHome = ChildHomeStringsEn
    override val discovery = DiscoveryStringsEn
    override val matches = MatchesStringsEn
    override val childProfileDialog = ChildProfileDialogStringsEn
    override val parentHome = ParentHomeStringsEn
    override val parentBrowse = ParentBrowseStringsEn
    override val profileDetail = ProfileDetailStringsEn
    override val parentMessages = ParentMessagesStringsEn
    override val settings = SettingsStringsEn
    override val notifications = NotificationsStringsEn
    override val trust = TrustStringsEn
    override val shareLinks = ShareLinksStringsEn
    override val profileLink = ProfileLinkStringsEn
    override val identityVerification = IdentityVerificationStringsEn
    override val parity = ParityStringsEn
    override val sharedComponents = SharedComponentsStringsEn
}

object HiStrings : AppStrings {
    override val common = CommonStringsHi
    override val login = LoginStringsHi
    override val onboarding = OnboardingStringsHi
    override val childHome = ChildHomeStringsHi
    override val discovery = DiscoveryStringsHi
    override val matches = MatchesStringsHi
    override val childProfileDialog = ChildProfileDialogStringsHi
    override val parentHome = ParentHomeStringsHi
    override val parentBrowse = ParentBrowseStringsHi
    override val profileDetail = ProfileDetailStringsHi
    override val parentMessages = ParentMessagesStringsHi
    override val settings = SettingsStringsHi
    override val notifications = NotificationsStringsHi
    override val trust = TrustStringsHi
    override val shareLinks = ShareLinksStringsHi
    override val profileLink = ProfileLinkStringsHi
    override val identityVerification = IdentityVerificationStringsHi
    override val parity = ParityStringsHi
    override val sharedComponents = SharedComponentsStringsHi
}

object TaStrings : AppStrings {
    override val common = CommonStringsTa
    override val login = LoginStringsTa
    override val onboarding = OnboardingStringsTa
    override val childHome = ChildHomeStringsTa
    override val discovery = DiscoveryStringsTa
    override val matches = MatchesStringsTa
    override val childProfileDialog = ChildProfileDialogStringsTa
    override val parentHome = ParentHomeStringsTa
    override val parentBrowse = ParentBrowseStringsTa
    override val profileDetail = ProfileDetailStringsTa
    override val parentMessages = ParentMessagesStringsTa
    override val settings = SettingsStringsTa
    override val notifications = NotificationsStringsTa
    override val trust = TrustStringsTa
    override val shareLinks = ShareLinksStringsTa
    override val profileLink = ProfileLinkStringsTa
    override val identityVerification = IdentityVerificationStringsTa
    override val parity = ParityStringsTa
    override val sharedComponents = SharedComponentsStringsTa
}

fun stringsFor(locale: AppLocale): AppStrings = when (locale) {
    AppLocale.EN -> EnStrings
    AppLocale.HI -> HiStrings
    AppLocale.TA -> TaStrings
}

/**
 * Provided once at the App() root (App.kt) via CompositionLocalProvider, same
 * pattern as ui/components/ProfilePhoto.kt's LocalPhotoImageLoader -- every
 * screen reads `LocalStrings.current.<namespace>.<key>` instead of a literal
 * string. staticCompositionLocalOf (not compositionLocalOf): the value only
 * ever changes when the user explicitly switches language in Settings or on
 * LoginScreen, an infrequent, whole-tree-recompose-worthy event, not a
 * per-frame value -- static avoids the extra tracking overhead
 * compositionLocalOf pays for values that change often.
 *
 * Defaults to [EnStrings] (not an error-throwing default) so a @Preview or
 * a test composable that doesn't wrap itself in the real provider still
 * renders real English copy instead of crashing.
 */
val LocalStrings = staticCompositionLocalOf<AppStrings> { EnStrings }

/**
 * The raw [AppLocale] itself, provided alongside [LocalStrings] in App.kt's
 * root composable -- needed by Taxonomy.kt's toLabel(), which looks up a
 * canonical backend value's HI/TA transliteration/translation directly
 * (mirrors d2m_web's lib/taxonomy.js toLabel(value, lang) reading
 * i18n.language) rather than going through a per-screen AppStrings table,
 * since taxonomy values aren't scoped to any one screen's namespace.
 */
val LocalAppLocale = staticCompositionLocalOf<AppLocale> { AppLocale.EN }
