package com.memora.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.domain.model.ItemWithPlaces
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.repository.MoveOutcome
import com.memora.app.domain.usecase.MoveItemUseCase
import com.memora.app.intelligence.EntityExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Detail of one physical item: where it is now (highlighted), and each
 * distinct place it has been before — one entry per address, never repeated.
 */
class ItemDetailViewModel(
    repository: ItemRepository,
    private val moveItem: MoveItemUseCase,
    private val itemId: String,
) : ViewModel() {

    val detail: StateFlow<ItemWithPlaces?> = repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var moveJob: Job? = null

    /** Manual move from this screen. "drawer in bedroom" → nested chain. */
    fun onMove(placeText: String) {
        val chain = EntityExtractor.parsePlaceChain(placeText)
        if (chain.isEmpty()) {
            _message.value = "Type where it is now, e.g. drawer in bedroom."
            return
        }
        moveJob?.cancel()
        moveJob = viewModelScope.launch {
            _message.value = when (val outcome = moveItem(itemId, chain)) {
                is MoveOutcome.Moved -> {
                    val where = outcome.path?.display() ?: "unknown location"
                    "Moved to $where."
                }
                is MoveOutcome.Unchanged -> {
                    val where = outcome.path?.display() ?: "its current place"
                    "It is already at $where — no movement recorded."
                }
                MoveOutcome.NotFound -> "That item no longer exists."
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }
}
