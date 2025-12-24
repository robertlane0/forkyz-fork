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
-keep class app.crossword.yourealwaysbe.** { *; }

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

# SLF4J
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ktor
-dontwarn io.ktor.**
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt
-keep class io.ktor.** { *; }

# coil3
-dontwarn coil3.**
-keep class coil3.** { *; }

# Build config
-keep class app.crossword.yourealwaysbe.forkyz.BuildConfig { *; }

# Missing entry points and notes
-dontnote META-INF**
-dontnote module-info

# Keep all reflection methods that are dynamically accessed
-keepclassmembers class * {
    ** getPath();
    ** isParkingAllowed();
    ** getInputArguments();
    ** getThreadCpuTime(long);
    ** isThreadCpuTimeSupported();
    ** getEnclosingClass();
}

# Suppress reflection notes
-dontnote com.squareup.wire.KotlinConstructorBuilder
-dontnote com.squareup.wire.internal.FieldBinding
-dontnote com.squareup.wire.internal.ReflectionKt
-dontnote com.sun.jna.Klass
-dontnote com.sun.jna.Native
-dontnote com.sun.jna.Structure
-dontnote com.sun.jna.StructureReadContext
-dontnote com.sun.jna.platform.WindowUtils$MacWindowUtils$1
-dontnote com.sun.jna.platform.WindowUtils$X11WindowUtils
-dontnote com.sun.jna.platform.win32.COM.util.Convert
-dontnote com.sun.jna.ptr.ByReference
-dontnote junit.framework.TestSuite
-dontnote junit.runner.BaseTestRunner
-dontnote org.apache.commons.lang3.ClassUtils
-dontnote org.apache.commons.lang3.builder.Diff
-dontnote org.apache.commons.lang3.reflect.**
-dontnote org.apache.hc.core5.io.SocketSupport
-dontnote org.freedesktop.dbus.**
-dontnote org.junit.experimental.theories.internal.AllMembersSupplier
-dontnote org.junit.internal.builders.AnnotatedBuilder
-dontnote org.junit.internal.builders.SuiteMethodBuilder
-dontnote org.junit.internal.runners.SuiteMethod
-dontnote org.junit.internal.runners.TestClass
-dontnote org.junit.rules.TemporaryFolder
-dontnote org.junit.runner.FilterFactories
-dontnote org.junit.runner.manipulation.Ordering
-dontnote org.junit.runners.model.FrameworkField
-dontnote org.junit.runners.model.NoGenericTypeParametersValidator
-dontnote org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
-dontnote org.slf4j.LoggerFactory
-dontnote org.slf4j.helpers.SubstituteLogger

# Can't find dynamically referenced classes
-dontnote dev.dirs.Util
-dontnote io.ktor.http.content.BlockingBridgeKt
-dontnote org.apache.hc.client5.http.entity.BrotliDecompressingEntity
-dontnote org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy
-dontnote org.jsoup.helper.AuthenticationHandler
-dontnote org.jsoup.helper.RequestDispatch

# Keep descriptor classes
-keep class io.github.vinceglb.filekit.PlatformFile
-keep class io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
-keep class io.github.vinceglb.filekit.dialogs.compose.SaverResultLauncher
-keep class kotlinx.collections.immutable.ImmutableList
-keep class kotlinx.datetime.LocalDate
-keep class kotlinx.io.Buffer
-keep class kotlinx.io.RawSink
-keep class kotlinx.io.RawSource
-keep class kotlinx.io.Sink
-keep class kotlinx.io.Source
-keep class kotlinx.io.bytestring.ByteString
-keep class org.apache.hc.core5.http.ClassicHttpResponse
-keep class org.apache.hc.core5.http.io.support.ClassicRequestBuilder
-keep class org.hjson.JsonArray
-keep class org.hjson.JsonObject
-keep class org.hjson.JsonValue
-keep class org.jetbrains.compose.resources.DrawableResource
-keep class org.jetbrains.compose.resources.PluralStringResource
-keep class org.jetbrains.compose.resources.StringResource
-keep class org.json.JSONArray
-keep class org.json.JSONObject
-keep class org.koin.core.KoinApplication
-keep class org.koin.core.module.Module
-keep class org.koin.core.parameter.ParametersHolder
-keep class org.koin.core.scope.Scope
