# Nuvio Tracking & Incognito Architecture: Deep Dive Guide

This document is a highly detailed, file-by-file blueprint of how Nuvio's multi-provider tracking (AniList, MyAnimeList, Trakt, Simkl) and Incognito Mode are implemented in the `NuvioEnhanced Backup` codebase. It is written to give any AI or developer an exact map of the classes, logic flows, and edge-case fixes without needing to read the entire codebase.

---

## 1. Tracker Provider Architecture

Nuvio supports four different tracking services, split broadly into two categories:
1. **Anime Trackers**: AniList, MyAnimeList (MAL)
2. **Series/Movie Trackers**: Trakt, Simkl

### 1.1 Connection & State Management
Each service relies on an `AuthRepository` for managing OAuth state/tokens and an `ApiClient` for making network requests.

* **AniList**: 
  * `AniListAuthRepository.kt`: Manages `AniListConnectionMode` (e.g., `CONNECTED`), `accessToken`, and user settings (like `scoreFormat`).
  * `AniListApiClient.kt`: Contains `saveProgress()`, `fetchListEntry()`, and `deleteEntry()`. Uses GraphQL.
* **MyAnimeList (MAL)**: 
  * `MalAuthRepository.kt`: Stores `MalConnectionMode` and tokens.
  * `MalApiClient.kt`: Contains `saveProgress()`, `deleteEntry()`. Uses REST endpoints (`/v2/anime/{id}/my_list_status`).
* **Trakt**: 
  * `TraktAuthRepository.kt`: Stores `TraktConnectionMode`.
  * `TraktManualTracker.kt`: Handles manual user tracking/scoring via `saveRating()` and `removeRating()`.
  * `TraktScrobbleRepository.kt`: Handles automatic playback scrobbling.
* **Simkl**: 
  * `SimklAuthRepository.kt`: Stores `SimklConnectionMode`.
  * `SimklApiClient.kt`: Contains `saveStatusAndProgress()`, `search()`.

### 1.2 ID Mapping & Resolution (`AnimeTrackerMappingStorage.kt`)
Because Nuvio matches streams from raw scrapers/TMDB to specific tracker IDs, it often needs manual user overrides when automatic matching fails.
* **Storage implementation**: `AnimeTrackerMappingStorage` uses `expect`/`actual` declarations.
  * `AnimeTrackerMappingStorage.android.kt`: Uses Android's `SharedPreferences` (named `"AnimeTrackerMapping"`).
  * `AnimeTrackerMappingStorage.ios.kt`: Uses iOS `NSUserDefaults.standardUserDefaults`.
* **API**: Exposes functions like `saveAniListOverride(contentId: String, aniListId: Int)`, `getAniListOverride(contentId)`, and `removeAniListOverride(contentId)`.
* **Resolution**: When a background process needs to track a show, it calls resolution utility functions like `resolveToAniListId()` (in `AniListIdUtils.kt`), which first checks `AnimeTrackerMappingStorage` for a user-defined override. If none exists, it falls back to resolving via TMDB/IMDB IDs or title searches.

---

## 2. Manual Tracking UI

When a user taps the tracker icon on a show's details page, they open the tracker UI. 

### 2.1 UI Entry Points
* **`AnimeTrackerSheet.kt`**: A `ModalBottomSheet` displaying AniList and MAL sliders.
  * Supports Advanced Options: custom lists, granular scores (Story, Audio, Visuals), privacy toggles, start/finish dates, and priority.
* **`SeriesMoviesTrackerContent.kt`**: A `Dialog` or bottom sheet for Trakt and Simkl.
  * Supports 1-to-5 star Trakt ratings and 1-to-10 Simkl ratings, plus manual episode progress adjustments.

### 2.2 The "Save Cancellation" Bug (CRITICAL ARCHITECTURE NOTE)
In older versions, tapping the "Save" button in the tracker UI would fail to update the remote trackers randomly.
* **The Problem**: The "Save" button launched a `scope.launch { ... }` tied to the BottomSheet/Dialog UI. Because the dialog instantly called `onDismiss()` at the end of the block (or if the user tapped outside the modal while it was saving), the UI `CoroutineScope` was cancelled. This immediately aborted any pending `async` network requests inside that scope.
* **The Fix (Do not remove this if refactoring)**: 
  All saves are strictly wrapped in `kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable)`. 
  ```kotlin
  scope.launch {
      isSaving = true
      kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
          kotlinx.coroutines.coroutineScope {
              val saves = mutableListOf<kotlinx.coroutines.Deferred<Any?>>()
              if (aniListConnected) {
                  saves.add(async { AniListApiClient.saveProgress(...) })
              }
              // ...
              saves.awaitAll()
          }
      }
      isSaving = false
      onDismiss()
  }
  ```
  This ensures the network API requests run entirely to completion even if the UI unmounts and cancels its host `scope`.

### 2.3 Unlink vs. Delete Workflows
Inside the tracker UIs, users have two distinct ways to detach a show:
1. **Unlink App**: Calls `removeAniListOverride(contentId)` (or MAL/Trakt/Simkl equivalent) from `AnimeTrackerMappingStorage`. This purely affects Nuvio's local database, forcing it to "forget" the mapping. The remote watch history on AniList/Trakt remains untouched.
2. **Delete from [Provider]**: Runs in a `NonCancellable` background scope. It unlinks the app locally **and** fires an API deletion request:
   * **AniList**: `AniListApiClient.fetchListEntry` -> `deleteEntry(token, entryId)`.
   * **MAL**: `MalApiClient.deleteEntry(token, malId)`.
   * **Trakt**: `TraktManualTracker.removeRating("show", finalTraktId)`.
   * *(Note: Simkl currently lacks an exposed endpoint for complete targeted deletion in Nuvio, so its "Delete" button only performs a local Unlink).*

---

## 3. Automatic Scrobbling (Playback Tracking)

When a user watches a video, Nuvio automatically tracks their progress remotely if they cross a specific threshold. This logic resides in **`PlayerScreenRuntimePlaybackActions.kt`**.

### 3.1 Playback Scrobble Flow
1. **The Threshold**: As video playback progresses, Nuvio periodically calculates `currentPlaybackProgressPercent()`.
2. **Trigger**: When the video is paused, stopped, or crosses the 80% mark, `emitScrobbleStop(progressPercent: Float?)` is called.
3. **Execution**:
   * It first checks `NuvioEnhancedSettingsRepository.uiState.value.incognitoModeEnabled`. If `true`, the method returns immediately (see Section 4).
   * A `scope.launch(NonCancellable)` is spawned.
   * It concurrently invokes the `scrobbleStop` methods on the four Scrobble Repositories:
     * `AniListScrobbleRepository.scrobbleStop(...)`
     * `MalScrobbleRepository.scrobbleStop(...)`
     * `SimklScrobbleRepository.scrobbleStop(...)`
     * `TraktScrobbleRepository.scrobbleStop(...)`
4. **Inside the Repository (e.g., `AniListScrobbleRepository.kt`)**:
   * **Rate Limiting**: Checks `minSendIntervalMs` (e.g., 8000ms) to prevent API spam.
   * **Resolution**: Calls `resolveToAniListId()` to get the correct remote media ID.
   * **Network Call**: Executes `AniListApiClient.saveProgress(...)`.

---

## 4. Incognito Mode Implementation

Incognito Mode is a strict privacy toggle that ensures Nuvio behaves in a "read-only" manner regarding watch history. When enabled, it completely bypasses both local SQLite history saving and remote API scrobbling.

### 4.1 State Source of Truth
* **Repository**: `NuvioEnhancedSettingsRepository.kt`
* **State property**: `uiState.value.incognitoModeEnabled` (Boolean).
* **Storage**: Saved persistently across app restarts.
* **UI**: Toggled in `SettingsRootPage.kt`.

### 4.2 Gatekeeping Logic (How it blocks tracking)
To enforce Incognito Mode, specific interceptor checks are placed at the beginning of critical state-mutating functions in **`PlayerScreenRuntimePlaybackActions.kt`**:

```kotlin
internal fun PlayerScreenRuntime.emitScrobbleStart() {
    // ...
    if (com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository.uiState.value.incognitoModeEnabled) return
    // ...
}

internal fun PlayerScreenRuntime.emitScrobbleStop(progressPercent: Float? = null) {
    // ...
    if (com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository.uiState.value.incognitoModeEnabled) return
    // ...
}
```

By placing these early returns, the application bypasses:
1. **Remote Trackers**: `emitScrobbleStart` and `emitScrobbleStop` never fire their API calls to Trakt/AniList/MAL/Simkl.
2. **Local History**: Functions that save to `WatchedStorage` or `ContinueWatchingEnrichmentStorage` (the local Realm/SQLite databases that populate the "Continue Watching" row on the home screen) also check this boolean and return early.

> [!CAUTION] 
> **Maintenance Rule**: If a new tracking provider is added, or a new feature is built that saves user activity (e.g., a "Recently Searched" database), the developer **must** explicitly check `NuvioEnhancedSettingsRepository.uiState.value.incognitoModeEnabled` and abort if it is `true`. The system relies on explicit bypasses at the call sites rather than a centralized interceptor.
