buildscript {
    dependencies {
        classpath(libs.kotlin.stdlib)
    }
}

plugins {
    alias(libs.plugins.room) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.wire) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.kotlin.plugin.parcelize) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(listOf(
                "-Xlint:fallthrough",
                "-Xmaxerrs", "5",
                "-Xmaxwarns", "5"
            ))
        }
    }
}
