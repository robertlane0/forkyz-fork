
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.versions)
}

group = "app.crossword.yourealwaysbe"

dependencies {
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.junit5.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.apache.commons.text)
    implementation(libs.hjson)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.io)
    implementation(libs.ksoup)
    implementation(libs.ksoup.kotlinx)
    implementation(libs.ktor.client.cio)
    implementation(libs.zip)
    implementation(libs.zip.kotlinx)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
}

tasks.compileJava {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()

    // -processing as adding room annotation processor library
    // causes warnings about unprocessed annotations for an
    // unclear reason
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    options.encoding = "UTF-8"
}

