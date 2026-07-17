package com.nuvio.app.features.anilist

fun handleAniListAuthCallbackUrl(url: String) {
    AniListAuthRepository.onAuthCallbackReceived(url)
}
