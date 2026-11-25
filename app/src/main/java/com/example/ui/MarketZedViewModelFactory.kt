package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.AppContainer
import com.example.ui.screens.account.AccountViewModel
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.compare.ComparisonViewModel
import com.example.ui.screens.detail.ListingDetailViewModel
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.post.PostListingViewModel
import com.example.ui.screens.saved.SavedViewModel
import com.example.ui.screens.seller.SellerProfileViewModel
import com.example.ui.screens.wanted.WantedViewModel

class MarketZedViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                accountRepository = container.accountRepository,
                authRepository = container.authRepository,
                motivationalMessageSelector = container.motivationalMessageSelector
            ) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                authRepository = container.authRepository
            ) as T
            modelClass.isAssignableFrom(ListingDetailViewModel::class.java) -> ListingDetailViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                accountRepository = container.accountRepository,
                authRepository = container.authRepository
            ) as T
            modelClass.isAssignableFrom(SavedViewModel::class.java) -> SavedViewModel(
                localUserRepository = container.localUserRepository,
                authRepository = container.authRepository
            ) as T
            modelClass.isAssignableFrom(WantedViewModel::class.java) -> WantedViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                authRepository = container.authRepository
            ) as T
            modelClass.isAssignableFrom(PostListingViewModel::class.java) -> PostListingViewModel(
                accountRepository = container.accountRepository,
                localUserRepository = container.localUserRepository,
                marketplaceRepository = container.marketplaceRepository,
                authRepository = container.authRepository,
                localAccountCache = container.localAccountCache
            ) as T
            modelClass.isAssignableFrom(AccountViewModel::class.java) -> AccountViewModel(
                authRepository = container.authRepository,
                accountRepository = container.accountRepository,
                localUserRepository = container.localUserRepository,
                marketplaceRepository = container.marketplaceRepository,
                syncScheduler = container.syncScheduler,
                notificationPreferencesRepository = container.notificationPreferencesRepository,
                localAccountCache = container.localAccountCache,
                adminRepository = container.adminRepository,
                devFixturesManager = container.devFixturesManager
            ) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(
                adminRepository = container.adminRepository,
                marketplaceRepository = container.marketplaceRepository
            ) as T
            modelClass.isAssignableFrom(ComparisonViewModel::class.java) -> ComparisonViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                authRepository = container.authRepository
            ) as T
            modelClass.isAssignableFrom(SellerProfileViewModel::class.java) -> SellerProfileViewModel(
                marketplaceRepository = container.marketplaceRepository,
                localUserRepository = container.localUserRepository,
                authRepository = container.authRepository
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
