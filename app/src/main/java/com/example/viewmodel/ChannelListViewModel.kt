package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.UserProfile
import com.example.data.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChannelListUiState {
    object Loading : ChannelListUiState()
    data class Success(val channels: List<UserProfile>) : ChannelListUiState()
    data class Error(val message: String) : ChannelListUiState()
}

class ChannelListViewModel(
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelListUiState>(ChannelListUiState.Loading)
    val uiState: StateFlow<ChannelListUiState> = _uiState.asStateFlow()

    fun loadChannels(userId: String) {
        viewModelScope.launch {
            _uiState.value = ChannelListUiState.Loading
            try {
                val channels = repository.getChannelsForUser(userId)
                _uiState.value = ChannelListUiState.Success(channels)
            } catch (e: Exception) {
                _uiState.value = ChannelListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
