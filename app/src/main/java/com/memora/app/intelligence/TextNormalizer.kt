package com.memora.app.intelligence

/**
 * Text normalization for the local language pipeline (assignment §8):
 * lowercase, contractions, determiners, light singularization.
 *
 * Pure Kotlin — unit-tested on the JVM.
 */
object TextNormalizer {

    private val contractions = mapOf(
        "i'm" to "i am",
        "i've" to "i have",
        "i'll" to "i will",
        "i'd" to "i would",
        "it's" to "it is",
        "that's" to "that is",
        "there's" to "there is",
        "where's" to "where is",
        "what's" to "what is",
        "let's" to "let us",
        "won't" to "will not",
        "can't" to "cannot",
        "don't" to "do not",
        "didn't" to "did not",
        "doesn't" to "does not",
        "isn't" to "is not",
        "aren't" to "are not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "haven't" to "have not",
        "hasn't" to "has not",
    )

    private val determiner = Regex("""^(?:my|the|a|an|our|his|her)\s+""")

    /** Lowercases, expands contractions, strips noise punctuation, collapses space. */
    fun normalize(raw: String): String {
        var s = raw.lowercase().trim()
        s = s.replace(Regex("""[.!?;:]+$"""), "")
        s = s.replace(",", " ")
        contractions.forEach { (from, to) -> s = s.replace(from, to) }
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s
    }

    /** Strips a leading determiner: "my car keys" → "car keys". */
    fun stripDeterminer(phrase: String): String =
        phrase.trim().replace(determiner, "").trim()

    /**
     * Match key for identity lookups: normalized + lightly singularized so
     * "car keys" and "car key" match the same physical object. Display names
     * are never changed and similar names are never merged — two objects can
     * still both match, which forces a user choice.
     */
    fun matchKey(name: String): String =
        normalize(stripDeterminer(name))
            .split(" ")
            .joinToString(" ") { singularize(it) }

    private fun singularize(word: String): String =
        if (word.length > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            word.dropLast(1)
        } else {
            word
        }

    // ------------------------------------------------------------------
    // Simple past-time expressions ("yesterday", "last week"…)
    // ------------------------------------------------------------------

    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR

    private val timeSuffixes = listOf(
        "yesterday" to -DAY,
        "last night" to -DAY,
        "last week" to -7 * DAY,
        "last month" to -30 * DAY,
        "an hour ago" to -HOUR,
        "a few hours ago" to -3 * HOUR,
        "earlier today" to -HOUR,
        "earlier" to -HOUR,
        "today" to 0L,
        "this morning" to 0L,
        "this afternoon" to 0L,
        "this evening" to 0L,
        "just now" to 0L,
        "a moment ago" to 0L,
        "now" to 0L,
    )

    /**
     * Extracts a trailing time expression. Returns the sentence with the time
     * phrase removed plus the offset in ms (null when no time phrase).
     * "I moved my charger yesterday" → ("i moved my charger", -1 day).
     */
    fun extractTimeOffsetMs(normalized: String): Pair<String, Long?> {
        for ((suffix, offset) in timeSuffixes) {
            if (normalized == suffix) return "" to offset
            if (normalized.endsWith(" $suffix")) {
                return normalized.dropLast(suffix.length + 1).trim() to offset
            }
        }
        return normalized to null
    }

    /**
     * Removes leading hedging/plan prefixes so the core sentence can be
     * matched: "i think i left my charger in the kitchen" →
     * "i left my charger in the kitchen". Modality is detected separately
     * (before this runs) — this only unlocks intent + entity extraction.
     */
    private val modalityPrefixes = listOf(
        "i think i ", "i thought i ", "i believe i ", "i guess i ", "i suppose i ",
        "maybe i ", "perhaps i ", "probably i ", "i probably ",
        "i might have ", "i may have ", "i must have ", "i could have ", "i should have ",
    )

    fun stripModalityPrefix(normalized: String): String {
        var s = normalized
        for (prefix in modalityPrefixes) {
            if (s.startsWith(prefix)) {
                s = "i " + s.removePrefix(prefix)
                break
            }
        }
        return s
    }
}
