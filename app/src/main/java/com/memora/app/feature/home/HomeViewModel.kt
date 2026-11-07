package com.memora.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.ParsedStatement
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.usecase.RecordSentenceUseCase
import com.memora.app.domain.usecase.UndoMoveUseCase
import com.memora.app.domain.usecase.UndoToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off feedback shown under the sentence input. */
sealed interface HomeEvent {
    data class Saved(val message: String, val undo: UndoToken?) : HomeEvent
    data class Info(val message: String) : HomeEvent
    data class Answer(val text: String) : HomeEvent
}

data class AmbiguityRequest(
    val candidates: List<ObjectCandidate>,
    val statement: ParsedStatement,
)

data class UncertaintyRequest(
    val statement: ParsedStatement,
)

/**
 * Home = the whole app for most users: one sentence in, items list out.
 *
 * The ViewModel owns the interaction loop of the sentence agent:
 * submit → Saved / Answer / NeedsChoice / NeedsConfirmation / NotRecorded.
 */
class HomeViewModel(
    repository: ItemRepository,
    private val recordSentence: RecordSentenceUseCase,
    private val undoMove: UndoMoveUseCase,
) : ViewModel() {

    val items: StateFlow<List<ItemSummary>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _event = MutableStateFlow<HomeEvent?>(null)
    val event: StateFlow<HomeEvent?> = _event.asStateFlow()

    private val _ambiguity = MutableStateFlow<AmbiguityRequest?>(null)
    val ambiguity: StateFlow<AmbiguityRequest?> = _ambiguity.asStateFlow()

    private val _uncertainty = MutableStateFlow<UncertaintyRequest?>(null)
    val uncertainty: StateFlow<UncertaintyRequest?> = _uncertainty.asStateFlow()

    fun onSentenceSubmit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            apply(recordSentence.execute(trimmed))
        }
    }

    /** User picked one of the same-named objects (id null = "add as new"). */
    fun onCandidateChosen(candidateId: String?) {
        val request = _ambiguity.value ?: return
        _ambiguity.value = null
        viewModelScope.launch {
            apply(recordSentence.executeChosen(request.statement, candidateId))
        }
    }

    /** User backed out of the ambiguity dialog — record nothing. */
    fun onAmbiguityDismissed() {
        _ambiguity.value = null
    }

    /** User answered the uncertainty prompt (record confirmed/uncertain or cancel). */
    fun onUncertaintyAnswer(confirmed: Boolean?) {
        val request = _uncertainty.value ?: return
        _uncertainty.value = null
        if (confirmed == null) {
            _event.value = HomeEvent.Info("Nothing was recorded.")
            return
        }
        viewModelScope.launch {
            apply(recordSentence.executeConfirmed(request.statement, confirmed))
        }
    }

    fun onUndo(token: UndoToken) {
        _event.value = null
        viewModelScope.launch {
            undoMove(token)
            _event.value = HomeEvent.Info("Move undone. History was kept.")
        }
    }

    fun onEventShown() {
        _event.value = null
    }

    private fun apply(outcome: RecordSentenceUseCase.Outcome) {
        _event.value = when (outcome) {
            is RecordSentenceUseCase.Outcome.Saved -> {
                val where = outcome.path?.display() ?: "location unknown"
                val who = if (outcome.undo == null) {
                    "“${outcome.item.name}” is now recorded at $where."
                } else {
                    "“${outcome.item.name}” → $where."
                }
                HomeEvent.Saved(who, outcome.undo)
            }
            is RecordSentenceUseCase.Outcome.Unchanged -> {
                val where = outcome.path?.display() ?: "its current place"
                HomeEvent.Info("“${outcome.item.name}” is already at $where — nothing changed.")
            }
            is RecordSentenceUseCase.Outcome.Answer -> HomeEvent.Answer(outcome.text)
            is RecordSentenceUseCase.Outcome.NeedsChoice -> {
                _ambiguity.value = AmbiguityRequest(outcome.candidates, outcome.statement)
                null
            }
            is RecordSentenceUseCase.Outcome.NeedsConfirmation -> {
                _uncertainty.value = UncertaintyRequest(outcome.statement)
                null
            }
            is RecordSentenceUseCase.Outcome.NotRecorded -> HomeEvent.Info(outcome.reason)
        }
    }
}
