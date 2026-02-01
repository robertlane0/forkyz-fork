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
# Thinking this is ok since it's an annotation..
-dontwarn kotlinx.parcelize.Parcelize
-keep class app.crossword.yourealwaysbe.forkyz.**
-keep class app.crossword.yourealwaysbe.forkyz.BuildConfig { *; }
-keep class app.crossword.yourealwaysbe.forkyz.settings.Settings { *; }

# Ignore intended-to-be-optional re2j classes - only needed if using re2j for
# jsoup regex jsoup safely falls back to JDK regex if re2j not on classpath,
# but has concrete re2j refs
# See: https://github.com/jhy/jsoup/issues/2459
-dontwarn com.google.re2j.**

# Copyright 2022 Google LLC.
# SPDX-License-Identifier: Apache-2.0
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

