
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache.MetaRecord

class PuzMetaFile internal constructor(
    private val handler : FileHandler,
    var puzHandle : PuzHandle,
    var meta : MetaRecord,
) {
    /**
     * A unique ID for the puzzle
     */
    val mainFileURI : String
        get() = handler.getUri(puzHandle.mainFileHandle)
            .toString()

    val fileName : String
        get() = handler.getName(puzHandle.mainFileHandle)

    val fileNameNoExtension : String
        get() {
            return fileName.substring(0, fileName.lastIndexOf("."))
        }

    val title : String?
        get() = meta.title

    val date : LocalDate?
        get() = meta.date

    val fileModifiedDate : LocalDate
        get() = handler.getModifiedDate(puzHandle.mainFileHandle)

    val complete : Int
        get() = meta.percentComplete

    val filled : Int
        get() = meta.percentFilled

    val hinted : Int
        get() = meta.percentHinted

    val rating : Char
        get() = meta.rating

    val hasSolution : Boolean
        get() { return meta.hasSolution }

    /**
     * Returns something that can be used as source
     *
     * May fall back to derived alternatives if no true source set.
     */
    val source : String?
        get() = meta.source

    val author : String?
        get() = meta.author

    override fun toString() : String {
        return handler.getUri(puzHandle.mainFileHandle).toString()
    }
}

