package com.jacob.coasters.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacob.coasters.data.RollerCoasterRepository
import com.jacob.coasters.model.RollerCoasterListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RollerCoasterListState(
    val query: String = "",
    val isLoading: Boolean = true,
    val items: List<RollerCoasterListItem> = emptyList(),
    val error: String? = null
)

class RollerCoasterListViewModel(
    private val repository: RollerCoasterRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(RollerCoasterListState())
    val state: StateFlow<RollerCoasterListState> = mutableState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        mutableState.update { it.copy(query = value) }
        searchJob?.cancel()
        if (value.isBlank()) {
            refresh()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            runSearch(value)
        }
    }

    fun refresh() {
        searchJob?.cancel()
        mutableState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.fetchAll() }
                .onSuccess { coasters ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = coasters.map { coaster -> RollerCoasterListItem(coaster) },
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableState.update {
                        it.copy(isLoading = false, error = throwable.message)
                    }
                }
        }
    }

    private suspend fun runSearch(query: String) {
        mutableState.update { it.copy(isLoading = true, error = null) }
        runCatching { repository.search(query) }
            .onSuccess { coasters ->
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        items = coasters.map { coaster -> RollerCoasterListItem(coaster) },
                        error = null
                    )
                }
            }
            .onFailure { throwable ->
                mutableState.update {
                    it.copy(isLoading = false, error = throwable.message)
                }
            }
    }
}
