package com.memora.app.intelligence

import com.memora.app.domain.model.Intent
import com.memora.app.domain.model.Modality
import com.memora.app.domain.model.ParsedStatement

/**
 * The local "agent" pipeline (assignment §8.1), tuned for remembering items:
 *
 *   text → normalize → intent + modality → entity extraction → ParsedStatement
 *
 * It is deterministic (rules + dictionaries), fully offline, tiny, and runs
 * ONLY when the user submits a sentence. Identity resolution and any database
 * write happen afterwards in domain use cases — the pipeline itself never
 * touches storage.
 */
object SentenceInterpreter {

    fun parse(raw: String): ParsedStatement? {
        val normalized = TextNormalizer.normalize(raw)
        if (normalized.isBlank()) return null

        val (withoutTime, timeOffset) = TextNormalizer.extractTimeOffsetMs(normalized)
        val modality = IntentClassifier.modalityOf(withoutTime)
        // Hedging wraps the action ("i think i left…"); strip it for structure.
        val structure = TextNormalizer.stripModalityPrefix(withoutTime)
        val intent = IntentClassifier.intentOf(structure)
        val extraction = EntityExtractor.extract(structure)

        return ParsedStatement(
            raw = raw.trim(),
            normalized = normalized,
            intent = if (extraction != null && intent == Intent.UNKNOWN) Intent.SET_LOCATION else intent,
            modality = modality,
            itemPhrase = extraction?.itemPhrase,
            placeChain = extraction?.placeChain ?: emptyList(),
            eventTimeOffsetMs = timeOffset,
        )
    }
}
