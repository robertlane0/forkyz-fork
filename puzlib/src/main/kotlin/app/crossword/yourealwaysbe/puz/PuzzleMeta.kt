
package app.crossword.yourealwaysbe.puz

import kotlinx.datetime.LocalDate

data class ListNum(val listName : String, val number : Int)

/**
 * Used only for .puz file IO, not to be confused with PuzMetaFile
 *
 * TODO: rename!
 */
data class PuzzleMeta @JvmOverloads constructor(
    var author : String? = null,
    var title : String? = null,
    var source : String? = null,
    var date : LocalDate? = null,
    var percentComplete : Int = 0,
    var percentFilled : Int = 0,
    var sourceUrl : String? = null,
    var supportUrl : String? = null,
    var position : Position? = null,
    var across : Boolean = false,
    var historyList : MutableList<ListNum> = mutableListOf(),
    var acrossNotes : MutableList<Note?> = mutableListOf(),
    var downNotes : MutableList<Note?> = mutableListOf(),
    var playerNote : Note? = null,
)

