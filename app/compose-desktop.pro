# Suppress duplicate resource/class definition notes
-dontnote **

# Wire
-dontwarn com.squareup.wire.**
-keep class com.squareup.wire.** { *; }
-keep class com.squareup.wire.ProtoAdapter { *; }
-keep class com.squareup.wire.Message { *; }

# Apache HTTP Client
-dontwarn org.apache.hc.**
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.conscrypt.Conscrypt

# App classes
-keep class app.crossword.yourealwaysbe.forkyz.** { *; }
-keep class app.crossword.yourealwaysbe.forkyz.settings.Settings { *; }

# Okio
-keep class okio.** { *; }
-keepclassmembers class okio.** { *; }
-dontwarn okio.**

# SQLite
-keep class androidx.sqlite.** { *; }
-keepclassmembers class androidx.sqlite.** { *; }
-dontwarn androidx.sqlite.**

# Room
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }
-dontwarn androidx.room.**

# DataStore
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# JSoup
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.jsoup.helper.RequestAuthHandler
-dontwarn org.jsoup.helper.HttpClientExecutor

# Apache Commons Text
-keep class org.apache.commons.text.** { *; }
-keepclassmembers class org.apache.commons.text.** { *; }
-keep class org.apache.commons.text.translate.** { *; }

# Apache Commons Lang3 - reflection usage
-keep class org.apache.commons.lang3.** { *; }
-keepclassmembers class org.apache.commons.lang3.** {
    ** clone();
}

# JUnit (test framework, shouldn't be in production but keeping to avoid notes)
-dontwarn junit.**
-dontwarn org.junit.**
-keep class junit.** { *; }
-keep class org.junit.** { *; }
-keepclassmembers class * {
    ** suite();
}

# SLF4J
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ktor
-dontwarn io.ktor.**
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt
-keep class io.ktor.** { *; }

# Keep all reflection methods that are dynamically accessed
-keepclassmembers class * {
    ** getPath();
    ** isParkingAllowed();
    ** getInputArguments();
    ** getThreadCpuTime(long);
    ** isThreadCpuTimeSupported();
}


