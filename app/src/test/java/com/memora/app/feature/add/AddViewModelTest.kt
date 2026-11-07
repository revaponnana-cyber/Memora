package com.memora.app.feature.add

import com.memora.app.domain.model.AddMode
import com.memora.app.domain.usecase.AddItemUseCase
import com.memora.app.domain.usecase.FakeItemRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddViewModelTest {

    private fun viewModel(mode: AddMode = AddMode.ITEM): AddViewModel =
        AddViewModel(AddItemUseCase(FakeItemRepository()), mode)

    @Test
    fun `default mode is ITEM`() {
        val viewModel = viewModel()

        assertEquals(AddMode.ITEM, viewModel.uiState.value.mode)
    }

    @Test
    fun `navigation argument DOCUMENT opens the screen in document mode`() {
        val viewModel = viewModel(AddMode.DOCUMENT)

        assertEquals(AddMode.DOCUMENT, viewModel.uiState.value.mode)
    }

    @Test
    fun `selecting a mode updates the ui state`() {
        val viewModel = viewModel()

        viewModel.onModeSelected(AddMode.DOCUMENT)
        assertEquals(AddMode.DOCUMENT, viewModel.uiState.value.mode)

        viewModel.onModeSelected(AddMode.ITEM)
        assertEquals(AddMode.ITEM, viewModel.uiState.value.mode)
    }

    @Test
    fun `save stays disabled until a name is typed`() {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onNameChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onNameChange("Car key")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `typing fields updates the ui state`() {
        val viewModel = viewModel()

        viewModel.onNameChange("Passport")
        viewModel.onDescriptionChange("blue booklet")
        viewModel.onPlaceChange("drawer in bedroom")

        val state = viewModel.uiState.value
        assertEquals("Passport", state.name)
        assertEquals("blue booklet", state.description)
        assertEquals("drawer in bedroom", state.place)
    }
}
