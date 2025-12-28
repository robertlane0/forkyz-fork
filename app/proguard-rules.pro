-dontwarn com.aayushatharva.brotli4j.decoder.BrotliInputStream
-dontwarn jdk.net.ExtendedSocketOptions
-dontwarn jdk.net.Sockets
-dontwarn kotlinx.datetime.Clock$System
-dontwarn kotlinx.datetime.Instant
-dontwarn org.apache.commons.compress.compressors.CompressorException
-dontwarn org.apache.commons.compress.compressors.CompressorInputStream
-dontwarn org.apache.commons.compress.compressors.CompressorOutputStream
-dontwarn org.apache.commons.compress.compressors.CompressorStreamFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class app.crossword.yourealwaysbe.forkyz.**
-keep class app.crossword.yourealwaysbe.forkyz.BuildConfig { *; }
-keep class app.crossword.yourealwaysbe.forkyz.settings.Settings { *; }

# Copyright 2022 Google LLC.
# SPDX-License-Identifier: Apache-2.0
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

