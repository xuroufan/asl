# ================================================================
# ProGuard / R8 rules for BlackFuture Trading Platform
# ================================================================

# ------ General ------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keepattributes Exceptions
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ------ Hilt / Dagger ------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @dagger.hilt.android.internal.lifecycle.HiltViewModelMap <fields>;
}

# ------ Kotlin Serialization ------
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.hackfuture.core.model.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.hackfuture.core.model.**$$serializer { *; }
-keepclassmembers class com.hackfuture.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.hackfuture.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ------ Room ------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.hackfuture.core.database.entity.** { *; }
-dontwarn androidx.room.paging.**

# ------ Retrofit / OkHttp ------
-keep,allowobfuscation interface com.hackfuture.core.network.ApiService
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking interface retrofit2.Response
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ------ Firebase / Crashlytics ------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ------ Firebase Messaging ------
-keep class com.hackfuture.trading.push.** { *; }
-keepclassmembers class com.hackfuture.trading.push.MyFirebaseMessagingService { *; }

# ------ LeakCanary (debug only, stripped in release) ------
-dontwarn leakcanary.**
-dontwarn com.squareup.leakcanary.**

# ------ Coroutines ------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ------ Compose ------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ------ Timber ------
-keep class timber.log.Timber { *; }

# ------ WebSocket (OkHttp) ------
-keep class com.hackfuture.core.network.websocket.** { *; }

# ------ Gson / JSON (if used for notifications) ------
-keep class org.json.** { *; }

# ------ EncryptedSharedPreferences ------
-keep class androidx.security.crypto.** { *; }

# ------ R8 full mode exceptions ------
-keepclassmembers,allowshrinking,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
