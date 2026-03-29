package app.crossword.yourealwaysbe.puz

/**
 * Representation of a word on the grid
 *
 * Normally where an answer to a clue goes, a sequence of boxes, usually
 * in a line across or down, but can be any sequence.
 */
class Zone : Iterable<Position> {
    enum class Direction {
        UP, DOWN, LEFT, RIGHT, INCONCLUSIVE;

        fun reverse() : Direction {
            when (this) {
                Direction.UP -> return Direction.DOWN
                Direction.DOWN -> return Direction.UP
                Direction.LEFT -> return Direction.RIGHT
                Direction.RIGHT -> return Direction.LEFT
                Direction.INCONCLUSIVE -> return Direction.INCONCLUSIVE
            }
            // never here, but don't return null
            return Direction.LEFT
        }
    }

    private val positions = mutableListOf<Position>()

    fun hasPosition(pos : Position?) : Boolean {
        return positions.contains(pos)
    }

    fun size() : Int {
        return positions.size
    }

    val isEmpty : Boolean
        get() = size() == 0

    /**
     * Get position at index
     *
     * Index out of bounds exception if not a proper idx
     */
    fun getPosition(idx : Int) : Position {
        return positions.get(idx)
    }

    /**
     * Index of position or -1
     */
    fun indexOf(pos : Position) : Int {
        return positions.indexOf(pos)
    }

    fun addPosition(pos : Position) {
        positions.add(pos)
    }

    fun appendZone(zone : Zone?) {
        if (zone == null)
            return
        for (pos in zone)
            addPosition(pos)
    }

    override fun iterator() : MutableIterator<Position> {
        return positions.iterator()
    }

    override fun equals(other : Any?) : Boolean {
        if (this === other)
            return true

        if (other !is Zone)
            return false

        return positions == other.positions
    }

    override fun hashCode() : Int {
        return positions.hashCode()
    }

    override fun toString() : String {
        return positions.toString()
    }

    /**
     * Get the direction of travel for zone from position to position + 1
     *
     * Insists directions are only for contiguous cells, else return
     * inconclusive.
     */
    fun getDirection(position : Int) : Direction {
        // just a default
        var position = position
        if (size() <= 1) {
            return if (position <= 0)
                Direction.LEFT
            else
                Direction.RIGHT
        }

        if (position < 0)
            position = 0

        // use last two if at the end
        if (position >= size() - 1)
            position = size() - 2

        val posStart = getPosition(position)
        val posEnd = getPosition(position + 1)

        if (posStart.row == posEnd.row) {
            if (posStart.col + 1 == posEnd.col)
                return Direction.RIGHT
            else if (posStart.col == posEnd.col + 1)
                return Direction.LEFT
        } else if (posStart.col == posEnd.col) {
            if (posStart.row + 1 == posEnd.row)
                return Direction.DOWN
            else if (posStart.row == posEnd.row + 1)
                return Direction.UP
        }

        return Direction.INCONCLUSIVE
    }
}

