package com.memora.app.feature.add

import androidx.lifecycle.SavedStateHandle
import com.memora.app.core.navigation.MemoraDestination
import com.memora.app.domain.model.AddMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AddViewModelTest {

    @Test
    fun `default mode is ITEM`() {
        val viewModel = AddViewModel(SavedStateHandle())

        assertEquals(AddMode.ITEM, viewModel.initialMode)
        assertEquals(AddMode.ITEM, viewModel.uiState.value.mode)
    }

    @Test
    fun `navigation argument DOCUMENT opens the screen in document mode`() {
        val handle = SavedStateHandle(
            mapOf(MemoraDestination.Add.MODE_ARG to AddMode.DOCUMENT.name),
        )

        val viewModel = AddViewModel(handle)

        assertEquals(AddMode.DOCUMENT, viewModel.initialMode)
        assertEquals(AddMode.DOCUMENT, viewModel.uiState.value.mode)
    }

    @Test
    fun `invalid navigation argument falls back to ITEM without crashing`() {
        val handle = SavedStateHandle(
            mapOf(MemoraDestination.Add.MODE_ARG to "NOT_A_MODE"),
        )

        val viewModel = AddViewModel(handle)

        assertEquals(AddMode.ITEM, viewModel.initialMode)
    }

    @Test
    fun `selecting a mode updates the ui state`() {
        val viewModel = AddViewModel(SavedStateHandle())

        viewModel.onModeSelected(AddMode.DOCUMENT)
        assertEquals(AddMode.DOCUMENT, viewModel.uiState.value.mode)

        viewModel.onModeSelected(AddMode.ITEM)
        assertEquals(AddMode.ITEM, viewModel.uiState.value.mode)
    }
}
