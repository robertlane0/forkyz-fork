# Wire
-dontwarn com.squareup.wire.**
-keep class com.squareup.wire.** { *; }
-keep class com.squareup.wire.ProtoAdapter { *; }
-keep class com.squareup.wire.Message { *; }

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

# Apache Commons Text
-keep class org.apache.commons.text.** { *; }
-keepclassmembers class org.apache.commons.text.** { *; }
-keep class org.apache.commons.text.translate.** { *; }

# Apache Commons Lang3 - reflection usage
-keep class org.apache.commons.lang3.** { *; }
-keepclassmembers class org.apache.commons.lang3.** {
    ** clone();
}

# SLF4J / kotlin-logging
# logback and GraalVM native-image annotations are optional dependencies
-dontwarn ch.qos.logback.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn io.github.oshai.kotlinlogging.internal.Target_**
-dontwarn io.github.oshai.kotlinlogging.logback.**
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ktor
-dontnote com.typesafe.config.impl.ConfigBeanImpl
-dontnote io.ktor.server.engine.ClassLoadersKt
-dontwarn io.ktor.**
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-keep class com.typesafe.config.** { *; }
-keep class io.ktor.** { *; }
-keep class kotlinx.datetime.** { *; }

# MCP
-keep class io.modelcontextprotocol.kotlin.** { *; }

# coil3
-dontwarn coil3.**
-keep class coil3.** { *; }

# FileKit
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.vinceglb.filekit.** { *; }
-keep class kotlinx.io.files.Path
-dontnote io.github.vinceglb.filekit.dialogs.platform.windows.jna.IShellItemArray
-dontnote io.github.vinceglb.filekit.dialogs.platform.windows.jna.ShellItemArray
# Below recommended by docs
# https://filekit.mintlify.app/dialogs/setup
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

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
-dontnote app.crossword.yourealwaysbe.org.json.JSONObject
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
-dontnote org.junit.runners.Parameterized$TestClassRunnerForParameters

# Can't find dynamically referenced classes
-dontnote com.typesafe.config.ConfigFactory
-dontnote io.github.oshai.kotlinlogging.internal.LogbackNotOnClasspath
-dontnote org.fusesource.jansi.internal.MingwSupport
-dontnote dev.dirs.Util
-dontnote io.ktor.http.content.BlockingBridgeKt
-dontnote com.google.re2j.Pattern

# Keep descriptor classes
-keep class com.fleeksoft.ksoup.nodes.Document
-keep class com.fleeksoft.ksoup.nodes.Element
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
-keep class org.jetbrains.compose.resources.DrawableResource
-keep class org.jetbrains.compose.resources.PluralStringResource
-keep class org.jetbrains.compose.resources.StringResource
-keep class org.koin.core.KoinApplication
-keep class org.koin.core.module.Module
-keep class org.koin.core.parameter.ParametersHolder
-keep class org.koin.core.scope.Scope

