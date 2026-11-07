package com.memora.app.intelligence

/**
 * Entity extraction: pulls the item phrase and the place chain out of a
 * normalized sentence.
 *
 * "i put my car keys in my drawer in bedroom"
 *   → item "car keys", chain ["drawer", "bedroom"]   (drawer INSIDE bedroom)
 *
 * "i put my car keys at the window"
 *   → item "car keys", chain ["window"]
 *
 * Pure Kotlin — unit-tested on the JVM.
 */
object EntityExtractor {

    data class Extraction(
        val itemPhrase: String,
        /** Innermost place first: ["drawer", "bedroom"]. */
        val placeChain: List<String>,
    )

    private val patterns = listOf(
        // "I moved my X to the Y" / "I took my X to Y"
        Regex("""^(?:i|we)\s+(?:just\s+)?(?:have\s+)?(?:moved|took)\s+(.+?)\s+(?:to|into)\s+(.+)$"""),
        // "I put/left/stored/kept/found my X in/on/at the Y"
        Regex("""^(?:i|we)\s+(?:just\s+)?(?:have\s+)?(?:put|placed|kept|left|stored|stashed|hid|hidden|dropped|threw|found)\s+(.+?)\s+(?:in(?:to|side)?|on(?:to)?|at|under|near|next to|by)\s+(.+)$"""),
        // "My X is/was in/on/at the Y"
        Regex("""^(?:my|the|a|an)?\s*(.+?)\s+(?:is|are|was|were)\s+(?:in(?:side)?|on|at|under|near)\s+(.+)$"""),
    )

    /** Location questions carry only the item: "where is my passport". */
    private val findPatterns = listOf(
        Regex("""^where (?:is|are)\s+(.+)$"""),
        Regex("""^(?:show|find)\s+(?:me\s+)?(.+)$"""),
        Regex("""^(?:i\s+)?(?:cannot|can not)\s+find\s+(.+)$"""),
    )

    fun extract(normalized: String): Extraction? {
        for (pattern in findPatterns) {
            val match = pattern.matchEntire(normalized) ?: continue
            val item = TextNormalizer.stripDeterminer(match.groupValues[1])
            if (item.isNotBlank()) return Extraction(item, emptyList())
        }
        for (pattern in patterns) {
            val match = pattern.matchEntire(normalized) ?: continue
            val item = TextNormalizer.stripDeterminer(match.groupValues[1])
            val chain = parsePlaceChain(match.groupValues[2])
            if (item.isNotBlank() && chain.isNotEmpty()) {
                return Extraction(item, chain)
            }
        }
        return null
    }

    /**
     * Parses a place expression into a chain (innermost first).
     * Nesting is expressed with "in"/"inside": "drawer in bedroom" →
     * ["drawer", "bedroom"]. "the table in the kitchen" → ["table", "kitchen"].
     */
    fun parsePlaceChain(placeExpression: String): List<String> {
        val segments = placeExpression
            .split(Regex("""\s+(?:in(?:side)?)\s+"""))
            .map { TextNormalizer.stripDeterminer(it) }
            .filter { it.isNotBlank() }
        return segments
    }
}
