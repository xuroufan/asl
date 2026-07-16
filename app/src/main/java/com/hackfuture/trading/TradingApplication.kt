package com.hackfuture.trading

import android.app.Application
import android.content.Context
import com.hackfuture.core.util.LocaleManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TradingApplication : Application() {

    /**
     * 在 Application 级别应用用户保存的语言。
     */
    override fun attachBaseContext(base: Context) {
        val savedLang = LocaleManager.getCurrentLanguage(base)
        val context = LocaleManager.setLocale(base, savedLang)
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initCrashlytics()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initCrashlytics() {
        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
                log("TradingApplication initialized")
            }
        }
    }
}
