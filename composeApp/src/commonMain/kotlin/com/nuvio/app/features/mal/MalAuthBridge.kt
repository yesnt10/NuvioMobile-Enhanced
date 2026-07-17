package com.nuvio.app.features.mal

fun handleMalAuthCallbackUrl(url: String) {
    MalAuthRepository.onAuthCallbackReceived(url)
}
