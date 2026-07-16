package com.hackfuture.trading.push

import android.content.Context
import timber.log.Timber

object ManufacturerPushHelper {

    fun registerAll(context: Context, fcmToken: String) {
        registerXiaomi(context, fcmToken)
        registerHuawei(context, fcmToken)
        registerOppo(context, fcmToken)
        registerVivo(context, fcmToken)
    }

    private fun registerXiaomi(context: Context, token: String) {
        try {
            Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
            val appId = getMetaData(context, "XIAOMI_APP_ID")
            val appKey = getMetaData(context, "XIAOMI_APP_KEY")
            if (appId != null && appKey != null) {
                Timber.d("Xiaomi push registered")
            }
        } catch (e: ClassNotFoundException) {
            Timber.w("Xiaomi push SDK not found")
        }
    }

    private fun registerHuawei(context: Context, token: String) {
        try {
            Class.forName("com.huawei.hms.push.HmsMessaging")
            val appId = getMetaData(context, "HUAWEI_APP_ID")
            if (appId != null) {
                Timber.d("Huawei push registered")
            }
        } catch (e: ClassNotFoundException) {
            Timber.w("Huawei push SDK not found")
        }
    }

    private fun registerOppo(context: Context, token: String) {
        try {
            Class.forName("com.heytap.msp.push.HeytapPushManager")
            val appKey = getMetaData(context, "OPPO_APP_KEY")
            val appSecret = getMetaData(context, "OPPO_APP_SECRET")
            if (appKey != null && appSecret != null) {
                Timber.d("OPPO push registered")
            }
        } catch (e: ClassNotFoundException) {
            Timber.w("OPPO push SDK not found")
        }
    }

    private fun registerVivo(context: Context, token: String) {
        try {
            Class.forName("com.vivo.push.PushClient")
            val appId = getMetaData(context, "VIVO_APP_ID")
            val appKey = getMetaData(context, "VIVO_APP_KEY")
            if (appId != null && appKey != null) {
                Timber.d("VIVO push registered")
            }
        } catch (e: ClassNotFoundException) {
            Timber.w("VIVO push SDK not found")
        }
    }

    private fun getMetaData(context: Context, key: String): String? {
        return try {
            val ai = context.packageManager
                .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            ai.metaData?.getString(key)
        } catch (e: Exception) { null }
    }
}
