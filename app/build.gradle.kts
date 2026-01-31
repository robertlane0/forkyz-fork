
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.room)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.versions)
}

val appName = "Forkyz"
val appPackageName = "app.crossword.yourealwaysbe.forkyz"
val appVersion = 78

buildConfig {
    packageName(appPackageName)
    buildConfigField("APP_VERSION", "$appVersion")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // Multiplatform Parcelize from
            // https://developer.android.com/kotlin/parcelize
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=app.crossword.yourealwaysbe.forkyz.util.Parcelize"
            )
        }
    }

    jvm { }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor)
            implementation(libs.kotlin.stdlib)
            implementation(libs.androidx.concurrent.futures)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.htmlconverter)
            implementation(libs.words.to.numbers)
            implementation(libs.datetime.wheel.picker)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.apache.httpclient5)
            implementation(libs.compose.navigation)
            implementation(libs.compose.navigationevent)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)

            implementation(project(path = ":puzlib", configuration = "shadow"))
        }

        androidMain.dependencies {
            // 2.10.0 has a bug maybe that prevents background threads in
            // workers from accessing the network
            // https://issuetracker.google.com/issues/387656869
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.work.runtime)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.preference.ktx)
            implementation(libs.seismic)
            implementation(libs.koin.androidx.workmanager)

            // mainly replaced by
            // org.jetbrains.compose.material:material but a few bits still
            // needed for Android specific theming. E.g. themes.xml.
            implementation(libs.material)
        }

        androidUnitTest.dependencies {
            implementation(libs.androidx.arch.core.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit4)
            // thanks https://stackoverflow.com/a/30759769
            implementation(libs.json.legacy)
            implementation(libs.mockito.kotlin)
        }

        jvmTest.dependencies {
            implementation(libs.junit4)
            implementation(libs.mockito.kotlin)
        }

        jvmMain.dependencies {
            //pick whether to build just for the local environment, for
            //a large jar for everyone (the latter is about 30mb larger)
            implementation(compose.desktop.currentOs)
            //implementation(compose.desktop.macos_arm64)
            //implementation(compose.desktop.windows_x64)
            //implementation(compose.desktop.linux_x64)

            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.directories)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.swing)

            // thanks https://stackoverflow.com/a/30759769
            implementation(libs.json.legacy)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

android {
    namespace = appPackageName
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appPackageName
        versionCode = appVersion * 100000
        versionName = "$appVersion"
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

    room {
        schemaDirectory("$projectDir/schemas")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // overrides 4.11 dependency of
    // com.github.jgraham0325:words-to-numbers:8a97d258fd'
    configurations.all {
        resolutionStrategy.force(libs.junit4)
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
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

wire {
    kotlin {
    }
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}

compose.resources {
    packageOfResClass = "${appPackageName}.app.generated.resources"
}

compose.desktop {
    application {
        mainClass = "${appPackageName}.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appPackageName
            packageVersion = "${appVersion}.0.0"

            linux {
                shortcut = true
                menuGroup = "$appName"
                iconFile.set(
                    project.file("src/jvmMain/resources/forkykz_icon.png")
                )
                // for FileKit
                // https://filekit.mintlify.app/dialogs/setup
                modules("jdk.security.auth")
            }
            macOS {
                iconFile.set(
                    project.file("src/jvmMain/resources/forkykz_icon.icns")
                )
            }
            windows {
                shortcut = true
                menuGroup = "$appName"
                iconFile.set(
                    project.file("src/jvmMain/resources/forkykz_icon.ico")
                )
            }
        }
        buildTypes.release.proguard {
            obfuscate.set(false)
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}

