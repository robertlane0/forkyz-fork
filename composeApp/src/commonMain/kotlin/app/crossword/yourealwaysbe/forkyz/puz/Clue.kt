
package app.crossword.yourealwaysbe.forkyz.puz

import java.util.Objects

import app.crossword.yourealwaysbe.forkyz.puz.util.HtmlUtil
import app.crossword.yourealwaysbe.forkyz.util.MutableSortedMap
import app.crossword.yourealwaysbe.forkyz.util.SortedMap
import app.crossword.yourealwaysbe.forkyz.util.SortedSet

/**
 * A clue on the board
 *
 * Must have a listName and index, number is optional.
 *
 * @param listName the name of the list the clue is in
 * @param index the index of the clue in its clue list
 * @param clueNumber may be null, but a human-facing identifier
 * @param label corresponds to cell number, but sometimes have instead
 * of number
 * @param hint the clue text
 * @param zone the cells the clue corresponds to
 * @param separators a map of positions to separators between n and
 * n-1th char
 */
class Clue constructor(
    val listName : String,
    val index : Int,
    val clueNumber : String? = null,
    val label : String? = null,
    initHint : String = "",
    var initZone : Zone? = null,
    separators : Map<Int, String>? = null,
) {
    constructor(
        listName : String,
        index : Int,
        clueNumber : String?,
        hint : String,
        zone : Zone?,
    ) : this(
        listName,
        index,
        clueNumber,
        null,
        hint,
        zone,
    )

    val clueID : ClueID = ClueID(listName, index)

    val zone : Zone = initZone ?: Zone()

    var hint : String = initHint
        set(value) {
            field = value
            // erase previously inferred separators
            inferredSeparators = null
        }

    /**
     * Flag clue
     *
     * During play, call Playboard.flagClue to make sure listeners
     * updated.
     */
    var isFlagged : Boolean = false

    /**
     * Set flag color
     *
     * 0x00rrggbb or DEFAULT_FLAG_COLOR
     *
     * During play, call Playboard.setFlagColor to make sure listeners
     * updated.
     */
    // 0x00rrggbb or DEFAULT_FLAG_COLOR
    var flagColor : Int = DEFAULT_FLAG_COLOR

    // separating characters between the n-1 and n cell in answer
    private val separators : SortedMap<Int, String> = if (separators == null)
        MutableSortedMap<Int, String>()
    else
        MutableSortedMap<Int, String>(separators)

    private var inferredSeparators : SortedMap<Int, String>? = null

    init {
        require(index >= 0) { "Clues must have non-negative index" }
    }

    fun hasClueNumber() : Boolean {
        return this.clueNumber != null
    }

    fun hasLabel() : Boolean {
        return label != null
    }

    fun hasZone() : Boolean {
        return zone.size() != 0
    }

    val isDefaultFlagColor : Boolean
        /**
         * If no custom flag color has been set
         */
        get() = isDefaultFlagColor(this.flagColor)

    val displayNumber : String?
        /**
         * The number that should be displayed with the clue
         *
         * This is label if set, else clue number if set, else null
         */
        get() {
            if (hasLabel()) return this.label
            else if (hasClueNumber()) return this.clueNumber
            else return null
        }

    /**
     * Get the separating character between the pos-1th and pos-th cell
     *
     * 0 means start of word. 1 means between 0th and 1st character.
     *
     * @param pos the position to get separator for
     * @param inferSeparators whether to infer separators from hint
     * @return the separating char or null char if no separator
     */
    fun getSeparator(pos : Int, inferSeparators : Boolean) : String? {
        var separator = separators.get(pos)
        if (separator == null && inferSeparators) {
            if (inferredSeparators == null)
                inferSeparators()
            separator = inferredSeparators?.get(pos)
        }
        return separator
    }

    /**
     * Get separator at pos (not inferred)
     */
    fun getSeparator(pos : Int) : String? {
        return getSeparator(pos, false)
    }

    /**
     * Whether clue has any separators
     *
     * @param inferSeparators whether to include inferred separators
     * when answering
     */
    fun hasSeparators(inferSeparators : Boolean = false) : Boolean {
        if (!separators.isEmpty())
            return true

        if (inferSeparators) {
            if (inferredSeparators == null)
                inferSeparators()
            return !(inferredSeparators?.isEmpty() ?: true)
        } else {
            return false
        }
    }

    val separatorPositions : SortedSet<Int>
        /**
         * Set of positions with separators (not inferred)
         *
         * Will iterate in ascending order
         */
        get() = separators.keys

    override fun toString() : String {
        return (
            clueID.toString()
            + " / " + clueNumber
            + " / " + hint
        )
    }

    override fun equals(other : Any?) : Boolean {
        if (other === this) return true
        if (other !is Clue) return false
        return clueID == other.clueID
            && clueNumber == other.clueNumber
            && label == other.label
            && hint == other.hint
            && zone == other.zone
            && isFlagged == other.isFlagged
            && separators == other.separators
    }

    override fun hashCode() : Int {
        return Objects.hash(clueID, clueNumber, label, hint, zone)
    }

    /**
     * Estimate separators from hint
     *
     * Looks for (enumeration) at end of clue. Estimation algorithm
     * based on IPuz enumeration specification, with modifications. E.g.
     * comma is interpreted as whitespace (common in most crosswords).
     * Same for + (see #173)
     */
    private fun inferSeparators() {
        val inferredSeparators = MutableSortedMap<Int, String>()

        val m = HtmlUtil.unHtmlString(hint)?.let {
            ENUMERATION_HINT_RE.matchEntire(it)
        }
        if (m == null)
            return

        val enumeration : String = m.groups[ENUMERATION_HINT_GRP]?.value ?: ""

        var len = 0
        var sepPos = 0
        val separator = ""

        var i = 0
        while (i < enumeration.length) {
            var c = enumeration.get(i)
            if (Character.isDigit(c)) {
                val digit = c.digitToIntOrNull() ?: -1
                // abandon after first word of unspecified length
                if (len == 0 && digit == 0) return
                len = 10 * len + digit
            } else if (ENUMERATION_SPECIAL.contains(c)) {
                // ignore
            } else {
                // backticks mean take next char literally
                if (c == '`' && i < enumeration.length - 1) {
                    c = enumeration.get(i + 1)
                    i += 1
                }

                sepPos = sepPos + len

                val nextSep = c.toString()
                val curSep = inferredSeparators.get(sepPos)
                if (curSep == null)
                    inferredSeparators.put(sepPos, nextSep)
                else
                    inferredSeparators.put(sepPos, curSep + nextSep)

                len = 0
            }
            i++
        }

        // replace whitespace with " "
        val spaces : MutableSet<Int> = mutableSetOf()
        for (entry in inferredSeparators.entries) {
            if (ENUMERATION_WHITESPACE_RE.matches(entry.value))
                spaces.add(entry.key)
        }
        for (pos in spaces)
            inferredSeparators.put(pos, " ")

        this.inferredSeparators = inferredSeparators
    }

    companion object {
        val DEFAULT_FLAG_COLOR : Int = -1

        // enumeration is e.g. (4, 5) at end of clue. Ignores IPuz word
        // length endings e.g. ;4, except `;4 where `; should be literal ;
        // also ignore cue words e.g. long= at start of clue. Only allows
        // digits and recognised enumeration characters to avoid
        // over-inferring.
        private val ENUMERATION_HINT_RE = (
            "^.*\\((?:\\w+=)?"
            + "([\\d\\s\\p{Punct}]*?)"
            + "(?:(?<!`);\\d+)?\\)\\s*$"
        ).toRegex()
        private const val ENUMERATION_HINT_GRP = 1
        private val ENUMERATION_WHITESPACE_RE = "^[\\s,+]+$".toRegex()

        // character that mean special things in IPuz enumerations that are
        // not fudged to something else for inferring
        private val ENUMERATION_SPECIAL = mutableSetOf<Char>('*', '^')

        fun isDefaultFlagColor(color : Int) : Boolean {
            return color == DEFAULT_FLAG_COLOR
        }
    }
}

