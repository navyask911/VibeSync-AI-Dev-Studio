plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val oauthClientId = providers.gradleProperty("VIBESYNC_OAUTH_CLIENT_ID").orElse("")
val oauthAuthorizeEndpoint = providers.gradleProperty("VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT").orElse("")
val oauthTokenEndpoint = providers.gradleProperty("VIBESYNC_OAUTH_TOKEN_ENDPOINT").orElse("")

val productionAdMobAppId = providers.gradleProperty("ADMOB_APP_ID")
    .orElse("ca-app-pub-4783826505860771~3225002433").get()
val productionAdMobBannerId = providers.gradleProperty("ADMOB_BANNER_UNIT_ID")
    .orElse("ca-app-pub-4783826505860771/3927535003").get()
val productionAdMobInterstitialId = providers.gradleProperty("ADMOB_INTERSTITIAL_UNIT_ID")
    .orElse("ca-app-pub-4783826505860771/4941233773").get()

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// CI materializes this ignored file from the base64 GitHub secret.
// Without all signing inputs, local release builds remain available as unsigned artifacts.
val releaseKeystoreFile = rootProject.file("release-keystore.jks")
val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("KEY_ALIAS")
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
val releaseSigningAvailable = releaseKeystoreFile.isFile &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.vibesync.promptstudio"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders["adsApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
        applicationId = "com.vibesync.promptstudio"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").orElse("20261023").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").orElse("1.0.4").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OAUTH_CLIENT_ID", "\"" + oauthClientId.get().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        buildConfigField("String", "OAUTH_AUTHORIZE_ENDPOINT", "\"" + oauthAuthorizeEndpoint.get().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        buildConfigField("String", "OAUTH_TOKEN_ENDPOINT", "\"" + oauthTokenEndpoint.get().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        buildConfigField("String", "OAUTH_REDIRECT_URI", "\"vibesync://auth/callback\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
    }

    signingConfigs {
        create("release") {
            if (releaseSigningAvailable) {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["adsApplicationId"] = productionAdMobAppId
            buildConfigField("String", "ADMOB_APP_ID", buildConfigString(productionAdMobAppId))
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", buildConfigString(productionAdMobBannerId))
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", buildConfigString(productionAdMobInterstitialId))
            isMinifyEnabled = true
            signingConfig = if (releaseSigningAvailable) signingConfigs.getByName("release") else null
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            manifestPlaceholders["adsApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf(
            "/META-INF/AL2.0",
            "/META-INF/LGPL2.1",
            "/META-INF/DEPENDENCIES"
        )
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose 1.10 / Material 3 1.4 are compatible with SDK 36 and AGP 8.13.x.
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.browser:browser:1.9.0")
    implementation("com.google.android.material:material:1.13.0")
    // Google Mobile Ads and User Messaging Platform: current supported Android SDKs.
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    // RE2/J provides predictable linear-time matching for the in-app regex utility.
    implementation("com.google.re2j:re2j:1.8")

    // Official Google Play Core APIs for flexible in-app updates and in-app review.
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Direct AndroidKeyStore AES/GCM is used because EncryptedSharedPreferences is deprecated.
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("javax.inject:javax.inject:1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // Ktor 3.4.3 uses Android-compatible OkHttp dependencies for compileSdk 36.
    val ktorVersion = "3.4.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
