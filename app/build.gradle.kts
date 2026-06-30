import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.dagger.hilt.android")
    id("androidx.room")
    kotlin("plugin.serialization") version "2.0.20"
    alias(libs.plugins.autoresconfig)
}

val releaseSigningPropertiesFile = rootProject.file("release/signing.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.isaakhanimann.journal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.huanli233.journal.open"
        minSdk = 26
        targetSdk = 36
        versionCode = 77
        versionName = "11.17.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    val localReleaseSigningConfig =
        if (releaseSigningPropertiesFile.isFile) {
            signingConfigs.create("localRelease") {
                storeFile =
                    rootProject.file(
                        requireNotNull(releaseSigningProperties.getProperty("storeFile")) {
                            "release/signing.properties 缺少 storeFile"
                        }
                    )
                storePassword =
                    requireNotNull(releaseSigningProperties.getProperty("storePassword")) {
                        "release/signing.properties 缺少 storePassword"
                    }
                keyAlias =
                    requireNotNull(releaseSigningProperties.getProperty("keyAlias")) {
                        "release/signing.properties 缺少 keyAlias"
                    }
                keyPassword =
                    requireNotNull(releaseSigningProperties.getProperty("keyPassword")) {
                        "release/signing.properties 缺少 keyPassword"
                    }
            }
        } else {
            null
        }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
            signingConfig = localReleaseSigningConfig
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

autoResConfig {
    generateClass = true
    generateRes = false
    generatedClassFullName = "com.isaakhanimann.journal.util.LangList"
    generatedArrayFirstItem = "SYSTEM"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive.navigation.suite.android)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20260522")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)

    implementation(libs.androidx.core.splashscreen)

    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
