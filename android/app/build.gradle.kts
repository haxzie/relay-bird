import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

// Release signing is read from a gitignored keystore.properties so no secrets live in git.
// Without it the release build falls back to the debug key, which keeps a fresh clone buildable.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// CI has no properties file — it decodes the keystore from a secret and passes the rest as
// environment variables. Either source works; neither being present falls back to the debug
// key, which keeps a fresh clone and a fork buildable.
fun signingValue(property: String, environment: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(environment)

val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_FILE")
val hasReleaseKeystore = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

// Written by scripts/sync-versions.mjs from the version changesets maintains.
val appVersion = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.haxzie.relaybird"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.haxzie.relaybird"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersion.getProperty("versionCode")?.toInt() ?: 1
        versionName = appVersion.getProperty("versionName") ?: "0.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // The three BouncyCastle jars each ship the same licence files.
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE.md",
            "/META-INF/NOTICE.md",
            "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    // Needed only to build the self-signed TLS certificate: Android has no public API for
    // that, and AndroidKeyStore's free self-signed certificate cannot serve TLS (see
    // ServerIdentity).
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    // Bundled ML Kit: the play-services variant downloads its model on first use and silently
    // returns no results until it finishes, which breaks pairing with no internet.
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
