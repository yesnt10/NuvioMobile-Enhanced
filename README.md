<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="NuvioMobile Enhanced" width="320" />

  <h1>NuvioMobile Enhanced</h1>

  <p>
    A community-maintained, polished build of NuvioMobile with a stronger focus on premium UX, faster media browsing, smarter playback, and the features we have been refining in the enhanced branch.
  </p>

  <p>
    <a href="https://github.com/yesnt10/NuvioMobile-Enhanced/releases/latest"><img src="https://img.shields.io/github/v/release/yesnt10/NuvioMobile-Enhanced?include_prereleases&style=for-the-badge&label=Latest%20Release" alt="Latest release" /></a>
    <a href="https://github.com/yesnt10/NuvioMobile-Enhanced/releases"><img src="https://img.shields.io/github/downloads/yesnt10/NuvioMobile-Enhanced/total?style=for-the-badge&label=Downloads" alt="Downloads" /></a>
    <a href="https://github.com/yesnt10/NuvioMobile-Enhanced/blob/enhanced/LICENSE"><img src="https://img.shields.io/github/license/yesnt10/NuvioMobile-Enhanced?style=for-the-badge" alt="License" /></a>
    <a href="https://github.com/yesnt10/NuvioMobile-Enhanced/stargazers"><img src="https://img.shields.io/github/stars/yesnt10/NuvioMobile-Enhanced?style=for-the-badge" alt="Stars" /></a>
  </p>

  <p>
    <a href="#download">Download</a> | <a href="#enhanced-highlights">Highlights</a> | <a href="#build-from-source">Build from source</a> | <a href="#credits">Credits</a>
  </p>

  [Website](https://nuvio.tv) · [GitHub releases](https://github.com/NuvioMedia/NuvioMobile/releases/latest) · [Support Nuvio](https://nuvio.tv/support)

</div>

## Overview

NuvioMobile Enhanced is our tuned fork of NuvioMobile, built to keep pace with upstream while adding the kind of polish that makes the app feel more deliberate in day-to-day use.

The project stays close to the original codebase, but the enhanced branch is where we land improvements like the premium release calendar, richer AI replies, better live TV navigation, and smoother player interactions.



## Download

The fastest way to get the enhanced build is through GitHub Releases:

- [Latest release](https://github.com/yesnt10/NuvioMobile-Enhanced/releases/latest)


## What This Fork Is

This repository is a community-maintained fork of the original NuvioMobile project.

We use it to ship:

- incremental quality-of-life improvements
- UI and interaction refinements
- feature experiments that fit the app's direction
- release builds that stay easy to discover and compare

We also try to keep the relationship with upstream clear, so it is obvious what comes from the original project and what belongs to the enhanced branch.

## Build From Source

```bash
git clone https://github.com/yesnt10/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
./gradlew :composeApp:assembleDebug
```

On Windows, use:

```powershell
git clone https://github.com/yesnt10/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
.\gradlew.bat :composeApp:assembleDebug
```

If you want a broader app-level check, the shared module is the place to start:

- `composeApp/` for shared Kotlin Multiplatform and Compose Multiplatform code
- `composeApp/src/commonMain/` for shared UI, features, repositories, and app logic
- `composeApp/src/androidMain/` for Android-specific integrations
- `composeApp/src/iosMain/` for iOS-specific integrations
- `iosApp/` for the native iOS entry point

## What We Optimize For

Our releases usually focus on one of these themes:

- polish that makes the app feel more premium
- fixes for calendar, playback, and browsing edge cases
- stronger defaults for media discovery and live TV
- smaller updates that reduce friction without changing the app's identity

That is the style we will keep using for future enhanced builds.

## Credits

- Original project: [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile)
- Enhanced fork: [yesnt10/NuvioMobile-Enhanced](https://github.com/yesnt10/NuvioMobile-Enhanced)
- Shared brand asset used here: [tapframe/NuvioTV](https://github.com/tapframe/NuvioTV)

iOS development requires macOS and Xcode.

NuvioMobile Enhanced functions as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

The project does not host, store, or distribute media content and is not affiliated with third-party extensions, catalogs, sources, or content providers.

For the full legal policy and disclaimer, see the upstream legal page:

- [Legal & Disclaimer](https://nuvioapp.space/legal)

## Star History

<a href="https://www.star-history.com/#yesnt10/NuvioMobile-Enhanced&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=yesnt10/NuvioMobile-Enhanced&type=date&theme=dark&legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=yesnt10/NuvioMobile-Enhanced&type=date&legend=top-left" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=yesnt10/NuvioMobile-Enhanced&type=date&legend=top-left" />
  </picture>
</a>
