package com.jacob.coasters.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacob.coasters.data.RollerCoasterRepository
import com.jacob.coasters.model.RollerCoasterDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RollerCoasterDetailState(
    val isLoading: Boolean = true,
    val detail: RollerCoasterDetail? = null,
    val error: String? = null
)

class RollerCoasterDetailViewModel(
    private val repository: RollerCoasterRepository,
    private val slug: String
) : ViewModel() {
    private val mutableState = MutableStateFlow(RollerCoasterDetailState())
    val state: StateFlow<RollerCoasterDetailState> = mutableState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        mutableState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.loadDetail(slug) }
                .onSuccess { detail ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            detail = detail,
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
}
