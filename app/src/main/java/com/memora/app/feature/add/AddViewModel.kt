package com.memora.app.feature.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.memora.app.core.navigation.MemoraDestination
import com.memora.app.domain.model.AddMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI state of the Add screen. */
data class AddUiState(
    val mode: AddMode = AddMode.ITEM,
)

/**
 * ViewModel for the Add screen (MVVM pattern established here; data-backed
 * ViewModels arrive with the database in Phase 2).
 *
 * The initial mode comes from navigation (e.g. Home → "Save document" opens
 * the screen in [AddMode.DOCUMENT]); afterwards the user can switch freely.
 * An unknown or missing argument falls back to [AddMode.ITEM] — an invalid
 * value must never crash the app.
 */
class AddViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val initialMode: AddMode = savedStateHandle.get<String>(MemoraDestination.Add.MODE_ARG)
        ?.let { raw -> AddMode.entries.firstOrNull { it.name == raw } }
        ?: AddMode.ITEM

    private val _uiState = MutableStateFlow(AddUiState(mode = initialMode))
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    fun onModeSelected(mode: AddMode) {
        _uiState.update { it.copy(mode = mode) }
    }
}
