package com.d2m.app.ui.components

/**
 * Direct Kotlin mirror of d2m_core_engine/app/taxonomy.py (and d2m_web's own
 * lib/taxonomy.js mirror of it) -- illustrative, not exhaustive, per that
 * file's own docstring. Kept in sync by hand, same as the other two copies.
 */
object Taxonomy {
    val RELIGIONS = listOf("Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi", "Other")

    val COMMUNITIES = listOf(
        "iyer", "iyengar", "brahmin_other", "gsb", "bengali_brahmin",
        "kashmiri_pandit", "saraswat", "nair", "menon", "ezhava", "nadar",
        "mudaliar", "pillai", "vellalar", "chettiar", "gounder", "reddy",
        "naidu", "kamma", "kapu", "velama", "vokkaliga", "lingayat",
        "bunt", "kayastha", "rajput", "jat", "gujjar", "yadav", "kurmi",
        "khatri", "agarwal", "baniya", "maratha", "patel", "sindhi",
        "punjabi_other", "bengali_other", "anglo_indian", "sc_st", "other",
    )

    val MARITAL_STATUSES = listOf("never_married", "divorced", "widowed", "annulled")
    val GENDERS = listOf("male", "female", "non_binary")
    val EDUCATION_LEVELS = listOf("high_school", "bachelors", "masters", "doctorate", "professional_degree")
    val PROFESSION_TIERS = listOf("student", "early_career", "mid_career", "senior", "business_owner", "not_working")
    val INCOME_BANDS = listOf("under_5l", "5l_10l", "10l_25l", "25l_50l", "50l_plus")

    val HOBBIES = listOf(
        "reading", "travel", "cooking", "fitness", "music", "movies", "hiking",
        "photography", "gaming", "volunteering", "sports", "dancing", "art", "yoga", "pets",
    )
    val LIFESTYLE_TAGS = listOf(
        "non_smoker", "smoker", "non_drinker", "social_drinker", "vegetarian",
        "non_vegetarian", "vegan", "wants_children", "open_on_children", "no_children_wanted",
    )
    val RELATIONSHIP_GOALS = listOf("marriage_ready", "marriage_within_a_year", "open_to_long_courtship")

    val BODY_TYPES = listOf("slim", "athletic", "average", "heavy")
    val HOROSCOPE_MATCH_PREFERENCES = listOf("required", "preferred", "not_required")
    val EMPLOYMENT_SECTORS = listOf("private", "government_psu", "business", "self_employed", "not_working")
    val FAMILY_TYPES = listOf("nuclear", "joint")
    val FAMILY_VALUES = listOf("traditional", "moderate", "liberal")
    val CITIZENSHIP_STATUSES = listOf("citizen", "permanent_resident", "work_visa", "student_visa", "other")

    // Display-ready values (proper nouns/codes), rendered as-is, no toLabel().
    val COMPLEXIONS = listOf("Very Fair", "Fair", "Wheatish", "Wheatish Brown", "Dark")
    val LANGUAGES = listOf(
        "Tamil", "Telugu", "Kannada", "Malayalam", "Hindi", "Marathi", "Gujarati",
        "Bengali", "Punjabi", "Urdu", "Odia", "Assamese", "Konkani", "Sindhi",
        "Kashmiri", "Maithili", "Bhojpuri", "Tulu", "Sanskrit", "Nepali", "English", "Other",
    )
    val GOTHRAMS = listOf(
        "Bharadwaja", "Kashyapa", "Vashishta", "Kaushika", "Atreya", "Vishwamitra",
        "Shrivatsa", "Gautama", "Agastya", "Koundinya", "Shandilya", "Harita",
        "Vatsa", "Angirasa", "Parashara", "Jamadagni", "Kutsa", "Sankriti",
        "Marichi", "Pulastya", "Bhrigu", "Other",
    )
    val SECTS = listOf(
        "Vadama", "Brahacharanam", "Vathima", "Ashtasahasram", "Vadadesha",
        "Vadakalai", "Thenkalai", "Smartha", "Madhwa", "Shaiva", "Vaishnava",
        "Not applicable", "Other",
    )
    val CURRENCIES = listOf("INR", "USD", "GBP", "EUR", "AED", "CAD", "AUD", "SGD")

    private val ACRONYMS = mapOf("usa" to "USA", "uk" to "UK", "ncr" to "NCR")

    /** "5l_10l" -> "5L - 10L"-ish, "usa_bay_area" -> "Usa Bay Area" -- same simple title-casing as toLabel() on web. */
    fun toLabel(value: String): String =
        value.split("_").joinToString(" ") { part ->
            ACRONYMS[part] ?: part.replaceFirstChar { it.uppercase() }
        }
}
