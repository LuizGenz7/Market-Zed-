package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedListingEntity::class,
        CategoryCatalogueEntity::class,
        LocationCatalogueEntity::class,
        FavoriteEntity::class,
        RecentlyViewedEntity::class,
        ListingDraftEntity::class,
        SearchHistoryEntity::class,
        SavedSearchEntity::class,
        ComparisonSelectionEntity::class,
        SyncMetadataEntity::class,
        AppSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MarketZedDatabase : RoomDatabase() {
    abstract fun dao(): MarketZedDao

    companion object {
        @Volatile
        private var INSTANCE: MarketZedDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `cached_listings`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_listings` (
                        `id` TEXT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `ownerId` TEXT,
                        `sellerName` TEXT,
                        `sellerVerified` INTEGER NOT NULL,
                        `isPromoted` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `subcategory` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `price` REAL,
                        `currency` TEXT NOT NULL,
                        `priceMode` TEXT,
                        `province` TEXT NOT NULL,
                        `town` TEXT NOT NULL,
                        `imagesJson` TEXT NOT NULL,
                        `contactPreferencesJson` TEXT,
                        `categoryDetailsJson` TEXT,
                        `moderationStatus` TEXT,
                        `availabilityStatus` TEXT,
                        `wantedStatus` TEXT,
                        `isPublic` INTEGER NOT NULL,
                        `reviewRequired` INTEGER NOT NULL,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        `publishedAt` TEXT,
                        `expiresAt` TEXT,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`sectionKey`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_listings_id` ON `cached_listings` (`id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_listings_sectionKey` ON `cached_listings` (`sectionKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_listings_categoryId` ON `cached_listings` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_listings_cachedAt` ON `cached_listings` (`cachedAt`)")
                db.execSQL("DELETE FROM `sync_metadata`")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history_new` (
                        `userScope` TEXT NOT NULL,
                        `query` TEXT NOT NULL,
                        `searchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userScope`, `query`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `search_history_new` (`userScope`, `query`, `searchedAt`)
                    SELECT 'guest', `query`, `searchedAt` FROM `search_history`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `search_history`")
                db.execSQL("ALTER TABLE `search_history_new` RENAME TO `search_history`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_userScope_searchedAt` ON `search_history` (`userScope`, `searchedAt`)")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `originalPrice` REAL")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `sellerPhotoUrl` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `sellerAccountType` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `sellerBio` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `sellerMemberSince` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `specificationsJson` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `campaignFormat` TEXT")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `impressionsCount` INTEGER")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `clicksCount` INTEGER")
                db.execSQL("ALTER TABLE `cached_listings` ADD COLUMN `visitsCount` INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_searches` (
                        `id` TEXT NOT NULL,
                        `userScope` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `query` TEXT NOT NULL,
                        `category` TEXT,
                        `subcategory` TEXT,
                        `province` TEXT,
                        `town` TEXT,
                        `minPrice` REAL,
                        `maxPrice` REAL,
                        `condition` TEXT,
                        `sort` TEXT NOT NULL,
                        `notifyMatches` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastMatchedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_searches_userScope_createdAt` ON `saved_searches` (`userScope`, `createdAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `comparison_selections` (
                        `userScope` TEXT NOT NULL,
                        `listingId` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userScope`, `listingId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_comparison_selections_userScope_addedAt` ON `comparison_selections` (`userScope`, `addedAt`)")
            }
        }

        fun getInstance(context: Context): MarketZedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MarketZedDatabase::class.java,
                    "marketzed_offline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
