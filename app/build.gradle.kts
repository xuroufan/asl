plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    // alias(libs.plugins.firebase.crashlytics) # skip crashlytics for debug
    // alias(libs.plugins.firebase.services) # skip google-services for debug
}

// ========== 应用签名配置（从环境变量读取） ==========
apply(from = "signing.gradle")

android {
    namespace = "com.hackfuture.trading"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hackfuture.trading"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-alpha1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    // ========== 多渠道环境 ==========
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // dev 环境不混淆
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-stg"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("prod") {
            dimension = "environment"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// ========== 环境配置：为每个 flavor + buildType 生成 BuildConfig 常量 ==========
android.productFlavors.forEach { flavor ->
    val apiUrl = when (flavor.name) {
        "prod" -> "\"https://api.hackfuture.com/\""
        "staging" -> "\"https://staging.api.hackfuture.com/\""
        else -> "\"https://dev.api.hackfuture.com/\""
    }
    flavor.buildConfigField("String", "API_BASE_URL", apiUrl)
    flavor.buildConfigField("String", "WS_BASE_URL",
        if (flavor.name == "prod") "\"wss://ws.hackfuture.com/\""
        else "\"wss://ws-stage.hackfuture.com/\"")
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:util"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:market"))
    implementation(project(":feature:trading"))
    implementation(project(":feature:position"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.splash.screen)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation("androidx.hilt:hilt-work:1.2.0")

    implementation(libs.timber)
    implementation(libs.security.crypto)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    debugImplementation(libs.leakcanary.android)
    implementation(libs.androidx.profileinstaller)

    // ===== 单元测试 =====
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    // ===== UI 测试 (AndroidTest) =====
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.5")
    androidTestImplementation(libs.mockk)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.53.1") {
        exclude(group = "com.google.dagger", module = "hilt-android")
    }
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.53.1")
}
