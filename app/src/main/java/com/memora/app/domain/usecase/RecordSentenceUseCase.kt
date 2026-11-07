package com.memora.app.domain.usecase

import com.memora.app.domain.model.Intent
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.model.Modality
import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.ObjectResolution
import com.memora.app.domain.model.ParsedStatement
import com.memora.app.domain.model.PlacePath
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.repository.MoveOutcome
import com.memora.app.intelligence.IdentityResolver
import com.memora.app.intelligence.SentenceInterpreter
import com.memora.app.intelligence.TextNormalizer

/** Enough information to restore the previous location (undo keeps history). */
data class UndoToken(
    val itemId: String,
    val previousLocationId: String?,
    val previousPathDisplay: String?,
)

/**
 * Turns one user sentence into an action. This is the deterministic brain of
 * the app: language pipeline → identity resolution → validated transaction.
 *
 * The three identity outcomes (existing / new / uncertain) are kept separate
 * (assignment §2.5); uncertain statements and future plans never silently
 * become facts (assignment §8.4).
 */
class RecordSentenceUseCase(
    private val repository: ItemRepository,
) {

    sealed interface Outcome {
        data class Saved(val item: ItemSummary, val path: PlacePath?, val undo: UndoToken?) : Outcome
        data class Unchanged(val item: ItemSummary, val path: PlacePath?) : Outcome
        data class Answer(val text: String) : Outcome
        data class NeedsChoice(val candidates: List<ObjectCandidate>, val statement: ParsedStatement) : Outcome
        data class NeedsConfirmation(val statement: ParsedStatement) : Outcome
        data class NotRecorded(val reason: String) : Outcome
    }

    suspend fun execute(raw: String): Outcome {
        val statement = SentenceInterpreter.parse(raw)
            ?: return Outcome.NotRecorded(
                "I did not catch that. Try a sentence like: I put my car keys in my drawer in bedroom.",
            )

        return when (statement.modality) {
            Modality.INTENTION, Modality.HYPOTHETICAL ->
                Outcome.NotRecorded("That sounds like a plan or a guess, so nothing was recorded.")

            Modality.UNCERTAIN ->
                when (statement.intent) {
                    Intent.SET_LOCATION -> Outcome.NeedsConfirmation(statement)
                    Intent.FIND_ITEM -> executeStatement(statement)
                    else -> Outcome.NotRecorded("Nothing was recorded.")
                }

            Modality.STATEMENT -> executeStatement(statement)
        }
    }

    /** User picked an existing item from the ambiguity dialog (chosenId null = new item). */
    suspend fun executeChosen(statement: ParsedStatement, chosenId: String?): Outcome =
        if (chosenId == null) {
            createAndPlace(statement, confirmed = true)
        } else {
            moveExisting(statement, chosenId, confirmed = true)
        }

    /** User answered the uncertainty prompt. */
    suspend fun executeConfirmed(statement: ParsedStatement, confirmed: Boolean): Outcome =
        if (statement.intent == Intent.SET_LOCATION) {
            when (val resolution = resolve(statement)) {
                is ObjectResolution.Existing -> moveExisting(statement, resolution.candidate.id, confirmed)
                is ObjectResolution.Ambiguous -> Outcome.NeedsChoice(resolution.candidates, statement)
                ObjectResolution.New -> createAndPlace(statement, confirmed)
            }
        } else {
            Outcome.NotRecorded("Nothing was recorded.")
        }

    private suspend fun executeStatement(statement: ParsedStatement): Outcome {
        if (statement.itemPhrase.isNullOrBlank()) {
            return Outcome.NotRecorded("I could not tell which item you mean.")
        }
        return when (statement.intent) {
            Intent.FIND_ITEM -> answer(statement.itemPhrase)

            Intent.SET_LOCATION, Intent.MOVE_ITEM ->
                when (val resolution = resolve(statement)) {
                    is ObjectResolution.Existing ->
                        moveExisting(statement, resolution.candidate.id, confirmed = true)
                    is ObjectResolution.Ambiguous ->
                        Outcome.NeedsChoice(resolution.candidates, statement)
                    ObjectResolution.New ->
                        createAndPlace(statement, confirmed = true)
                }

            Intent.UNKNOWN ->
                Outcome.NotRecorded("I did not understand that yet. Try: I put my car keys in my drawer in bedroom.")
        }
    }

    private suspend fun resolve(statement: ParsedStatement): ObjectResolution {
        val phrase = statement.itemPhrase ?: return ObjectResolution.New
        val candidates = repository.findCandidates(TextNormalizer.matchKey(phrase))
        return IdentityResolver.resolve(phrase, candidates)
    }

    private suspend fun moveExisting(
        statement: ParsedStatement,
        itemId: String,
        confirmed: Boolean,
    ): Outcome {
        val outcome = repository.moveItem(
            itemId = itemId,
            placeChain = statement.placeChain,
            eventTime = eventTimeOf(statement),
            recordedTime = System.currentTimeMillis(),
            source = "SENTENCE",
            sentence = statement.raw,
            confirmed = confirmed,
            confidence = if (confirmed) 1.0 else 0.5,
        )
        return when (outcome) {
            is MoveOutcome.Moved -> {
                val summary = repository.itemSummary(itemId)
                    ?: return Outcome.NotRecorded("Something went wrong.")
                Outcome.Saved(
                    item = summary,
                    path = outcome.path,
                    undo = UndoToken(
                        itemId = itemId,
                        previousLocationId = outcome.previousLocationId,
                        previousPathDisplay = outcome.previousPathDisplay,
                    ),
                )
            }
            is MoveOutcome.Unchanged -> {
                val summary = repository.itemSummary(itemId)
                    ?: return Outcome.NotRecorded("Something went wrong.")
                Outcome.Unchanged(summary, outcome.path)
            }
            MoveOutcome.NotFound -> Outcome.NotRecorded("That item no longer exists.")
        }
    }

    private suspend fun createAndPlace(statement: ParsedStatement, confirmed: Boolean): Outcome {
        val name = statement.itemPhrase ?: return Outcome.NotRecorded("I could not tell which item you mean.")
        val item = repository.createItem(
            name = name,
            description = null,
            placeChain = statement.placeChain,
            eventTime = eventTimeOf(statement),
            recordedTime = System.currentTimeMillis(),
            source = "SENTENCE",
            sentence = statement.raw,
            confirmed = confirmed,
        )
        val summary = repository.itemSummary(item.id)
            ?: return Outcome.NotRecorded("Something went wrong.")
        return Outcome.Saved(summary, summary.currentPath, null)
    }

    private suspend fun answer(itemPhrase: String): Outcome {
        val candidates = repository.findCandidates(TextNormalizer.matchKey(itemPhrase))
        return when (candidates.size) {
            0 -> Outcome.Answer("I do not know an item named “$itemPhrase” yet.")
            else -> Outcome.Answer(
                candidates.joinToString("\n") { c ->
                    val where = c.currentPathDisplay ?: "location unknown"
                    "“${c.name}” — $where"
                },
            )
        }
    }

    private fun eventTimeOf(statement: ParsedStatement): Long =
        System.currentTimeMillis() + (statement.eventTimeOffsetMs ?: 0L)
}
