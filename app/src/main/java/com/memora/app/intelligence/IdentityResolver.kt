package com.memora.app.intelligence

import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.ObjectResolution

/**
 * Object identity resolution (assignment §2.5 — the core rules):
 *
 * 1. Explicit user selection always wins (see RecordSentenceUseCase).
 * 2. Exactly one strongly matching object → select it.
 * 3. Contextual disambiguation (later phases).
 * 4. Multiple candidates → ask the user. NEVER pick by recency.
 * 5. No candidate → this is a NEW physical object (new UUID, same name OK).
 *
 * Never merges records; never invents descriptions.
 *
 * Pure Kotlin — unit-tested on the JVM.
 */
object IdentityResolver {

    fun resolve(itemPhrase: String, candidates: List<ObjectCandidate>): ObjectResolution {
        val key = TextNormalizer.matchKey(itemPhrase)
        val matches = candidates.filter { TextNormalizer.matchKey(it.name) == key }
        return when (matches.size) {
            0 -> ObjectResolution.New
            1 -> ObjectResolution.Existing(matches.first())
            else -> ObjectResolution.Ambiguous(matches)
        }
    }
}
