# MarketZed UI polish checkpoint — 25 November 2026

## Changes made in this pass

- Notifications cards: increased visual hierarchy, clearer category icon containers, stronger unread surface/border treatment, more legible title/body typography, and a visible NEW pill for unread items. Existing notification actions and navigation callbacks were preserved.
- Listing editor: the Compose modal bottom sheet now uses up to 92% of available height, keeps the form area scrollable, and visually separates the persistent action row from the form. Existing validation, upload, discard-warning, and save callbacks were preserved.
- Shared empty state: redesigned as a consistent rounded surface with a larger orange icon tile, clearer title/body hierarchy, and a more prominent accessible action button. This is shared across Explore and account/category empty states.

## Preserved

- Kotlin + Jetpack Compose.
- Existing Firebase/Auth/Firestore and payment code paths.
- Existing notification state and callbacks.
- Wanted text-only edit behavior.
- Existing validation, ownership/backend mutation path, and image upload callback.
- Existing light/dark Material 3 color scheme.

## Verification status

- Source-level delimiter sanity checks passed for the three edited Kotlin files.
- A Gradle build and APK could not be run in this environment: the supplied project archive does not include `gradlew` or `gradle-wrapper.jar`, and no system Gradle installation is available. Android build/test status is therefore **not verified**.

## Files changed

- `app/src/main/java/com/example/ui/screens/account/NotificationsScreen.kt`
- `app/src/main/java/com/example/ui/screens/account/ListingEditBottomSheet.kt`
- `app/src/main/java/com/example/ui/components/MarketZedComponents.kt`
