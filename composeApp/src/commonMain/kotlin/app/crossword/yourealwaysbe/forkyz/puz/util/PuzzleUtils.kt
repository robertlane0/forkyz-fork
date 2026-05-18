
package app.crossword.yourealwaysbe.forkyz.puz.util

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueList
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.Zone

object PuzzleUtils {
    fun joinedTop(puzzle : Puzzle, row : Int, col : Int) : Boolean {
        val boxCur = puzzle.checkedGetBox(row, col)
        val boxAbove = puzzle.checkedGetBox(row - 1, col)

        if (
            boxAbove == null
            || boxCur == null
            || isBlock(boxAbove)
            || isBlock(boxCur)
        )
            return false

        return !(boxCur.isSolidBarredTop() || boxAbove.isSolidBarredBottom())
    }

    fun joinedBottom(puzzle : Puzzle, row : Int, col : Int) : Boolean {
        val boxCur = puzzle.checkedGetBox(row, col)
        val boxBelow = puzzle.checkedGetBox(row + 1, col)

        if (
            boxBelow == null
            || boxCur == null
            || isBlock(boxBelow)
            || isBlock(boxCur)
        ) return false

        return !(boxCur.isSolidBarredBottom() || boxBelow.isSolidBarredTop())
    }

    fun joinedLeft(puzzle : Puzzle, row : Int, col : Int) : Boolean {
        val boxCur = puzzle.checkedGetBox(row, col)
        val boxLeft = puzzle.checkedGetBox(row, col - 1)

        if (
            boxLeft == null
            || boxCur == null
            || isBlock(boxLeft)
            || isBlock(boxCur)
        )
            return false

        return !(boxCur.isSolidBarredLeft() || boxLeft.isSolidBarredRight())
    }

    fun joinedRight(puzzle : Puzzle, row : Int, col : Int) : Boolean {
        val boxCur = puzzle.checkedGetBox(row, col)
        val boxRight = puzzle.checkedGetBox(row, col + 1)

        if (
            boxRight == null
            || boxCur == null
            || isBlock(boxRight)
            || isBlock(boxCur)
        )
            return false

        return !(boxCur.isSolidBarredRight() || boxRight.isSolidBarredLeft())
    }

    /**
     * Return first across list name found or null
     */
    fun getAcrossListName(puz : Puzzle) : String? {
        for (listName in puz.clueListNames) {
            val list = puz.getClues(listName)
            if (list != null && isAcrossList(puz, list))
                return listName
        }
        return null
    }

    /**
     * Returns first across list found or null
     */
    fun getAcrossList(puz : Puzzle) : ClueList? {
        val list = getAcrossListName(puz)
        if (list == null)
            return null
        else
            return puz.getClues(list)
    }

    /**
     * Check all clues follow "across rules"
     *
     * I.e. start at the right number, then go right until the box is no
     * longer joined to the right.
     */
    fun isAcrossList(puz : Puzzle, clues : ClueList) : Boolean {
        for (clue in clues) {
            if (!clue.hasZone())
                return false

            val zone = clue.zone
            if (zone.isEmpty)
                return false

            val start = zone.getPosition(0)
            var box = puz.checkedGetBox(start)

            if (box == null || isBlock(box))
                return false
            if (!box.hasClueNumber())
                return false
            if (box.clueNumber != clue.clueNumber)
                return false

            for (i in 1..<zone.size()) {
                val prev = zone.getPosition(i - 1)
                val cur = zone.getPosition(i)

                if (prev.row != cur.row)
                    return false
                if (prev.col + 1 != cur.col)
                    return false

                box = puz.checkedGetBox(cur)
                if (box == null || isBlock(box))
                    return false

                val joined = joinedRight(puz, prev.row, prev.col)
                if (!joined)
                    return false
            }

            val end = zone.getPosition(zone.size() - 1)
            val joined = joinedRight(puz, end.row, end.col)
            if (joined)
                return false
        }
        return true
    }

    /**
     * Return first down list name found or null
     */
    fun getDownListName(puz : Puzzle) : String? {
        for (listName in puz.clueListNames) {
            val list = puz.getClues(listName)
            if (list != null && isDownList(puz, list))
                return listName
        }
        return null
    }

    /**
     * Returns first down list found or null
     */
    fun getDownList(puz : Puzzle) : ClueList? {
        val list = getDownListName(puz)
        if (list == null)
            return null
        else
            return puz.getClues(list)
    }

    fun isDownList(puz : Puzzle, clues : ClueList) : Boolean {
        for (clue in clues) {
            if (!clue.hasZone())
                return false

            val zone = clue.zone
            if (zone.isEmpty)
                return false

            val start = zone.getPosition(0)
            var box = puz.checkedGetBox(start)

            if (box == null || isBlock(box))
                return false
            if (!box.hasClueNumber())
                return false
            if (box.clueNumber != clue.clueNumber)
                return false

            for (i in 1..<zone.size()) {
                val prev = zone.getPosition(i - 1)
                val cur = zone.getPosition(i)

                if (prev.row + 1 != cur.row)
                    return false
                if (prev.col != cur.col)
                    return false

                box = puz.checkedGetBox(cur)
                if (box == null || isBlock(box))
                    return false

                val joined = joinedBottom(puz, prev.row, prev.col)
                if (!joined)
                    return false
            }

            val end = zone.getPosition(zone.size() - 1)
            val joined = joinedBottom(puz, end.row, end.col)
            if (joined)
                return false
        }
        return true
    }

    /**
     * True if any clue has a non-empty zone
     */
    fun isZonesList(clues : ClueList) : Boolean {
        for (clue in clues) {
            if (clue.hasZone() && !clue.zone.isEmpty)
                return true
        }
        return false
    }

    fun getAcrossZone(puz : Puzzle, start : Position) : Zone {
        val zone = Zone()

        val row = start.row
        val col = start.col

        var off = -1
        do {
            off += 1
            zone.addPosition(Position(row, col + off))
        } while (joinedRight(puz, row, col + off))

        return zone
    }

    fun getDownZone(puz : Puzzle, start : Position) : Zone {
        val zone = Zone()

        val row = start.row
        val col = start.col

        var off = -1
        do {
            off += 1
            zone.addPosition(Position(row + off, col))
        } while (joinedBottom(puz, row + off, col))

        return zone
    }

    fun compareStringNumbers(
        num1 : String?,
        num2 : String?
    ) : Int {
        if (num1 == null && num2 == null) {
            return 0
        } else if (num1 == null) {
            return -1
        } else if (num2 == null) {
            return 1
        } else {
            try {
                val n1 = num1.toInt()
                val n2 = num2.toInt()
                return n1.compareTo(n2)
            } catch (e : NumberFormatException) {
                return num1.compareTo(num2)
            }
        }
    }
}

