package com.hackfuture.core.network.di

import com.hackfuture.core.network.ApiConstants
import com.hackfuture.core.network.ApiService
import com.hackfuture.core.network.AuthInterceptor
import com.hackfuture.core.network.alltick.AllTickApiService
import com.hackfuture.core.network.alltick.AllTickConstants
import com.hackfuture.core.network.auth.TokenAuthenticator
import com.hackfuture.core.network.security.SslPinner
import com.hackfuture.core.network.security.TrustAllManager
import com.hackfuture.core.network.websocket.WebSocketConfig
import com.hackfuture.core.network.websocket.WebSocketManager
import com.hackfuture.core.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor()

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true; isLenient = true
        encodeDefaults = true; coerceInputValues = true; prettyPrint = false
    }

    @Provides @Singleton
    fun provideOkHttpClient(
        sslPinner: SslPinner,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val isDebug = try {
            Class.forName("com.hackfuture.trading.BuildConfig")
                .getField("DEBUG").getBoolean(null)
        } catch (_: Exception) { true }
        val authInterceptor = AuthInterceptor()
        return OkHttpClient.Builder()
            .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .certificatePinner(sslPinner.buildCertificatePinner(isDebug))
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json").build())
            }
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @Named("api_base_url") baseUrl: String,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)



    @Provides @Singleton @Named("alltick")
    fun provideAllTickRetrofit(json: Json): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(AllTickConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideAllTickApiService(@Named("alltick") allTickRetrofit: Retrofit): AllTickApiService =
        allTickRetrofit.create(AllTickApiService::class.java)

    @Provides @Singleton
    fun provideNetworkMonitor(application: android.app.Application): NetworkMonitor =
        NetworkMonitor(application)

    @Provides @Singleton
    fun provideWebSocketConfig(): WebSocketConfig = WebSocketConfig()

    @Provides @Singleton
    fun provideWebSocketManager(
        app: android.app.Application,
        okHttpClient: OkHttpClient,
        tokenManager: com.hackfuture.core.network.security.TokenManager,
        config: WebSocketConfig,
        @Named("ws_base_url") wsBaseUrl: String,
    ): WebSocketManager = WebSocketManager(app, okHttpClient, tokenManager, config)
}
