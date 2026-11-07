package com.memora.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.repository.ItemRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Search over local items by name and description. Offline, no indexing
 * service — a simple SQL LIKE over the Room table, debounced.
 */
class SearchViewModel(
    private val repository: ItemRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<ItemSummary>>(emptyList())
    val results: StateFlow<List<ItemSummary>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()
        if (value.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            _results.value = repository.searchItems(value)
        }
    }
}
