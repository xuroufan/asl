package com.hackfuture.core.network.security

import okhttp3.CertificatePinner
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SslPinner @Inject constructor() {

    fun buildCertificatePinner(isDebug: Boolean = false): CertificatePinner {
        if (isDebug) {
            Timber.w("SSL Pinning disabled in debug mode")
            return CertificatePinner.Builder().build()
        }
        val builder = CertificatePinner.Builder()
        builder.add("api.hackfuture.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        builder.add("api.hackfuture.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        builder.add("ws.hackfuture.com", "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=")
        val pinner = builder.build()
        // pins count removed in newer OkHttp
        return pinner
    }

    fun isHostPinned(hostname: String): Boolean {
        return TRUSTED_HOSTS.any { hostname.endsWith(it, ignoreCase = true) }
    }

    companion object {
        private val TRUSTED_HOSTS = listOf(
            "hackfuture.com",
            "api.hackfuture.com",
            "ws.hackfuture.com",
        )
    }
}
