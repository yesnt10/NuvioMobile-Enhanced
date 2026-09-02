package com.nuvio.app.features.trakt

fun handleTraktAuthCallbackUrl(url: String) {
    TraktAuthRepository.onAuthCallbackReceived(url)
}
