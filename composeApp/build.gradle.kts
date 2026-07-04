
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.room)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.versions)
}

val appName : String = providers.gradleProperty("appName").get()
val appPackageName : String = providers.gradleProperty("appPackageName").get()
val appVersion : String = providers.gradleProperty("appVersion").get()
val appLibraryNamespace = "$appPackageName.lib"

buildConfig {
    packageName(appPackageName)
    buildConfigField("APP_VERSION", appVersion)
}

kotlin {
    android {
        namespace = appLibraryNamespace
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            // Multiplatform Parcelize from
            // https://developer.android.com/kotlin/parcelize
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=app.crossword.yourealwaysbe.forkyz.util.Parcelize"
            )
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("proguard-rules.pro")
            }
        }

        androidResources {
            enable = true
        }

        withHostTestBuilder {}.configure {}
    }

    jvm { }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.concurrent.futures)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.codepoints)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.compose.navigation)
            implementation(libs.compose.navigationevent)
            implementation(libs.date.picker.spinner)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.htmlconverter)
            implementation(libs.json5)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.json.io)
            implementation(libs.ksoup)
            implementation(libs.ksoup.kotlinx)
            implementation(libs.ktor.server.cio)
            implementation(libs.zip)
            implementation(libs.zip.kotlinx)
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
            implementation(libs.mcp.sdk.server)

            // mainly replaced by
            // org.jetbrains.compose.material:material but a few bits still
            // needed for Android specific theming. E.g. themes.xml.
            implementation(libs.material)
        }

        // weird that this works when docs suggest
        // androidHostTest.dependencies
        // https://developer.android.com/kotlin/multiplatform/plugin#android-kmp_1
        // possible AGP 9 issue
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.arch.core.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit5.jupiter)
            implementation(libs.mockito.kotlin)
            runtimeOnly(libs.junit.platform.launcher)
        }

        jvmTest.dependencies {
            implementation(libs.mockito.kotlin)
            implementation(libs.junit5.jupiter)
            implementation(libs.junit5.jupiter.params)
            runtimeOnly(libs.junit.platform.launcher)
        }

        jvmMain.dependencies {
            //pick whether to build just for the local environment, for
            //a large jar for everyone (the latter is about 30mb larger)
            implementation(compose.desktop.currentOs)
            //implementation(compose.desktop.macos_arm64)
            //implementation(compose.desktop.windows_x64)
            //implementation(compose.desktop.linux_x64)

            implementation(libs.directories)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.mcp.sdk.server)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

lint {
    lintConfig = file("lint.xml")
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
    publicResClass = true
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "${appPackageName}.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appPackageName
            packageVersion = "$appVersion.0.0"

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
            // TODO: remove when default proguard supports Java 25
            version.set("7.8.0")
            obfuscate.set(false)
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}

