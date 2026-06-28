
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.plugin.compose)
}

val appName = providers.gradleProperty("appName").get()
val appPackageName = providers.gradleProperty("appPackageName").get()
val appVersion = providers.gradleProperty("appVersion").get()

android {
    namespace = appPackageName
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appPackageName
        versionCode = appVersion.toInt() * 100000
        versionName = appVersion
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        multiDexEnabled = true
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        create("release") {
            storeFile = getReleaseKeyStoreFile()
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // needed with httpclient5-fluent
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

fun getReleaseKeyStoreFile(): File? {
    val filename = System.getenv("ANDROID_KEY_STOREFILE")
    return if (filename != null) file(filename) else null
}

dependencies {
    implementation(project(":composeApp"))

    // Koin for dependency injection
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.workmanager)

    // AndroidX dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.preference.ktx)

    // Compose
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)

    // Other
    implementation(libs.seismic)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
