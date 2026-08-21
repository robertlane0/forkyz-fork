
package app.crossword.yourealwaysbe.forkyz.util

fun Char.isISOControl() : Boolean {
    return this.code in 0x00..0x1F || this.code in 0x7F..0x9F
}

fun Char.isSurrogate() : Boolean {
    return this.category == CharCategory.SURROGATE
}

fun Char.isPunctuation() : Boolean {
    return this.category in PUNCTUATION_CATEGORIES
}

private val PUNCTUATION_CATEGORIES : Set<CharCategory> by lazy {
    setOf(
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION,
    )
}

