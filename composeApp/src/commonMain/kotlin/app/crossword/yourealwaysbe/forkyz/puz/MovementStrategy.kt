
package app.crossword.yourealwaysbe.forkyz.puz

import app.crossword.yourealwaysbe.forkyz.puz.Playboard.Word
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

interface MovementStrategy {
    fun move(board : Playboard, skipCompletedLetters : Boolean) : Word
    fun back(board : Playboard) : Word

    companion object {
        val MOVE_NEXT_ON_AXIS : MovementStrategy = object : MovementStrategy {
            override fun move(
                board : Playboard,
                skipCompletedLetters : Boolean,
            ) : Word {
                val startPos = board.highlightLetter
                val word = board.moveZoneForward(skipCompletedLetters)
                val newPos = board.highlightLetter

                if (startPos == newPos) {
                    // follow trajectory determined by last two positions
                    // in zone, or assume move right
                    var drow = 0
                    var dcol = 1
                    val zone = word.zone
                    if (zone.size() >= 2) {
                        val last = zone.getPosition(zone.size() - 1)
                        val penultimate = zone.getPosition(zone.size() - 2)
                        drow = getDirection(penultimate.row, last.row)
                        dcol = getDirection(penultimate.col, last.col)
                    }
                    val next = board.findNextDelta(
                        newPos,
                        skipCompletedLetters,
                        drow,
                        dcol,
                    )
                    if (next != null)
                        board.setHighlightLetter(next)
                }

                return word
            }

            override fun back(board : Playboard) : Word {
                val start = board.highlightLetter
                val word = board.moveZoneBack(false)
                val end = board.highlightLetter

                // if did not move, assume at start of zone
                if (start == end) {
                    // follow trajectory determined by first two positions
                    // in zone, or assume move left
                    var drow = 0
                    var dcol = -1
                    val zone = word.zone
                    if (zone.size() >= 2) {
                        val first = zone.getPosition(0)
                        val second = zone.getPosition(1)
                        drow = getDirection(second.row, first.row)
                        dcol = getDirection(second.col, first.col)
                    }
                    val next = board.findNextDelta(end, false, drow, dcol)
                    if (next != null)
                        board.setHighlightLetter(next)
                }

                return word
            }

            private fun getDirection(start : Int, end : Int) : Int {
                val diff = end - start
                if (diff < 0)
                    return -1
                if (diff > 0)
                    return 1
                return 0
            }
        }

        val STOP_ON_END : MovementStrategy = object : MovementStrategy {
            override fun move(
                board : Playboard,
                skipCompletedLetters : Boolean,
            ) : Word = board.moveZoneForward(skipCompletedLetters)

            override fun back(board : Playboard) : Word
                = board.moveZoneBack(false)
        }

        val MOVE_NEXT_CLUE : MovementStrategy = object : MovementStrategy {
            override fun move(
                board : Playboard,
                skipCompletedLetters : Boolean
            ) : Word = internalMove(board, skipCompletedLetters)

            override fun back(board : Playboard) : Word {
                val start = board.highlightLetter
                val word = board.moveZoneBack(false)
                val end = board.highlightLetter

                // if did not move, assume at end of zone and move to
                // previous zoned clue in same list
                if (start == end) {
                    val clueID = board.clueID
                    if (clueID != null) {
                        val puz = board.puzzle
                        val listName = clueID.listName
                        val curIndex = clueID.index
                        puz.getClues(listName)?.let { clues ->
                            // Move to prev clue or wrap to end of prev list
                            val firstIndex = clues.getFirstZonedIndex()
                            if (curIndex == firstIndex) {
                                val prevList = getPrevList(puz, listName)
                                puz.getClues(prevList)?.let { prevClues ->
                                    val lastIndex
                                        = prevClues.getLastZonedIndex()
                                    board.jumpToClueEnd(
                                        ClueID(prevList, lastIndex),
                                    )
                                }
                            } else {
                                val prevIndex =
                                    clues.getPreviousZonedIndex(curIndex, true)
                                board.jumpToClueEnd(ClueID(listName, prevIndex))
                            }
                        }
                    }
                }

                return word
            }

            /**
             * Keep moving to next clue unless we hit the breakPos
             *
             * If breakPos/breakClueID is null, keep moving until we hit
             * the start of the next clue again.
             *
             * If breakPos hit with same clue selected, reset to resetPos
             * (if not null) and resetClueID.
             */
            private fun internalMove(
                board : Playboard,
                skipCompletedLetters : Boolean,
                breakPos : Position? = null,
                breakClueID : ClueID? = null,
                resetPos : Position? = null,
                resetClueID : ClueID? = null,
            ) : Word {
                var breakPos = breakPos
                var breakClueID = breakClueID
                var resetPos = resetPos
                var resetClueID = resetClueID

                val start = board.highlightLetter
                val word = board.moveZoneForward(skipCompletedLetters)
                var newPos = board.highlightLetter

                if (start == newPos) {
                    val clueID = board.clueID
                    if (clueID != null) {
                        val puz = board.puzzle
                        val listName = clueID.listName
                        val curIndex = clueID.index
                        puz.getClues(listName)?.let { clues ->
                            // Move to next clue or wrap to start of next list
                            val lastIndex = clues.getLastZonedIndex()
                            if (curIndex == lastIndex) {
                                val nextList = getNextList(puz, listName)
                                puz.getClues(nextList)?.let { nextClues ->
                                    val firstIndex
                                        = nextClues.getFirstZonedIndex()
                                    board.jumpToClue(
                                        ClueID(nextList, firstIndex),
                                    )
                                }
                            } else {
                                val nextIndex =
                                    clues.getNextZonedIndex(curIndex, true)
                                board.jumpToClue(ClueID(listName, nextIndex))
                            }
                        }

                        // Check if we should skip
                        val box = board.currentBox
                        if (board.skipBox(box, skipCompletedLetters)) {
                            // only carry on if we haven't been here before
                            newPos = board.highlightLetter
                            val isBreak = (
                                breakPos == newPos
                                && breakClueID == clueID
                            )
                            if (isBreak) {
                                // didn't find anything, reset
                                if (resetClueID != null)
                                    board.jumpToClue(resetClueID)
                                if (resetPos != null)
                                    board.setHighlightLetter(resetPos)
                            } else {
                                if (breakPos == null) {
                                    breakPos = newPos
                                    breakClueID = clueID
                                }
                                if (resetPos == null) {
                                    resetPos = start
                                    resetClueID = clueID
                                }
                                internalMove(
                                    board,
                                    skipCompletedLetters,
                                    breakPos, breakClueID,
                                    resetPos, resetClueID
                                )
                            }
                        }
                    }
                }

                return word
            }

            private fun getNextList(
                puz : Puzzle,
                listName : String,
            ) : String = getListDelta(puz, listName, 1)

            private fun getPrevList(
                puz : Puzzle,
                listName : String,
            ) : String = getListDelta(puz, listName, -1)

            private fun getListDelta(
                puz : Puzzle,
                listName : String,
                delta : Int,
            ) : String {
                // inefficient to sort list every time, but we don't expect
                // many lists, and this function won't be called often
                val clueLists = puz.clueListNames.sorted()
                val curPos = clueLists.indexOf(listName)
                val n = clueLists.size
                // +n trick to avoid negatives
                // https://stackoverflow.com/a/21212090/6882587
                val nextPos = (((curPos + delta) % n) + n) % n
                return clueLists[nextPos]
            }
        }

        val MOVE_PARALLEL_WORD : MovementStrategy = object : MovementStrategy {
            override fun move(
                board : Playboard,
                skipCompletedLetters : Boolean,
            ) : Word = internalMove(board, skipCompletedLetters)

            override fun back(board : Playboard) : Word {
                val start = board.highlightLetter
                val word = board.moveZoneBack(false)
                val end = board.highlightLetter

                if (start == end) {
                    moveToParallelWordBack(board, word)
                }

                return word
            }

            private fun internalMove(
                board : Playboard,
                skipCompletedLetters : Boolean,
                resetPos : Position? = null,
            ) : Word {
                var resetPos = resetPos
                val start = board.highlightLetter
                val word = board.moveZoneForward(skipCompletedLetters)
                var newPos = board.highlightLetter

                if (start == newPos) {
                    moveToParallelWord(board, word)
                    val box = board.currentBox
                    if (board.skipBox(box, skipCompletedLetters)) {
                        // only carry on if we budged
                        newPos = board.highlightLetter
                        if (start == newPos) {
                            // didn't find anything, reset
                            board.setHighlightLetter(resetPos)
                        } else {
                            if (resetPos == null)
                                resetPos = start
                            internalMove(board, skipCompletedLetters, resetPos)
                        }
                    }
                }

                return word
            }

            /**
             * Move to nearest word in same direction that is alongside w
             *
             * Or leave unchanged if no suitable word found. Starts
             * scanning from the nearest edge of the board.
             *
             * @return new word highlighted
             */
            private fun moveToParallelWord(
                board : Playboard,
                w : Word,
            ) : Word {
                val biasStart = getWordBias(board, w)
                val across = getWordDirection(w)
                val zone = w.zone

                if (zone.size() == 0)
                    return w

                val start = zone.getPosition(0)
                val end = zone.getPosition(zone.size() - 1)
                val clue = w.clueID
                val listName = clue?.listName

                // TODO: won't really work for backwards/upwards clues
                val numRows = end.row - start.row
                val numCols = end.col - start.col

                val puz = board.puzzle

                if (across) {
                    // scan from row below underneath the word until another
                    // across word found, stop row scan if we hit the
                    // current word (for diagonals)
                    val height = puz.height
                    for (row in (start.row + 1) until height) {
                        for (offCol in 0 until numCols) {
                            val col = if (biasStart)
                                start.col + offCol
                            else
                                end.col - offCol

                            val box = puz.checkedGetBox(row, col)

                            if (box != null && box.isPartOf(clue))
                                break

                            val newClueID = box?.getIsPartOfClue(listName)

                            if (newClueID != null) {
                                board.jumpToClue(newClueID)
                                return board.currentWord
                            }
                        }
                    }
                } else {
                    // scan from col after the word in same rows until
                    // another down word found
                    val width = puz.width
                    for (col in (start.col + 1) until width) {
                        for (offRow in 0 until numRows) {
                            val row = if (biasStart)
                                start.row + offRow
                            else
                                end.row - offRow

                            val box = puz.checkedGetBox(row, col)

                            if (box != null && box.isPartOf(clue))
                                break

                            val newClueID = box?.getIsPartOfClue(listName)

                            if (newClueID != null) {
                                board.jumpToClue(newClueID)
                                return board.currentWord
                            }
                        }
                    }
                }

                return w
            }

            /**
             * If word is nearer start or end of board
             */
            private fun getWordBias(board : Playboard, w : Word) : Boolean {
                val startOffset : Int
                val endOffset : Int
                val across = getWordDirection(w)
                val zone = w.zone

                if (zone.size() == 0)
                    return true

                val start = zone.getPosition(0)
                val end = zone.getPosition(zone.size() - 1)

                if (across) {
                    val width = board.puzzle.width
                    startOffset = start.col
                    endOffset = width - end.col
                } else {
                    val height = board.puzzle.height
                    startOffset = start.row
                    endOffset = height - end.row
                }

                return startOffset <= endOffset
            }

            /**
             * Move to nearest word in same direction that is back and
             * alongside w
             *
             * Or leave unchanged if no suitable word found. Starts
             * scanning from the nearest edge of the board.
             *
             * @return new word highlighted
             */
            private fun moveToParallelWordBack(
                board : Playboard,
                w : Word,
            ) : Word {
                val boxes = board.boxes
                val biasStart = getWordBias(board, w)
                val across = getWordDirection(w)
                val zone = w.zone

                if (zone.size() == 0)
                    return w

                val start = zone.getPosition(0)
                val end = zone.getPosition(zone.size() - 1)
                val clue = w.clueID
                val listName = clue?.listName

                if (across) {
                    // scan from row below underneath the word until another
                    // across word found, stop if hit current clue
                    for (row in (start.row - 1) downTo 0) {
                        for (offCol in 0 until end.col) {
                            val col = if (biasStart)
                                start.col + offCol
                            else
                                end.col - offCol

                            val box = boxes[row][col]

                            if (box != null && box.isPartOf(clue))
                                break

                            val newClueID = box?.getIsPartOfClue(listName)

                            if (newClueID != null) {
                                board.jumpToClueEnd(newClueID)
                                return board.currentWord
                            }
                        }
                    }
                } else {
                    // scan from col after the word in same rows until
                    // another down word found, stop if hit current clue
                    for (col in (start.col - 1) downTo 0) {
                        for (offRow in 0 until end.row) {
                            val row = if (biasStart)
                                start.row + offRow
                            else
                                end.row - offRow

                            val box = boxes[row][col]

                            if (box != null && box.isPartOf(clue))
                                break

                            val newClueID = box?.getIsPartOfClue(listName)

                            if (newClueID != null) {
                                board.jumpToClueEnd(newClueID)
                                return board.currentWord
                            }
                        }
                    }
                }

                return w
            }

            /**
             * Whether to search across or down
             *
             * Assumes across unless clearly down. This isn't going to
             * really work for clues that go up or left, but this movement
             * strategy is already complicated enough...
             */
            private fun getWordDirection(word : Word) : Boolean {
                val zone = word.zone
                if (zone.size() >= 2) {
                    val pos0 = zone.getPosition(0)
                    val pos1 = zone.getPosition(1)
                    if (pos0.col == pos1.col)
                        return false
                }
                return true
            }
        }
    }

    /**
     * Return to start of unfilled word, or delegate to inner strategy
     *
     * When moving forwards or backwards, cycle current word if it has
     * not been completed. If it has, delegate to another strategy given
     * during construction. Backwards doesn't fire if the backs are
     * coming rapidly (e.g. keyboard held down).
     */
    class CycleUnfilled(
        private val innerStrategy : MovementStrategy,
        private val cycleForwards : Boolean,
        private val cycleBackwards : Boolean,
    ) : MovementStrategy {
        private var lastBack : Long = -1

        override fun move(
            board : Playboard,
            skipCompletedLetters : Boolean,
        ) : Word {
            val startPos = board.highlightLetter
            var word = board.moveZoneForward(skipCompletedLetters)
            val newPos = board.highlightLetter

            if (startPos == newPos) {
                val clue = board.clue
                val puz = board.puzzle

                if (
                    clue == null
                    || clue.zone.isEmpty
                    || board.isFilledClueID(clue.clueID)
                    || !cycleForwards
                ) {
                    word = innerStrategy.move(board, skipCompletedLetters)
                } else {
                    val start = clue.zone.getPosition(0)
                    val box = puz.checkedGetBox(start)

                    var next : Position? = start
                    if (box != null && !box.isBlank())
                        next = board.findNextZone(start, skipCompletedLetters)

                    if (next != null)
                        board.setHighlightLetter(next, clue.clueID)
                }
            }

            return word
        }

        override fun back(board : Playboard) : Word {
            val startPos = board.highlightLetter
            var word = board.moveZoneBack(false)
            val newPos = board.highlightLetter

            if (startPos == newPos) {
                val clue = board.clue
                val puz = board.puzzle

                if (
                    clue == null
                    || clue.zone.isEmpty
                    || board.isFilledClueID(clue.clueID)
                    || !cycleBackwards
                ) {
                    word = innerStrategy.back(board)
                } else if (!isPauseBackCycle()) {
                    val clueZone = clue.zone
                    val end = clueZone.getPosition(clueZone.size() - 1)
                    val box = puz.checkedGetBox(end)

                    if (box != null)
                        board.setHighlightLetter(end, clue.clueID)
                }
            }

            recordBack()

            return word
        }

        private fun recordBack() {
            lastBack = TimeUtils.currentTimeMillis()
        }

        /**
         * If should cycle this delete
         *
         * Added a pause since the last delete so that holding down
         * delete pauses at the start of the word to give the user a
         * chance to avoid cycling
         */
        private fun isPauseBackCycle() : Boolean {
            return if (lastBack < 0)
                false
            else
                TimeUtils.currentTimeMillis() < lastBack + BACK_CYCLE_DELAY
        }

        companion object {
            private const val BACK_CYCLE_DELAY : Long = 150 // ms
        }
    }
}

