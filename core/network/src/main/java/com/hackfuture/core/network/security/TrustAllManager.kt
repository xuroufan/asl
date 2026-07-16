package com.hackfuture.core.network.security

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/** 信任所有证书的开发用 TrustManager（仅用于 AllTick API） */
class TrustAllManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}
