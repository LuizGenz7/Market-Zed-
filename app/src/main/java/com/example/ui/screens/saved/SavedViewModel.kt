package com.example.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.AppSettings
import com.example.data.model.Listing
import com.example.data.repository.LocalUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val favorites: List<Listing> = emptyList(),
    val recentlyViewed: List<Listing> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val userScope: String = "guest"
)

class SavedViewModel(
    private val localUserRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favListingsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteListings(auth.userScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recentFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeRecentlyViewed(auth.userScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    val uiState: StateFlow<SavedUiState> = combine(
        favListingsFlow,
        recentFlow,
        favIdsFlow,
        localUserRepository.observeSettings(),
        authRepository.authState
    ) { favs, recent, favIds, settings, auth ->
        SavedUiState(
            favorites = favs,
            recentlyViewed = recent,
            favoriteIds = favIds,
            settings = settings,
            userScope = auth.userScope
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SavedUiState()
    )

    fun toggleFavorite(listingId: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleFavorite(scope, listingId)
        }
    }

    fun clearRecentlyViewed() {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.clearRecentlyViewed(scope)
        }
    }
}
