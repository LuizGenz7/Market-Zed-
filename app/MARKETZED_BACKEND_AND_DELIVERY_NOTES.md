# MarketZed Android — Backend Integration, Security Rules & Delivery Notes

This document summarizes the backend contracts, Firestore security rules, composite indexes, Lipila webhook entitlements, Room v4 backward-compatibility strategy, and dev-only test fixture isolation required to pair seamlessly with this MarketZed Android release.

---

## 1. Firestore Security Rules

Clients may read public eligible listings and their own user/payment/notification documents, while sensitive entitlements (`verified`, `verificationStatus`, `verificationPaymentStatus`, `postingEnabled`, `freeWantedPostUsed`, `wantedPostsCount`, `isPromoted`) **MUST** only be written server-side (Cloud Functions / Netlify Backend Admin SDK).

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    function isAdmin() {
      return isSignedIn() && (
        request.auth.token.admin == true ||
        request.auth.token.role in ['admin', 'super_admin', 'superadmin']
      );
    }

    // Users collection: clients can read own profile and edit basic fields ONLY.
    // Verification, posting activation, and freeWantedPostUsed are server-only.
    match /users/{uid} {
      allow read: if isOwner(uid) || isAdmin();
      allow create: if false; // Created via server/auth trigger
      allow update: if isOwner(uid) &&
        !request.resource.data.diff(resource.data).affectedKeys().hasAny([
          'verified',
          'verificationStatus',
          'verificationPaymentStatus',
          'verificationPaymentId',
          'verificationDate',
          'verificationFeeZmw',
          'postingEnabled',
          'freeWantedPostUsed',
          'wantedPostsCount',
          'role',
          'isAdmin',
          'admin'
        ]);
      allow delete: if isAdmin();
    }

    // Listings collection: rental-only property policy & Wanted text-only policy
    match /listings/{listingId} {
      allow read: if resource.data.isPublic == true || isOwner(resource.data.ownerId) || isAdmin();
      allow create, update, delete: if false; // Routed through REST backend / Cloud Functions for validation & quota enforcement
    }

    // Payments collection: strictly server-written via Lipila initiation + webhook
    match /payments/{paymentId} {
      allow read: if isSignedIn() && (resource.data.uid == request.auth.uid || isAdmin());
      allow write: if false;
    }

    // User Notifications subcollection/collection: user may mark read or delete own notification
    match /notifications/{notificationId} {
      allow read: if isSignedIn() && resource.data.uid == request.auth.uid;
      allow update: if isSignedIn()
        && resource.data.uid == request.auth.uid
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['read']);
      allow delete: if isSignedIn() && resource.data.uid == request.auth.uid;
      allow create: if isAdmin();
    }
  }
}
```

---

## 2. Suggested Firestore Composite Indexes (`firestore.indexes.json`)

```json
{
  "indexes": [
    {
      "collectionGroup": "listings",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isPublic", "order": "ASCENDING" },
        { "fieldPath": "categoryId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "listings",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "ownerId", "order": "ASCENDING" },
        { "fieldPath": "categoryId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "listings",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isPublic", "order": "ASCENDING" },
        { "fieldPath": "location.province", "order": "ASCENDING" },
        { "fieldPath": "location.district", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "notifications",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "uid", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    }
  ]
}
```

---

## 3. Server-Side Entitlement Transactions & REST Endpoints

### A. Atomic 1-Free Wanted Post Enforcement (`POST /api/listings`)
Inside a Firestore transaction (`db.runTransaction`):
1. Read `users/{uid}` and check:
   - If `categoryId == "wanted"`:
     - Enforce `images` is empty (`images = []`).
     - If `!user.verified && !user.postingEnabled`:
       - If `user.freeWantedPostUsed == true || (user.wantedPostsCount || 0) >= 1`, reject with HTTP `403` (`"UNLOCK_MORE_WANTED_POSTS: One-time account verification required for 2nd+ Wanted post."`).
       - Otherwise allow publication and atomically increment `wantedPostsCount` by `1` and set `freeWantedPostUsed = true` **only inside the commit block after creating the listing**.
   - If `categoryId != "wanted"`:
     - Require `user.verified == true || (user.postingEnabled && user.verificationPaymentStatus === "paid")`.
   - Always enforce MarketZed's rental-only property policy (reject `categoryId == "land"`, `priceMode == "sale"`, or land/plot/for-sale terms in property listings).
2. The Android client enforces the exact same rule defensively in `PostListingViewModel` inside a coroutine `Mutex` and marks `LocalAccountCacheStore.markLocalFreeWantedPostUsed(userScope)` **only** upon `MutationResult.Success`.

### B. Lipila Mobile Money Payment Webhook (`POST /api/payments/webhook/lipila`)
When Lipila confirms `status === "successful"` (signature-verified, idempotent by `paymentId`):
- Inside a Firestore transaction:
  - Skip if `payment.status === "successful"` already.
  - Update `payments/{paymentId}` to `status = "successful"`, `confirmedAt = now`.
  - If `payment.purpose === "account_verification"`:
    - Update `users/{uid}`:
      - `verified = true`
      - `verificationStatus = "verified"`
      - `verificationPaymentStatus = "paid"`
      - `postingEnabled = true`
      - `verificationPaymentId = paymentId`
      - `verificationDate = now`
  - If `payment.purpose === "listing_promotion"`:
    - Create active promotion record (`expiresAt = now + durationDays`) and mark listing `isPromoted = true`.

### C. Full Listing Editing (`POST /api/listings/revise`)
`ListingEditBottomSheet` submits structured changes in `ReviseListingRequestDto.changes`:
- `title`, `description`, `subcategory`, `price` (numeric or `null`), `availabilityStatus` (`"available"` / `"unavailable"` / `"fulfilled"`)
- `location`: `{ province, district, town, nearbyArea }` (no map/lat/lng fields)
- `contactPreferences`: `{ phone, whatsapp, email }`
- `categoryDetails`: `{ condition, bedrooms, bathrooms, amenities, serviceArea, employerName, employmentType, swapFor, wantedDetails }`
- `images`: array of `ListingImage` (strictly forced to `[]` for `wanted` listings)

Server endpoint must verify `listing.ownerId === req.uid`, re-run rental-only validation for `property`, and strip `images` if `listing.categoryId === "wanted"`.

### D. Notification Actions (`PATCH` / `DELETE /api/account/notifications`)
- `PATCH /api/account/notifications` supports `{ notificationId }` (existing) and optionally `{ action: "mark_all_read" }` (client also marks unread items cleanly).
- `DELETE /api/account/notifications/{id}` deletes a notification document owned by `req.uid`. Local cache tracks deleted IDs so deleted items do not reappear on offline refresh.

### E. Verification Fee Configuration (`GET /api/promotions/pricing`)
- Returns `prices` map including optional `"account_verification"` (or `"verification"`).
- Client defaults cleanly to **ZMW 30 (`K30`)** when omitted (`MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW`).

---

## 4. Local Database (Room v4) Compatibility
- `MarketZedDatabase` stays on **version 4** (`cached_listings`, `listing_drafts`, `saved_searches`, `favorites`, etc.) with zero destructive migration.
- `LocalMappers` encodes `district` and `nearbyArea` inside `specificationsJson` (`__district` / `__nearbyArea`) for `CachedListingEntity` and inside `categoryDetails.specs` (`__draftDistrict` / `__draftNearbyArea`) for `ListingDraftEntity`, stripping `__*` keys before displaying or submitting to REST endpoints.

---

## 5. Dev-Only Test Data Fixtures (`120` Zambian Listings)
- Isolated strictly inside `app/src/debug/java/com/example/data/dev/DevFixturesProviderFactory.kt` and gated behind:
  `BuildConfig.DEBUG && BuildConfig.MARKETZED_TEST_DATA_ENABLED` (`true` in `debug` buildType, `false` in `release` buildType).
- `app/src/release/java/com/example/data/dev/DevFixturesProviderFactory.kt` compiles a minimal no-op stub returning `isSupported = false` and `emptyList()`.
- Exact distribution (`120` total):
  - `35` Marketplace Products
  - `20` Rental Properties (strictly rental-only)
  - `20` Services
  - `15` Jobs & Opportunities
  - `15` Wanted Requests (strictly text-only, `images = emptyList()`)
  - `15` Swaps
- Easily toggled via **Account -> App Settings & Offline Cache -> Developer Test Data (Debug Build Only)** (`Seed 120 Test Listings` / `Clear Dev Test Listings`).

---

## 6. Key Changed & Added Files
- **Build & Config**: `app/build.gradle.kts` (`MARKETZED_TEST_DATA_ENABLED` flags)
- **Models & Catalogue**: `MarketplaceModels.kt`, `MarketZedCatalogue.kt`, `MotivationalMessages.kt` (`HomepageEmptyMessages`)
- **Persistence & Dev Fixtures**: `MarketZedDao.kt`, `LocalMappers.kt`, `LocalAccountCache.kt`, `NotificationPreferencesRepository.kt`, `DevFixturesBridge.kt`, `src/debug/.../DevFixturesProviderFactory.kt`, `src/release/.../DevFixturesProviderFactory.kt`, `MarketplaceRepository.kt`, `AccountRepository.kt`
- **Shared UI Components**: `PaymentComponents.kt`, `VerticalContactActionsColumn.kt`, `CategoryListingCards.kt`, `MarketZedComponents.kt`
- **Screens & ViewModels**: `AccountViewModel.kt`, `AccountScreen.kt`, `AccountSubScreens.kt`, `NotificationsScreen.kt`, `ListingEditBottomSheet.kt`, `PromoteBusinessScreen.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `ExploreViewModel.kt`, `ExploreScreen.kt`, `WantedScreen.kt`, `ListingDetailScreen.kt`, `SellerProfileScreen.kt`, `PostListingViewModel.kt`, `PostListingScreen.kt`, `MarketZedApp.kt`, `MarketZedViewModelFactory.kt`
- **Removed**: `MapDiscoveryView.kt` (no map discovery UI or coordinate centroid logic remains)
- **Tests**: `src/test/java/com/example/MarketZedEnhancementsSpecTest.kt`
