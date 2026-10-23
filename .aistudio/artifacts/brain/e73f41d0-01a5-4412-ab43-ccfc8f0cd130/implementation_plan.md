# Implementation Plan: Notification Auto-Hide, Admin Balance Fix & In-App Updater

## 1. Overview & Goals
- **Auto-Hide Toast / Notification Banner (3 Seconds)**: Ensure the top real-time notification banner (`RealTimeNotificationBanner`) automatically dismisses itself after 3 seconds (`3000ms`) whenever triggered, while still allowing manual dismissal via the close (`X`) button.
- **Admin Panel User Balance & Button Layout Fix**: Fix the horizontal layout compression in `UserDetailItemCard` on the Admin User Management screen (`AdminUserManagerScreen.kt`) so that user wallet balances (`₹250`, `₹5000`, etc.) never wrap vertically or get crushed off-screen by long usernames/IGNs, and bottom action buttons (`Balance`, `Suspend`/`Activate`) display cleanly on a single line.
- **In-App Version Check & Mandatory Updater**:
  - Automatically check if the installed app version matches the latest version when the user opens the app.
  - When a newer version is available, present a **mandatory (non-dismissible) Force Update modal** blocking access until the user completes the built-in in-app update with a smooth download & installation progress indicator, after which the local installed version updates to the latest version.
  - Provide a manual **"Check for Updates / App Version"** card in the User Profile screen (`ProfileScreen.kt`) and allow Admins to configure/bump the latest target version and release notes in Application Settings (`AdminSettingsScreen.kt`) for testing or rolling out updates at any time.

---

## 2. Key Technical Changes

### A. 3-Second Auto-Dismiss for Top Notification Banner
- **`MainActivity.kt` & `CommonComponents.kt` (`RealTimeNotificationBanner`)**:
  - Add a `LaunchedEffect(alert)` inside `RealTimeNotificationBanner` (and synchronize with `currentLiveAlert` in `MainActivity.kt`) that waits `3000L` milliseconds whenever a non-null `LiveAlertMessage` is displayed and then automatically invokes `onDismiss()`.
  - Ensure any rapid consecutive alert resets the 3-second timer cleanly.

### B. Admin User Management Balance & Action Row Fix
- **`AdminUserManagerScreen.kt` (`UserDetailItemCard`)**:
  - Apply `Modifier.weight(1f).padding(end = 10.dp)` to the left-side user identity `Row` and `Modifier.weight(1f)` to its inner text `Column`, adding `maxLines = 1` and `overflow = TextOverflow.Ellipsis` to the username, IGN/UID, and email texts so they never push the balance badge off the card.
  - Configure the wallet balance `Surface` pill on the right with `maxLines = 1` and `softWrap = false` so balances like `₹250` and `₹5000` always render intact on a single line.
  - Adjust the bottom action buttons (`Details`, `Balance`, `Suspend`/`Activate`, `Delete`) with balanced weights, `contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)`, `maxLines = 1`, and `softWrap = false` so `"Balance"` never breaks across two lines (`"Balan"` / `"ce"`).

### C. In-App Version Check & Mandatory In-App Update Flow
- **Data Model & Version State (`FirestoreModels.kt`, `ThemePreferenceManager.kt`, `AppViewModel.kt`)**:
  - Extend `AppSettings` with `latestAppVersion: String = "2.5.0"` and `updateReleaseNotes: String = "Performance improvements, enhanced tournament room sync, and UI polish."`.
  - Track the user's currently installed app version in `ThemePreferenceManager` (defaulting to `"2.4.0"` initially so the user can immediately experience and test the in-app update to `"2.5.0"`, or trigger new updates whenever the Admin bumps `latestAppVersion`).
  - Expose `installedAppVersion`, `isUpdateAvailable`, and `completeInAppUpdate(newVersion: String)` in `AppViewModel`.
- **Mandatory In-App Update Modal (`CommonComponents.kt` & `MainActivity.kt`)**:
  - Create a polished `MandatoryAppUpdateDialog` composable triggered automatically on app launch whenever `installedAppVersion != appSettings.latestAppVersion` (and when triggered manually from Profile).
  - Block dismissal (`DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`) to enforce the mandatory update policy.
  - Display current version vs. latest version badge, release notes, and an **"Update Now"** button that transitions into an animated in-app download & installation progress bar (`0% -> 100%` with step statuses: *Downloading update package...*, *Verifying integrity...*, *Installing update...*), then updates `installedAppVersion` to `latestAppVersion` and unlocks the app with a confirmation alert.
- **Manual Check in User Profile & Admin Version Control (`ProfileScreen.kt` & `AdminSettingsScreen.kt`)**:
  - Add an **"App Version & Updates"** card in `ProfileScreen.kt` showing the current installed version badge, status (*Up to date* or *Update available*), and a **"Check for Updates"** button that verifies the version against the latest release (and opens the updater or allows simulating/re-checking).
  - Add an **"App Version & Mandatory Update"** section in `AdminSettingsScreen.kt` so the Admin can set the `latestAppVersion` and `updateReleaseNotes` at any time to push an immediate mandatory in-app update to users.
