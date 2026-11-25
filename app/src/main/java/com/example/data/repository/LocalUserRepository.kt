package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.MarketZedDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AppSettings
import com.example.data.model.Listing
import com.example.data.model.ListingDraft
import com.example.data.model.SavedSearch
import com.example.data.model.SearchFilter
import com.example.data.repository.LocalMappers.toDomain
import com.example.data.repository.LocalMappers.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ComparisonAddResult {
    data object Added : ComparisonAddResult()
    data object AlreadySelected : ComparisonAddResult()
    data object LimitReached : ComparisonAddResult()
}

class LocalUserRepository(
    private val context: Context,
    private val dao: MarketZedDao
) {

    fun observeFavoriteIds(userScope: String): Flow<Set<String>> =
        dao.observeFavoriteIds(userScope).map { it.toSet() }

    fun observeFavoriteListings(userScope: String): Flow<List<Listing>> =
        dao.observeFavoriteListings(userScope).map { rows -> rows.map { it.toDomain() } }

    suspend fun toggleFavorite(userScope: String, listingId: String): Boolean =
        dao.toggleFavorite(userScope, listingId)

    fun observeRecentlyViewed(userScope: String): Flow<List<Listing>> =
        dao.observeRecentlyViewed(userScope).map { rows -> rows.map { it.toDomain() } }

    suspend fun recordRecentlyViewed(userScope: String, listingId: String) {
        dao.recordRecentlyViewed(userScope, listingId)
    }

    suspend fun clearRecentlyViewed(userScope: String) {
        dao.clearRecentlyViewed(userScope)
    }

    fun observeDrafts(userScope: String): Flow<List<ListingDraft>> =
        dao.observeDrafts(userScope).map { rows -> rows.map { it.toDomain() } }

    suspend fun getDraft(localUuid: String): ListingDraft? =
        dao.getDraft(localUuid)?.toDomain()

    suspend fun saveDraft(draft: ListingDraft) {
        dao.upsertDraft(draft.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteDraft(localUuid: String) {
        dao.deleteDraft(localUuid)
    }

    suspend fun copyPickerUriToPrivateDraftStorage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val draftDir = File(context.filesDir, "draft_images").apply { mkdirs() }
            val mime = context.contentResolver.getType(uri).orEmpty()
            val ext = when {
                mime.contains("png", ignoreCase = true) -> "png"
                mime.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val destFile = File(draftDir, "draft_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not read selected image."))

            val maxBytes = 8L * 1024L * 1024L
            if (destFile.length() > maxBytes) {
                destFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Image exceeds 8 MB maximum size."))
            }
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteDraftImageFileIfUnused(path: String) {
        runCatching {
            val file = File(path)
            if (file.exists() && file.absolutePath.contains("draft_images")) {
                file.delete()
            }
        }
    }

    fun observeSearchHistory(userScope: String): Flow<List<String>> =
        dao.observeSearchHistory(userScope).map { list -> list.map { it.query } }

    suspend fun addSearchHistory(userScope: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            dao.upsertSearchHistory(
                SearchHistoryEntity(
                    userScope = userScope,
                    query = trimmed,
                    searchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeSearchHistoryItem(userScope: String, query: String) {
        dao.deleteSearchHistoryItem(userScope, query)
    }

    suspend fun clearSearchHistory(userScope: String) {
        dao.clearSearchHistory(userScope)
    }

    // ---- Saved Searches ----
    fun observeSavedSearches(userScope: String): Flow<List<SavedSearch>> =
        dao.observeSavedSearches(userScope).map { list -> list.map { it.toDomain() } }

    suspend fun saveSearch(
        userScope: String,
        name: String,
        filter: SearchFilter,
        notifyMatches: Boolean = true
    ): SavedSearch {
        val cleanName = name.trim().ifEmpty {
            listOfNotNull(
                filter.query.takeIf { it.isNotBlank() },
                filter.category,
                filter.province
            ).joinToString(" • ").ifEmpty { "Saved search" }
        }
        val entry = SavedSearch(
            id = UUID.randomUUID().toString(),
            userScope = userScope,
            name = cleanName,
            query = filter.query.trim(),
            category = filter.category?.takeIf { it.isNotBlank() },
            subcategory = filter.subcategory?.takeIf { it.isNotBlank() },
            province = filter.province?.takeIf { it.isNotBlank() },
            town = filter.town?.takeIf { it.isNotBlank() },
            minPrice = filter.minPrice,
            maxPrice = filter.maxPrice,
            condition = filter.condition?.takeIf { it.isNotBlank() },
            sort = filter.sort,
            notifyMatches = notifyMatches,
            createdAt = System.currentTimeMillis(),
            lastMatchedAt = System.currentTimeMillis()
        )
        dao.upsertSavedSearch(entry.toEntity())
        return entry
    }

    suspend fun toggleSavedSearchAlerts(userScope: String, id: String, enabled: Boolean) {
        dao.updateSavedSearchAlert(userScope, id, enabled)
    }

    suspend fun deleteSavedSearch(userScope: String, id: String) {
        dao.deleteSavedSearch(userScope, id)
    }

    // ---- Product Comparison (Max 3 per userScope) ----
    fun observeComparisonIds(userScope: String): Flow<List<String>> =
        dao.observeComparisonIds(userScope)

    suspend fun addComparisonItem(userScope: String, listingId: String): ComparisonAddResult {
        return when (dao.addComparisonSelectionUpToThree(userScope, listingId)) {
            1 -> ComparisonAddResult.Added
            0 -> ComparisonAddResult.AlreadySelected
            else -> ComparisonAddResult.LimitReached
        }
    }

    suspend fun removeComparisonItem(userScope: String, listingId: String) {
        dao.removeComparisonSelection(userScope, listingId)
    }

    suspend fun clearComparisonItems(userScope: String) {
        dao.clearComparisonSelections(userScope)
    }

    fun observeSettings(): Flow<AppSettings> =
        dao.observeSettings().map { it.toDomain() }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = dao.getSettings().toDomain()
        dao.upsertSettings(transform(current).toEntity())
    }
}
