package com.memora.app.domain.model

/** What the user wants to do. Designed for extension (assignment §8.2). */
enum class Intent {
    SET_LOCATION,
    MOVE_ITEM,
    FIND_ITEM,
    UNKNOWN,
}

/**
 * How certain the statement is. This is separate from speech-recognition
 * confidence and from object-identity confidence (assignment §8.1).
 *
 * - STATEMENT: a completed fact — record it.
 * - UNCERTAIN: the user is guessing ("I think I left…") — ask before recording.
 * - INTENTION: a future plan ("I will move…") — never record as fact.
 * - HYPOTHETICAL: conditional ("If I put…") — never record as fact.
 */
enum class Modality { STATEMENT, UNCERTAIN, INTENTION, HYPOTHETICAL }

/** The result of the local language pipeline — before any database write. */
data class ParsedStatement(
    val raw: String,
    val normalized: String,
    val intent: Intent,
    val modality: Modality,
    /** Display name of the item, e.g. "car keys" (determiners already stripped). */
    val itemPhrase: String?,
    /**
     * Place chain innermost-first, e.g. ["drawer", "bedroom"] for
     * "my drawer in bedroom" — the drawer lives inside the bedroom.
     */
    val placeChain: List<String>,
    /** Offset in ms from "now" for when the event actually happened (e.g. yesterday). */
    val eventTimeOffsetMs: Long?,
)

/** Candidate for identity resolution. Never ranked by recency. */
data class ObjectCandidate(
    val id: String,
    val name: String,
    val description: String?,
    val currentPathDisplay: String?,
)

/**
 * Identity resolution outcome — three separate cases (assignment §2.5):
 * an existing object, a new physical object, or an uncertain identity.
 */
sealed interface ObjectResolution {
    data class Existing(val candidate: ObjectCandidate) : ObjectResolution
    data class Ambiguous(val candidates: List<ObjectCandidate>) : ObjectResolution
    data object New : ObjectResolution
}
