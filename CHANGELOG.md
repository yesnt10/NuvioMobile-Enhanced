# Changelog

## NuvioMobile Enhanced 0.4.13 (117)

Integrated upstream NuvioMobile changes through `0.4.13` into the Enhanced fork.

### Added

- Added Telegram account integration to the full iOS build using TDLib.
- Added phone, login code, email verification, and two-step verification states under Settings > Integrations > Telegram.
- Added Telegram video files as a first-class stream source alongside addons and plugins.
- Added resilient episode searches for `S01E01`, `S1E1`, `1x01`, and Season/Episode filename styles.
- Added a seekable loopback HTTP range bridge so Telegram files can play in the existing MPV pipeline while downloading.
- Added Telegram video cache size and clear controls.
- Added a Behind the Scenes selector on detail pages so trailers and teasers are grouped behind one compact control.
- Added "Remember My Choices" playback preferences for carrying selected audio, subtitles, and video fit behavior into the next episode.
- Added Enhanced catalog display controls for poster columns, poster size, poster layout, and Home shelf row count.

### Improved

- Updated the Enhanced fork to upstream version `0.4.13 (117)`.
- Stored the TDLib database encryption key in iOS Keychain and limited the media bridge to localhost.
- Added correct `HEAD`, byte range, MIME type, filename, reconnect-after-logout, and duplicate result handling.
- Restored clean-build generation of the optional Simkl runtime configuration.
- Moved Home and catalog presentation controls into the Nuvio Enhanced settings area.
- Made Home shelves configurable with one to four visible poster rows per catalog section.
- Made poster release-date hiding apply across Home, catalog, folders, and poster grids instead of only the Home page.
- Kept the streaming-style hero enabled by default and simplified the hero personalization surface.
- Improved Hero trailer playback quality selection for higher-resolution previews when available.

### Removed

- Removed Release Radar surfaces and settings while the replacement calendar-led library flow is being reconsidered.
- Removed the "Continue-ready" hero badge and its related setting.
- Removed unsupported or unused Enhanced settings surfaces, including iOS CloudStream/CS3, network DNS controls, feedback mail, and disabled supporter/contributor navigation for now.

### Notes

- Telegram sources currently require an iOS full build and developer-provided `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` values in `local.properties`.
- Telegram results only include media visible to the connected account; the app does not provide or join channels.

## NuvioMobile Enhanced 0.4.12 (116)

Integrated upstream NuvioMobile changes through `0.4.12` into the Enhanced fork.

### Added

- Six-character device login codes for the newer authentication flow.
- Simkl improvements for anime tracking, per-season MAL/Kitsu IDs, library search, poster handling, and local scrobble reconciliation.
- Selectable app icons and matching splash/logo assets.
- Supporter membership status surfaces and cached member asset loading.
- TMDB Discover exclusion filters and richer entity browsing badges.
- iOS test IPA build workflows and sideload source automation.
- Self-hosted server discovery endpoint support.
- Arabic locale resources and expanded Greek, Turkish, Slovak, Bulgarian, Hungarian, Dutch, and support strings.

### Improved

- Updated the Kotlin and Compose Multiplatform build stack from upstream.
- Modularized app navigation and profile gate structure.
- Reduced duplicate sync reads, foreground polling, provider credential churn, and remote progress writes.
- Improved trailer behavior, expandable descriptions, landscape poster sizing, and detail page episode handling.
- Improved subtitle handling with sidecar subtitle buffering, SDH filtering, forced subtitle selection, RTL fixes, and active subtitle auto-scroll.
- Improved playback service startup, progress handling, pause descriptions, MPV local file handling, and controls behavior.
- Improved profile loading, profile backgrounds, PIN cache handling, and add-profile visibility.
- Improved network retry handling and addon/plugin cache behavior.

### Fixed

- Crash clusters reported by upstream mobile telemetry.
- Search state loss when changing tabs and catalog scroll reset after opening details.
- Now Playing recycled bitmap crashes and player metadata edge cases.
- Trakt watched sync rate limits, redundant refreshes, large response preservation, and watched state persistence.
- Simkl completed/dropped/on-hold state reconciliation and anime movie watched markers.
- iOS launch screen configuration and scoped iOS foreground/application state.
- Android launcher icon scale and Play compliance metadata.
- Several collection, episode progress, subtitle parsing, and stream loading regressions.

### Enhanced Fork

- Rebased the app onto upstream `0.4.12` while keeping the Nuvio Enhanced settings area and fork identity.
- Updated the settings footer to show Nuvio Enhanced with the current app version `0.4.12 (116)`.
- Restored fork README links and build instructions for `yesnt10/NuvioMobile-Enhanced`.
- Kept the local iOS simulator build path usable with `NUVIO_IOS_DISTRIBUTION=appstore` when the private full `NuvioEngine.xcframework` is unavailable.
- Kept full iOS simulator builds usable with addon/plugin sources enabled when the private `NuvioEngine.xcframework` is unavailable by falling back only the iOS P2P engine implementation.
- Fixed iOS simulator startup after the merge by restoring missing homescreen resources and aligning the native player bridge with the current Kotlin API.
