package com.memora.app.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.domain.model.AddMode
import com.memora.app.domain.usecase.AddItemUseCase
import com.memora.app.intelligence.EntityExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state of the Add screen (manual fallback for the sentence input). */
data class AddUiState(
    val mode: AddMode = AddMode.ITEM,
    val name: String = "",
    val description: String = "",
    val place: String = "",
    val message: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/**
 * ViewModel for manual item creation. The primary way to add items is the
 * sentence input on Home ("I put my car keys in my drawer in bedroom");
 * this screen is the explicit fallback.
 */
class AddViewModel(
    private val addItem: AddItemUseCase,
    initialMode: AddMode,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState(mode = initialMode))
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    fun onModeSelected(mode: AddMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, message = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onPlaceChange(value: String) {
        _uiState.update { it.copy(place = value, message = null) }
    }

    fun onSave() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(message = "Type an item name first.") }
            return
        }
        val chain = EntityExtractor.parsePlaceChain(state.place.trim())
        viewModelScope.launch {
            addItem(name, state.description.trim().takeIf { it.isNotEmpty() }, chain)
            _uiState.update {
                it.copy(message = "Saved.", saved = true)
            }
        }
    }

    /** Called after the UI handled the successful save (e.g. navigated away). */
    fun onSavedHandled() {
        _uiState.update { it.copy(saved = false) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
