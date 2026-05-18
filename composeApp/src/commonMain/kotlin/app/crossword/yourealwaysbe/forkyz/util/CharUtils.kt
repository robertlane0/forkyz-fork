
package app.crossword.yourealwaysbe.forkyz.util

fun Char.isISOControl() : Boolean {
    return this.code in 0x00..0x1F || this.code in 0x7F..0x9F
}

fun Char.isSurrogate() : Boolean {
    return this.category == CharCategory.SURROGATE
}

