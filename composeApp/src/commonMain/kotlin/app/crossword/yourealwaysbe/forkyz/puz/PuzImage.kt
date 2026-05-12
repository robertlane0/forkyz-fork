
package app.crossword.yourealwaysbe.forkyz.puz

/**
 * An image to be on the board
 *
 * Specified by grid start position and width/height in cells. The URL can
 * either be a url to a file, or a data url in base64.
 *
 * @param tag Add an object to the image. Designed to optimise renders
 * that might want to keep a copy of their bitmap object somewhere
 * convenient.
 */
data class PuzImage @JvmOverloads constructor(
    var url : String,
    val row : Int,
    val col : Int,
    val width : Int,
    val height : Int,
    var tag : Any? = null,
)

