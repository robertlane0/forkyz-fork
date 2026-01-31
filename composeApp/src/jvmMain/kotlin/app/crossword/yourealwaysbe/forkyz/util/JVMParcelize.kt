
package app.crossword.yourealwaysbe.forkyz.util

// see: https://developer.android.com/kotlin/parcelize

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
actual annotation class Parcelize

actual interface Parcelable

