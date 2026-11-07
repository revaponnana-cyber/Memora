package com.memora.app.intelligence

import com.memora.app.domain.model.Intent
import com.memora.app.domain.model.Modality

/**
 * Rule-based intent and modality classification (assignment §8).
 *
 * Modality is essential: statements of completed actions are recorded,
 * guesses are confirmed first, future plans and hypotheticals are NEVER
 * recorded as facts (assignment §8.4).
 *
 * Pure Kotlin — unit-tested on the JVM.
 */
object IntentClassifier {

    private val findPatterns = listOf(
        Regex("""^where (?:is|are)\s"""),
        Regex("""^(?:show|find)\s+(?:me\s+)?"""),
        Regex("""^(?:i\s+)?(?:cannot|can not)\s+find\s"""),
    )

    private val setLocationPatterns = listOf(
        Regex("""^(?:i|we)\s+(?:just\s+)?(?:have\s+)?(?:put|placed|kept|left|stored|stashed|hid|hidden|dropped|threw|found)\s"""),
        Regex("""^(?:i|we)\s+(?:just\s+)?(?:have\s+)?moved\s"""),
        Regex("""^(?:my|the|a|an)?\s*\S.*\s+(?:is|are|was|were)\s+(?:in(?:side)?|on|at|under|near)\s"""),
    )

    private val hypotheticalMarkers = listOf(
        "what if", "if i ", "if we ", "if he ", "if she ", "if they ", "if it ",
        "imagine ", "imagine if", "suppose ", "supposing", "i would ", "would put",
        "would have", "could just",
    )

    private val uncertainPastMarkers = listOf(
        "might have", "may have", "must have", "could have", "should have",
        "i think i", "i thought i", "i believe i", "i guess i", "i suppose i",
        "maybe i", "probably i", "i probably", "not sure where", "seems like i",
    )

    private val intentionMarkers = listOf(
        " will ", "i will", "will put", "will move", "will take", "will keep",
        "shall ", "going to", "gonna", "plan to", "planning to", "want to",
        "would like to", "hope to", "decide to", "tomorrow", "later",
        "next week", "next month", "next time", "someday", "i might ",
        "i may ", "thinking of", "about to",
    )

    private val uncertainSimpleMarkers = listOf(
        "maybe", "probably", "perhaps", "i think", "i thought", "i believe",
        "i guess", "i suppose", "not certain", "unsure", "i am not sure",
        "must be", "might be", "may be", "could be",
    )

    fun intentOf(normalized: String): Intent = when {
        findPatterns.any { it.containsMatchIn(normalized) } -> Intent.FIND_ITEM
        setLocationPatterns.any { it.containsMatchIn(normalized) } -> Intent.SET_LOCATION
        else -> Intent.UNKNOWN
    }

    fun modalityOf(normalized: String): Modality = when {
        hypotheticalMarkers.any { normalized.contains(it) } -> Modality.HYPOTHETICAL
        uncertainPastMarkers.any { normalized.contains(it) } -> Modality.UNCERTAIN
        intentionMarkers.any { normalized.contains(it) } -> Modality.INTENTION
        uncertainSimpleMarkers.any { normalized.contains(it) } -> Modality.UNCERTAIN
        else -> Modality.STATEMENT
    }
}
