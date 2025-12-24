-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class app.crossword.yourealwaysbe.forkyz.**
-keep class app.crossword.yourealwaysbe.forkyz.BuildConfig { *; }
-keep class app.crossword.yourealwaysbe.forkyz.settings.Settings { *; }
-dontwarn kotlinx.datetime.Clock$System
-dontwarn kotlinx.datetime.Instant

# Copyright 2022 Google LLC.
# SPDX-License-Identifier: Apache-2.0
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
