package com.memora.app.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.memora.app.domain.model.AddMode
import com.memora.app.feature.add.AddViewModel
import com.memora.app.feature.detail.ItemDetailViewModel
import com.memora.app.feature.home.HomeViewModel
import com.memora.app.feature.search.SearchViewModel

/**
 * Single hand-written ViewModel factory — dependencies come from
 * [AppContainer]; screen arguments are passed in explicitly.
 */
class MemoraViewModelFactory(
    private val container: AppContainer,
    private val itemId: String? = null,
    private val addMode: AddMode = AddMode.ITEM,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.itemRepository, container.recordSentence, container.undoMove)

        modelClass.isAssignableFrom(SearchViewModel::class.java) ->
            SearchViewModel(container.itemRepository)

        modelClass.isAssignableFrom(AddViewModel::class.java) ->
            AddViewModel(container.addItem, addMode)

        modelClass.isAssignableFrom(ItemDetailViewModel::class.java) ->
            ItemDetailViewModel(container.itemRepository, container.moveItem, itemId.orEmpty())

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
