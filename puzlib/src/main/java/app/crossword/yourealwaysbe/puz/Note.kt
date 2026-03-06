
package app.crossword.yourealwaysbe.puz

import java.util.logging.Logger

data class Note(
    val scratch : String?,
    val text : String?,
    val anagramSource : String?,
    val anagramSolution : String?,
) {
    constructor(wordLength : Int) : this(
        createBlankString(wordLength),
        null,
        null,
        null
    )

    val compressedScratch : String?
        /**
         * Return null if the string is full of blanks
         */
        get() {
            if (isBlankString(scratch)) return null
            else return scratch
        }

    val compressedAnagramSource : String?
        /**
         * Return null if the string is full of blanks
         */
        get() {
            if (isBlankString(anagramSource)) return null
            else return anagramSource
        }

    val compressedAnagramSolution : String?
        /**
         * Return null if the string is full of blanks
         */
        get() {
            return if (isBlankString(anagramSolution))
                null
            else
                anagramSolution
        }

    val isEmpty : Boolean
        get() = (
            ((text?.length ?: 0) == 0)
            && ((scratch?.trim { it <= ' ' }?.length ?: 0) == 0)
            && ((anagramSource?.trim { it <= ' ' }?.length ?: 0) == 0)
            && ((anagramSolution?.trim { it <= ' ' }?.length ?: 0) == 0)
        )

    fun withScratch(scratch : String?) : Note {
        return Note(
            scratch,
            this.text,
            this.anagramSource,
            this.anagramSolution
        )
    }

    fun withText(text : String?) : Note {
        return Note(
            this.scratch,
            text,
            this.anagramSource,
            this.anagramSolution
        )
    }

    fun withAnagramSource(anagramSource : String?) : Note {
        return Note(
            this.scratch,
            this.text,
            anagramSource,
            this.anagramSolution
        )
    }

    fun withAnagramSolution(anagramSolution : String?) : Note {
        return Note(
            this.scratch,
            this.text,
            this.anagramSource,
            anagramSolution
        )
    }

    fun withAnagram(anagramSource : String?, anagramSolution : String?) : Note {
        return Note(
            this.scratch,
            this.text,
            anagramSource,
            anagramSolution
        )
    }

    /**
     * Create a copy with updated scratch letter
     *
     * Returns same object if scratch is currently null or pos is out of
     * range
     */
    fun withScratchLetter(pos : Int, letter : Char) : Note {
        val letterText = letter.toString()
        val newScratchText : String?

        if (scratch == null || pos < 0 || pos >= scratch.length) {
            LOG.warning(
                "Can't set scratch letter because scratch text not created"
            )
            return this
        }

        val len = scratch.length
        if (pos == 0) {
            newScratchText = letterText + scratch.substring(1)
        } else if (pos == len - 1) {
            newScratchText = scratch.substring(0, pos) + letterText
        } else {
            newScratchText = (
                scratch.substring(0, pos)
                + letterText
                + scratch.substring(pos + 1)
            )
        }

        return Note(
            newScratchText,
            this.text,
            this.anagramSource,
            this.anagramSolution
        )
    }

    /**
     * Return new note with scratch letter at pos deleted
     *
     * Returns same object if pos out of range
     */
    fun withDeleteScratchLetterAt(pos : Int) : Note {
        return withScratchLetter(pos, BLANK_CHAR)
    }

    override fun toString() : String {
        return (
            "Scratch: " + this.scratch + "; "
            + "Text: " + this.text + "; "
            + "Anagram source: " + this.anagramSource + "; "
            + "Anagram solution: " + this.anagramSolution
        )
    }

    companion object {
        private val LOG : Logger
            = Logger.getLogger(Note::class.java.getCanonicalName())

        private val BLANK_CHAR = Box.BLANK.get(0)

        private fun createBlankString(len : Int) : String {
            if (len == 0)
                return ""

            val padding = CharArray(len) { BLANK_CHAR }
            return String(padding)
        }

        private fun isBlankString(s : String?) : Boolean {
            if (s == null)
                return true
            for (i in 0..<s.length) {
                if (s.get(i) != BLANK_CHAR)
                    return false
            }
            return true
        }
    }
}

