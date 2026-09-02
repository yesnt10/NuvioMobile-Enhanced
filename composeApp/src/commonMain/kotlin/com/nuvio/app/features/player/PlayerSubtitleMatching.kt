package com.nuvio.app.features.player

internal object SubtitleLanguageMatching {
    internal val BRAZILIAN_TAGS = listOf(
        "pt-br", "pt_br", "pob", "brazilian", "brazil", "brasil", "brasileiro", " br", "(br)",
    )
    internal val EUROPEAN_PT_TAGS = listOf(
        "pt-pt", "pt_pt", "iberian", "european", "portugal", "europeu", " eu", "(eu)",
    )
    internal val LATINO_TAGS = listOf(
        "es-419", "es_419", "es-la", "es-lat", "latino", "latinoamerica",
        "latinoamericano", "latam", "lat am", "latin america",
    )
    internal val CASTILIAN_TAGS = listOf(
        "es-es", "es_es", "castilian", "castellano", "spain", "espana", "iberian",
    )

    private val LANGUAGE_OVERRIDES = mapOf(
        "pt-pt" to "pt",
        "pt_pt" to "pt",
        "por" to "pt",
        "pt-br" to "pt-br",
        "pt_br" to "pt-br",
        "br" to "pt-br",
        "pob" to "pt-br",
        "fre" to "fr",
        "ger" to "de",
        "deu" to "de",
        "dut" to "nl",
        "nld" to "nl",
        "chi" to "zh",
        "zho" to "zh",
        "jpn" to "ja",
        "kor" to "ko",
        "ara" to "ar",
        "hin" to "hi",
        "rus" to "ru",
        "pol" to "pl",
        "spa" to "es",
        "spl" to "es-419",
        "es-419" to "es-419",
        "es_419" to "es-419",
        "es-la" to "es-419",
        "es-lat" to "es-419",
        "fra" to "fr",
        "ita" to "it",
        "eng" to "en",
        "swe" to "sv",
        "nor" to "no",
        "dan" to "da",
        "fin" to "fi",
        "tur" to "tr",
        "ell" to "el",
        "gre" to "el",
        "heb" to "he",
        "tha" to "th",
        "vie" to "vi",
        "ind" to "id",
        "msa" to "ms",
        "may" to "ms",
        "ces" to "cs",
        "cze" to "cs",
        "hun" to "hu",
        "ron" to "ro",
        "rum" to "ro",
        "ukr" to "uk",
        "bul" to "bg",
        "hrv" to "hr",
        "srp" to "sr",
        "slk" to "sk",
        "slo" to "sk",
        "slv" to "sl",
        "zht" to "zh-tw",
        "zhs" to "zh-cn",
        "zh-tw" to "zh-tw",
        "zh_tw" to "zh-tw",
        "zh-cn" to "zh-cn",
        "zh_cn" to "zh-cn",
    )

    private val LANGUAGE_NAMES = mapOf(
        "ar" to "arabic",
        "bg" to "bulgarian",
        "cs" to "czech",
        "da" to "danish",
        "de" to "german",
        "el" to "greek",
        "en" to "english",
        "es" to "spanish",
        "es-419" to "spanish",
        "fi" to "finnish",
        "fr" to "french",
        "he" to "hebrew",
        "hi" to "hindi",
        "hr" to "croatian",
        "hu" to "hungarian",
        "id" to "indonesian",
        "it" to "italian",
        "ja" to "japanese",
        "ko" to "korean",
        "nl" to "dutch",
        "no" to "norwegian",
        "pl" to "polish",
        "pt" to "portuguese",
        "pt-br" to "portuguese",
        "ro" to "romanian",
        "ru" to "russian",
        "sk" to "slovak",
        "sl" to "slovenian",
        "sr" to "serbian",
        "sv" to "swedish",
        "tr" to "turkish",
        "uk" to "ukrainian",
        "zh" to "chinese",
        "zh-cn" to "chinese",
        "zh-tw" to "chinese",
    )

    fun normalizeLanguageCode(lang: String): String {
        val code = lang.trim().lowercase()
        if (code.isBlank()) return ""
        val normalizedCode = code.replace('_', '-')
        val tokenized = normalizedCode
            .replace('-', ' ')
            .replace('.', ' ')
            .replace('/', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        fun containsAny(vararg values: String): Boolean =
            values.any { value -> tokenized.contains(value) }

        if (containsAny("portuguese", "portugues")) {
            return when {
                containsAny("brazil", "brasil", "brazilian", "brasileiro", "pt br", "ptbr", "pob", "(br)") -> "pt-br"
                containsAny("portugal", "european", "europeu", "iberian", "pt pt", "ptpt") -> "pt"
                else -> "pt"
            }
        }

        if (containsAny("spanish", "espanol", "castellano")) {
            return if (containsAny("latin", "latino", "latinoamerica", "latam", "lat am", "es 419", "es419")) {
                "es-419"
            } else {
                "es"
            }
        }

        return LANGUAGE_OVERRIDES[code] ?: normalizedCode
    }

    fun matchesLanguageCode(language: String?, target: String): Boolean {
        if (language.isNullOrBlank()) return false
        val normalizedLanguage = normalizeLanguageCode(language)
        val normalizedTarget = normalizeLanguageCode(target)
        return normalizedLanguage == normalizedTarget ||
            normalizedLanguage.startsWith("$normalizedTarget-") ||
            normalizedLanguage.startsWith("${normalizedTarget}_")
    }

    fun detectTrackLanguageVariant(language: String?, name: String?, trackId: String?): String {
        val baseLang = normalizeLanguageCode(language ?: "")
        val haystack = listOfNotNull(name, language, trackId).joinToString(" ").lowercase()
        if (baseLang == "pt" || baseLang == "por") {
            val hasBrazilian = BRAZILIAN_TAGS.any { haystack.contains(it) }
            val hasEuropean = EUROPEAN_PT_TAGS.any { haystack.contains(it) }
            if (hasBrazilian && !hasEuropean) return "pt-br"
            if (hasEuropean && !hasBrazilian) return "pt"
        }
        if (baseLang == "es" || baseLang == "spa") {
            val hasLatino = LATINO_TAGS.any { haystack.contains(it) }
            val hasCastilian = CASTILIAN_TAGS.any { haystack.contains(it) }
            if (hasLatino && !hasCastilian) return "es-419"
            if (hasCastilian && !hasLatino) return "es"
        }
        return baseLang
    }

    fun languageCodeToName(code: String): String {
        val normalized = normalizeLanguageCode(code)
        if (normalized == "none") return "none"
        if (normalized == "und" || normalized == "unknown" || normalized == "unk") return "unknown"
        return LANGUAGE_NAMES[normalized] ?: LANGUAGE_NAMES[normalized.substringBefore('-')] ?: normalized
    }

    fun languageCodeAppearsInHaystack(haystack: String, normalizedTarget: String): Boolean {
        if (normalizedTarget.isBlank()) return false
        var searchFrom = 0
        while (searchFrom <= haystack.length - normalizedTarget.length) {
            val matchIndex = haystack.indexOf(normalizedTarget, startIndex = searchFrom)
            if (matchIndex < 0) return false
            val before = matchIndex - 1
            val after = matchIndex + normalizedTarget.length
            val startsAtBoundary = before < 0 || !haystack[before].isLetterOrDigit()
            val endsAtBoundary = after >= haystack.length || !haystack[after].isLetterOrDigit()
            if (startsAtBoundary && endsAtBoundary) return true
            searchFrom = matchIndex + 1
        }
        return false
    }

    fun trackMatchesLanguage(name: String?, language: String?, trackId: String?, target: String): Boolean {
        if (matchesLanguageCode(language, target)) return true
        val normalizedTarget = normalizeLanguageCode(target)
        val targetName = languageCodeToName(target)
        val haystack = listOfNotNull(name, language, trackId).joinToString(" ").lowercase()
        return languageCodeAppearsInHaystack(haystack, normalizedTarget) ||
            (targetName.isNotBlank() && haystack.contains(targetName))
    }

    fun subtitleHasAnyTag(name: String?, language: String?, trackId: String?, tags: List<String>): Boolean {
        val haystack = listOfNotNull(name, language, trackId).joinToString(" ").lowercase()
        return tags.any { tag -> haystack.contains(tag) }
    }
}
