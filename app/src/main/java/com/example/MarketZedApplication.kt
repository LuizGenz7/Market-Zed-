package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.core.util.MotivationalMessageSelector
import com.example.core.util.SharedPrefsMotivationalHistoryStore
import com.example.data.auth.AuthRepository
import com.example.data.dev.DevFixturesManager
import com.example.data.local.MarketZedDatabase
import com.example.data.notifications.MarketZedNotificationHelper
import com.example.data.notifications.NotificationPreferencesRepository
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.NetworkClient
import com.example.data.repository.AccountRepository
import com.example.data.repository.AdminRepository
import com.example.data.repository.LocalAccountCacheStore
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.SharedPrefsLocalAccountCache
import com.example.data.sync.MarketZedWorkerFactory
import com.example.data.sync.MarketplaceSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: MarketZedDatabase = MarketZedDatabase.getInstance(application)
    val connectivityObserver: ConnectivityObserver = ConnectivityObserver(application)
    val authRepository: AuthRepository = AuthRepository(application)
    val devFixturesManager: DevFixturesManager = DevFixturesManager(
        context = application,
        dao = database.dao()
    )
    val marketplaceRepository: MarketplaceRepository = MarketplaceRepository(
        dao = database.dao(),
        api = NetworkClient.apiService,
        connectivityObserver = connectivityObserver,
        devFixturesManager = devFixturesManager
    )
    val accountRepository: AccountRepository = AccountRepository(
        context = application,
        api = NetworkClient.apiService,
        authRepository = authRepository,
        connectivityObserver = connectivityObserver
    )
    val adminRepository: AdminRepository = AdminRepository(
        api = NetworkClient.apiService,
        authRepository = authRepository,
        connectivityObserver = connectivityObserver
    )
    val localUserRepository: LocalUserRepository = LocalUserRepository(
        context = application,
        dao = database.dao()
    )
    val localAccountCache: LocalAccountCacheStore = SharedPrefsLocalAccountCache(application)
    val notificationPreferencesRepository: NotificationPreferencesRepository =
        NotificationPreferencesRepository(application)
    val motivationalMessageSelector: MotivationalMessageSelector =
        MotivationalMessageSelector(SharedPrefsMotivationalHistoryStore(application))
    val syncScheduler: MarketplaceSyncScheduler = MarketplaceSyncScheduler(application)
}

class MarketZedApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    val container: AppContainer by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                MarketZedWorkerFactory(
                    marketplaceRepository = container.marketplaceRepository,
                    dao = container.database.dao(),
                    notificationPreferencesRepository = container.notificationPreferencesRepository,
                    motivationalMessageSelector = container.motivationalMessageSelector
                )
            )
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        val appContainer = container
        MarketZedNotificationHelper.ensureChannelsCreated(this)
        appContainer.notificationPreferencesRepository.recordAppActive()
        appScope.launch {
            runCatching {
                appContainer.notificationPreferencesRepository.syncFcmTopics()
                appContainer.notificationPreferencesRepository.refreshFcmTokenAsync()
            }
            runCatching {
                appContainer.marketplaceRepository.ensureCatalogueSeeded()
            }
            runCatching {
                val settings = appContainer.database.dao().getSettings()
                appContainer.syncScheduler.schedulePeriodicSync(
                    wifiOnly = settings?.wifiOnlyPrefetch ?: false,
                    dataSaverEnabled = settings?.dataSaverEnabled ?: true
                )
                appContainer.syncScheduler.scheduleMotivationalNotifications(true)
            }
            runCatching {
                var previousConnected: Boolean? = null
                appContainer.connectivityObserver.isConnectedHint.collect { connected ->
                    if (connected && (previousConnected == false || previousConnected == null)) {
                        runCatching {
                            appContainer.syncScheduler.enqueueImmediateSync(force = false, source = "auto_on_connect")
                        }
                    }
                    previousConnected = connected
                }
            }
        }
    }

    companion object {
        @Volatile
        var isForeground: Boolean = false
    }

    override fun newImageLoader(): ImageLoader {
        // Bounded memory & disk image caches optimized for modest Android devices
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("marketzed_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L) // 64 MB cap
                    .build()
            }
            .crossfade(180)
            .build()
    }
}
