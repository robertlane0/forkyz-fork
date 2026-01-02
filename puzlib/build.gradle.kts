
plugins {
    checkstyle
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

group = "app.crossword.yourealwaysbe"

dependencies {
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.junit5.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.base64)
    implementation(libs.apache.commons.text)
    implementation(libs.hjson)
    implementation(libs.json)
    implementation(libs.jsoup)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    relocate("org.json", "app.crossword.yourealwaysbe.org.json")
}

tasks.compileJava {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()

    // -processing as adding room annotation processor library
    // causes warnings about unprocessed annotations for an
    // unclear reason
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    options.encoding = "UTF-8"
}
