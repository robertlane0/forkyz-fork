
package app.crossword.yourealwaysbe.forkyz.puz

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.MovementStrategy.CycleUnfilled
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.ShowErrorsMode
import app.crossword.yourealwaysbe.forkyz.puz.io.IO
import app.crossword.yourealwaysbe.forkyz.puz.io.IOTest
import app.crossword.yourealwaysbe.forkyz.puz.io.IPuzIO
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class PlayboardTest {
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
    )
    @Retention(AnnotationRetention.RUNTIME)
    @ParameterizedTest(name = "blockobjects = {0}")
    @ValueSource(booleans = [false, true])
    private annotation class TestWithAndWithoutBlockObjects

    @TestWithAndWithoutBlockObjects
    fun testMoveUp(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)

        val board = Playboard(puz)
        moveToPosition(board, 5, 5)
        board.moveUp()

        assertAtRow(board, 4)
        board.moveUp()

        assertAtRow(board, 3)
        board.moveUp()

        assertAtRow(board, 2)
        board.moveUp()

        assertAtRow(board, 2)
        board.moveUp()

        assertAtRow(board, 2)
        board.moveUp()

        moveToPosition(board, 4, 4)

        assertAtRow(board, 4)
        board.moveUp()

        assertAtRow(board, 2)
        board.moveUp()

        assertAtRow(board, 1)
        board.moveUp()

        assertAtRow(board, 0)
        board.moveUp()

        assertAtRow(board, 0)
        board.moveUp()

        assertAtRow(board, 0)
        board.moveUp()
    }

    @TestWithAndWithoutBlockObjects
    fun testDeleteLetter(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)

        val board = Playboard(puz)
        board.setDeleteCrossingMode(Playboard.DeleteCrossingMode.DELETE)

        // Create
        //
        //  A
        // ABCDE
        //    A
        //
        // then delete from E back, row should be empty
        moveToPosition(board, 1, 0)
        board.playLetter('A')
        board.playLetter('B')
        board.playLetter('C')
        board.playLetter('D')
        board.playLetter('E')

        moveToPosition(board, 0, 1)
        board.playLetter('A')

        moveToPosition(board, 2, 3)
        board.playLetter('A')

        moveToPosition(board, 1, 4)
        for (i in 0..4)
            board.deleteOrUndoLetter()

        assertBoxBlank(puz, 1, 0)
        assertBoxBlank(puz, 1, 1)
        assertBoxBlank(puz, 1, 2)
        assertBoxBlank(puz, 1, 3)
        assertBoxBlank(puz, 1, 4)
        assertBoxLetter(puz, 0, 1, "A")
        assertBoxLetter(puz, 2, 3, "A")
    }

    @TestWithAndWithoutBlockObjects
    fun testDeleteLetterCrossingWords(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)

        val board = Playboard(puz)
        board.setDeleteCrossingMode(
            Playboard.DeleteCrossingMode.PRESERVE_FILLED_WORDS,
        )

        // Create
        //
        //  A
        // ABCDE
        //  A A
        //  A
        //
        // then delete from E back, D should remain
        // D should also be skipped
        moveToPosition(board, 1, 0)
        board.playLetter('A')
        board.playLetter('B')
        board.playLetter('C')
        board.playLetter('D')
        board.playLetter('E')

        moveToPosition(board, 0, 1)
        board.playLetter('A')
        moveToPosition(board, 2, 1)
        board.playLetter('A')
        moveToPosition(board, 3, 1)
        board.playLetter('A')

        moveToPosition(board, 2, 3)
        board.playLetter('A')

        moveToPosition(board, 1, 4)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 4)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 3)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 2)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 0)

        assertBoxLetter(puz, 1, 1, "B")
        assertBoxBlank(puz, 1, 2)
        assertBoxBlank(puz, 1, 3)
        assertBoxBlank(puz, 1, 4)
    }

    @TestWithAndWithoutBlockObjects
    fun testDeleteLetterCrossingCells(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)

        val board = Playboard(puz)
        board.setDeleteCrossingMode(
            Playboard.DeleteCrossingMode.PRESERVE_FILLED_CELLS,
        )

        // Create
        //
        //  A
        // ABCDE
        //  A A
        //  A
        //
        // then delete from E back, B and D should remain
        // B and D should also be skipped
        moveToPosition(board, 1, 0)
        board.playLetter('A')
        board.playLetter('B')
        board.playLetter('C')
        board.playLetter('D')
        board.playLetter('E')

        moveToPosition(board, 0, 1)
        board.playLetter('A')
        moveToPosition(board, 2, 1)
        board.playLetter('A')
        moveToPosition(board, 3, 1)
        board.playLetter('A')

        moveToPosition(board, 2, 3)
        board.playLetter('A')

        moveToPosition(board, 1, 4)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 4)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 2)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 0)

        assertBoxBlank(puz, 1, 0)
        assertBoxLetter(puz, 1, 1, "B")
        assertBoxBlank(puz, 1, 2)
        assertBoxLetter(puz, 1, 3, "D")
        assertBoxBlank(puz, 1, 4)
    }

    @TestWithAndWithoutBlockObjects
    fun testDeletePreserveCorrect(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)

        val board = Playboard(puz)
        board.setShowErrorsMode(ShowErrorsMode.GRID)
        board.movementStrategy = MovementStrategy.MOVE_NEXT_CLUE

        // Reveal full puzzle, but make one letter wrong
        //
        // IRINY
        //
        // then delete from Y back
        // should skip to I because it is not correct
        board.revealPuzzle()

        moveToPosition(board, 1, 2)
        board.playLetter('I')

        board.setPreserveCorrectLettersInShowErrors(true)

        moveToPosition(board, 1, 4)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 2)
        assertBoxBlank(puz, 1, 2)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 2)

        // do twice to end up across again
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 2)

        // check ok when all correct (no infinite loops)
        board.revealPuzzle()
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 1)

        board.movementStrategy = MovementStrategy.STOP_ON_END
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 0)
        board.deleteOrUndoLetter()
        assertPosition(board, 1, 0)
    }

    @TestWithAndWithoutBlockObjects
    fun testMoveNextOnAxis(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        board.movementStrategy = MovementStrategy.MOVE_NEXT_ON_AXIS

        // Across
        for (i in 0..3)
            board.playLetter('A')
        assertPosition(board, 0, 4)
        board.playLetter('A')
        assertPosition(board, 0, 6)
        for (i in 0..9)
            board.playLetter('A')
        assertPosition(board, 0, 14)

        // Down
        moveToPosition(board, 0, 6)
        board.toggleSelection()

        for (i in 0..2)
            board.playLetter('A')
        assertPosition(board, 3, 6)
        board.playLetter('A')
        assertPosition(board, 5, 6)
        for (i in 0..9)
            board.playLetter('A')
        assertPosition(board, 14, 6)

        // Down / Back
        moveToPosition(board, 5, 6)
        board.deleteOrUndoLetter()
        board.deleteOrUndoLetter()
        assertPosition(board, 3, 6)
        for (i in 0..4)
            board.deleteOrUndoLetter()

        // Across / Back
        moveToPosition(board, 0, 6)
        board.toggleSelection()
        board.deleteOrUndoLetter()
        assertPosition(board, 0, 4)
        for (i in 0..5)
            board.deleteOrUndoLetter()
        assertPosition(board, 0, 0)
    }

    @TestWithAndWithoutBlockObjects
    fun testMoveStopEnd(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        board.movementStrategy = MovementStrategy.STOP_ON_END

        // Across
        for (i in 0..3)
            board.playLetter('A')
        assertPosition(board, 0, 4)
        board.playLetter('A')
        assertPosition(board, 0, 4)

        // Down
        moveToPosition(board, 0, 6)
        board.toggleSelection()

        for (i in 0..2)
            board.playLetter('A')
        assertPosition(board, 3, 6)
        board.playLetter('A')
        assertPosition(board, 3, 6)

        // Down / Back
        moveToPosition(board, 5, 6)
        board.deleteOrUndoLetter()
        board.deleteOrUndoLetter()
        assertPosition(board, 5, 6)

        // Across / Back
        moveToPosition(board, 0, 6)
        board.toggleSelection()
        board.deleteOrUndoLetter()
        assertPosition(board, 0, 6)
    }

    @TestWithAndWithoutBlockObjects
    fun testMoveNextClue(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        board.movementStrategy = MovementStrategy.MOVE_NEXT_CLUE

        // Across
        for (i in 0..3)
            board.playLetter('A')
        assertPosition(board, 0, 4)
        board.playLetter('A')
        assertPosition(board, 0, 6)
        for (i in 0..7)
            board.playLetter('A')
        assertPosition(board, 1, 0)

        // Down
        moveToPosition(board, 0, 6)
        board.toggleSelection()

        for (i in 0..2)
            board.playLetter('A')
        assertPosition(board, 3, 6)
        board.playLetter('A')
        assertPosition(board, 0, 7)

        // Down / Back
        board.deleteOrUndoLetter()
        board.deleteOrUndoLetter()
        assertPosition(board, 3, 6)

        // Across / Back
        moveToPosition(board, 0, 6)
        board.toggleSelection()
        board.deleteOrUndoLetter()
        board.deleteOrUndoLetter()
        assertPosition(board, 0, 4)
        for (i in 0..4)
            board.deleteOrUndoLetter()
        // wrap to down clues
        assertPosition(board, 14, 10)
        for (i in 0..2)
            board.deleteOrUndoLetter()
        assertPosition(board, 14, 14)

        // Wrap across
        board.toggleSelection()
        board.playLetter('A')
        assertPosition(board, 0, 0)

        // Wrap down
        board.toggleSelection()
        board.deleteOrUndoLetter()
        // wrap to across clues
        assertPosition(board, 14, 10)
        board.playLetter('A')
        assertPosition(board, 0, 0)
    }

    @TestWithAndWithoutBlockObjects
    fun testMoveParallel(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        board.movementStrategy = MovementStrategy.MOVE_PARALLEL_WORD

        // Across
        for (i in 0..3)
            board.playLetter('A')
        assertPosition(board, 0, 4)
        board.playLetter('A')
        assertPosition(board, 1, 0)
        for (i in 0..99)
            board.playLetter('A')
        assertPosition(board, 14, 3)

        // Down
        moveToPosition(board, 0, 4)
        board.toggleSelection()

        for (i in 0..1)
            board.playLetter('A')
        assertPosition(board, 2, 4)
        board.playLetter('A')
        assertPosition(board, 0, 6)
        for (i in 0..99)
            board.playLetter('A')
        assertPosition(board, 3, 14)

        // Down / Back
        for (i in 0..4)
            board.deleteOrUndoLetter()
        assertPosition(board, 3, 13)
        for (i in 0..99)
            board.deleteOrUndoLetter()
        assertPosition(board, 0, 0)

        // Across / Back
        moveToPosition(board, 14, 3)
        board.toggleSelection()
        for (i in 0..4)
            board.deleteOrUndoLetter()
        assertPosition(board, 13, 3)
        for (i in 0..99)
            board.deleteOrUndoLetter()
        assertPosition(board, 0, 0)
    }

    @TestWithAndWithoutBlockObjects
    fun testCycleUnfilled(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        board.isSkipCompletedLetters = true
        board.movementStrategy = CycleUnfilled(
            MovementStrategy.MOVE_NEXT_ON_AXIS,
            true,
            true,
        )

        // Across
        board.playLetter('A')
        moveToPosition(board, 0, 4)
        board.playLetter('A')
        assertPosition(board, 0, 1)
        board.playLetter('A')
        board.playLetter('A')
        board.playLetter('A')
        assertPosition(board, 0, 6)
        moveToPosition(board, 0, 9)
        board.playLetter('A')
        assertPosition(board, 0, 6)

        // make sure don't toggle direction if loop to same spot
        moveToPosition(board, 0, 4)
        val cid = board.clueID
        board.playLetter(Box.BLANK)
        assertPosition(board, 0, 4)
        Assertions.assertEquals(cid, board.clueID)

        // Down
        moveToPosition(board, 0, 0)
        board.toggleSelection()

        board.playLetter('A')
        moveToPosition(board, 3, 0)
        board.playLetter('A')
        assertPosition(board, 1, 0)
        board.playLetter('A')
        board.playLetter('A')
        assertPosition(board, 5, 0)

        // Down / Back
        moveToPosition(board, 11, 0)
        board.deleteOrUndoLetter()
        assertPosition(board, 14, 0)

        // Across / Back
        moveToPosition(board, 0, 11)
        board.toggleSelection()

        // cycle only happens if we delay delete
        board.deleteOrUndoLetter()
        assertPosition(board, 0, 11)
        board.deleteOrUndoLetter()
        assertPosition(board, 0, 11)
        try {
            Thread.sleep(200)
            board.deleteOrUndoLetter()
            assertPosition(board, 0, 14)
        } catch (e : InterruptedException) {
            Assertions.fail(e)
        }
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayFullMoveAxis(blockObjects : Boolean) {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_NEXT_ON_AXIS)
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayFullMoveStopEnd(blockObjects : Boolean) {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.STOP_ON_END)
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayFullMoveNextClue(blockObjects : Boolean) {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_NEXT_CLUE)
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayFullMoveParallelWord(blockObjects : Boolean) {
        checkNoMoveFullGrid(blockObjects, MovementStrategy.MOVE_PARALLEL_WORD)
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayFullMoveCycleUnfilled(blockObjects : Boolean) {
        checkNoMoveFullGrid(
            blockObjects,
            CycleUnfilled(
                MovementStrategy.MOVE_NEXT_ON_AXIS,
                true,
                true,
            )
        )
    }

    @TestWithAndWithoutBlockObjects
    fun testNextWord(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // reveal grid bar one character in the middle of the grid
        // select clue after it
        // test go forward
        board.revealPuzzle()

        moveToPosition(board, 2, 5)
        board.deleteOrUndoLetter()

        // first without skipping, end up at start of word
        moveToPosition(board, 1, 12)
        board.nextWord()
        assertPosition(board, 2, 0)

        // then with skipping, end up at empty cell
        board.isSkipCompletedLetters = true
        moveToPosition(board, 1, 12)
        board.nextWord()
        assertPosition(board, 2, 5)

        // if go back again, keep same cell (but end up in down direction)
        board.nextWord()
        assertPosition(board, 2, 5)

        // fill board, check nextWord doesn't loop but ends up at
        // end of word
        board.toggleSelection()
        board.playLetter('A')
        board.nextWord()
        assertPosition(board, 2, 9)
    }

    @TestWithAndWithoutBlockObjects
    fun testPreviousWord(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // reveal grid bar one character in the middle of the grid
        // select clue after it
        // test go back
        board.revealPuzzle()

        moveToPosition(board, 2, 5)
        board.deleteOrUndoLetter()

        // first without skipping, end up at start of word
        moveToPosition(board, 2, 12)
        board.previousWord()
        assertPosition(board, 2, 0)

        // then with skipping, end up at empty cell
        board.isSkipCompletedLetters = true
        moveToPosition(board, 2, 12)
        board.previousWord()
        assertPosition(board, 2, 5)

        // if go back again, keep same cell (but end up in down direction)
        board.previousWord()
        assertPosition(board, 2, 5)

        // fill board, check previousWord doesn't loop but ends up at
        // start of word
        board.toggleSelection()
        board.playLetter('A')
        board.previousWord()
        assertPosition(board, 2, 0)
    }

    @TestWithAndWithoutBlockObjects
    fun testDetachedWord(blockObjects : Boolean) {
        val puz = loadTestDetachedPuz(blockObjects)
        val board = Playboard(puz)
        moveToPosition(board, 8, 2)
        val word = board.currentWord
        val boardZone = word.zone

        val expectedZoneArr : Array<IntArray> = arrayOf<IntArray>(
            intArrayOf(7, 1),
            intArrayOf(7, 2),
            intArrayOf(7, 3),
            intArrayOf(7, 5),
            intArrayOf(8, 2),
            intArrayOf(9, 2),
            intArrayOf(10, 2),
            intArrayOf(9, 1),
            intArrayOf(9, 3),
            intArrayOf(12, 2),
            intArrayOf(14, 2),
        )
        val expectedZone = Zone()
        for (pos in expectedZoneArr)
            expectedZone.addPosition(Position(pos[0], pos[1]))

        Assertions.assertEquals(boardZone, expectedZone)
    }

    @TestWithAndWithoutBlockObjects
    fun testRevealInitialLetter(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)
        val pos1 = Position(14, 5)
        val box1 = puz.checkedGetBox(pos1)
        val box2 = puz.checkedGetBox(0, 3)
        val box3 = puz.checkedGetBox(3, 6)

        board.setHighlightLetter(pos1)
        box1!!.response = "_"

        board.revealInitialLetter()
        Assertions.assertEquals(box1.response, "_")

        box1.initialValue = "*"
        box2!!.initialValue = "*"

        board.revealInitialLetter()
        Assertions.assertEquals(box1.response, "*")
        Assertions.assertTrue(box2.isBlank())
        Assertions.assertTrue(box3!!.isBlank())

        board.revealInitialLetters()
        Assertions.assertEquals(box1.response, "*")
        Assertions.assertEquals(box2.response, "*")
        Assertions.assertTrue(box3.isBlank())
    }

    @Test
    fun testClueHasErrors() {
        val puz = loadTestPuz(false)
        val board = Playboard(puz)

        moveToPosition(board, 1, 0)
        val cid = board.clueID

        Assertions.assertFalse(board.clueHasErrors(cid))

        board.revealPuzzle()
        Assertions.assertFalse(board.clueHasErrors(cid))

        val box = puz.checkedGetBox(1, 2)!!
        box.response = "I"
        Assertions.assertTrue(board.clueHasErrors(cid))

        box.response = box.solution!!
        Assertions.assertFalse(board.clueHasErrors(cid))
    }

    @Test
    fun testEditHint() {
        val puz = loadTestPuz(false)
        val board = Playboard(puz)

        val cid = ClueID("Across", 10)
        val clue = puz.getClue(cid)
        Assertions.assertEquals(clue!!.hint, "Surprised reaction")

        val notified = AtomicBoolean(false)
        val listener = PlayboardListener { changes : PlayboardChanges ->
            val changedClues = changes.changedClueIDs
            Assertions.assertEquals(changedClues.size, 1)
            Assertions.assertTrue(changedClues.contains(cid))
            notified.set(true)
        }

        board.addListener(listener)
        board.editClue(clue, "Test update")
        board.removeListener(listener)

        Assertions.assertEquals(clue.hint, "Test update")
        Assertions.assertTrue(notified.get())
    }

    @Test
    fun testFlagClue() {
        val puz = loadTestPuz(false)
        val board = Playboard(puz)

        val cid = ClueID("Across", 10)
        val clue = puz.getClue(cid)

        Assertions.assertFalse(clue!!.isFlagged)

        val notified = AtomicBoolean(false)
        val listener = PlayboardListener { changes : PlayboardChanges ->
            val changedClues = changes.changedClueIDs
            Assertions.assertEquals(changedClues.size, 1)
            Assertions.assertTrue(changedClues.contains(cid))
            notified.set(true)
        }

        board.addListener(listener)
        board.flagClue(clue, true)
        board.removeListener(listener)

        Assertions.assertTrue(clue.isFlagged)
        Assertions.assertTrue(notified.get())
    }

    /**
     * Test failure https://gitlab.com/Hague/forkyz/-/issues/86
     */
    @TestWithAndWithoutBlockObjects
    fun testNextWordBug86(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        moveToPosition(board, 0, 0)
        board.nextWord()

        assertPosition(board, 0, 6)
        Assertions.assertEquals(board.clueID!!.listName, "Across")
    }

    @Test
    fun testPlayLetterUndoStack() {
        val puz = loadTestPuz(false)
        val board = Playboard(puz)

        board.setPlayLetterUndoStackEnabled(true)

        moveToPosition(board, 0, 0)
        board.playLetter('A')
        board.playLetter('B')
        board.playLetter('C')

        // undo stack cleared to here
        moveToPosition(board, 0, 1)
        board.playLetter('D')
        board.playLetter('E')

        // undoes E
        board.deleteOrUndoLetter()
        // undoes D
        board.deleteOrUndoLetter()

        // wait at end of undo stack
        try {
            Thread.sleep(200)
        } catch (e : InterruptedException) {
            Assertions.fail(e)
        }

        assertBoxLetter(puz, 0, 1, "B")
        assertBoxLetter(puz, 0, 2, "C")

        // undo stack clear, deletes B
        board.deleteOrUndoLetter()
        // undo stack clear, deletes A
        board.deleteOrUndoLetter()

        assertBoxBlank(puz, 0, 0)
        assertBoxBlank(puz, 0, 1)
    }

    @TestWithAndWithoutBlockObjects
    fun testToggleBeforeMove() {
        val puz = loadTestPuz(false)
        val board = Playboard(puz)

        // test without toggle first
        board.moveDown()
        assertPosition(board, 1, 0)
        board.moveRight()
        assertPosition(board, 1, 1)
        board.moveUp()
        assertPosition(board, 0, 1)
        board.moveLeft()
        assertPosition(board, 0, 0)

        board.setToggleBeforeMove(true)

        assertClue(board, "Across", "1")
        board.moveDown()
        assertPosition(board, 0, 0)
        assertClue(board, "Down", "1")
        board.moveDown()
        assertPosition(board, 1, 0)
        assertClue(board, "Down", "1")
        board.moveRight()
        assertPosition(board, 1, 0)
        assertClue(board, "Across", "14")
        board.moveRight()
        assertPosition(board, 1, 1)
        assertClue(board, "Across", "14")
        board.moveUp()
        assertPosition(board, 1, 1)
        assertClue(board, "Down", "2")
        board.moveUp()
        assertPosition(board, 0, 1)
        assertClue(board, "Down", "2")
        board.moveLeft()
        assertPosition(board, 0, 1)
        assertClue(board, "Across", "1")
        board.moveLeft()
        assertPosition(board, 0, 0)
        assertClue(board, "Across", "1")
    }

    @TestWithAndWithoutBlockObjects
    fun testPlayAnswer(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // Move to middle of clue, enter answer, test set in full clue
        moveToPosition(board, 1, 2)
        board.playAnswer("ABCDE")

        assertBoxLetter(puz, 1, 0, "A")
        assertBoxLetter(puz, 1, 1, "B")
        assertBoxLetter(puz, 1, 2, "C")
        assertBoxLetter(puz, 1, 3, "D")
        assertBoxLetter(puz, 1, 4, "E")

        assertPosition(board, 1, 4)
    }

    fun testPlayLetters(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // Move to middle of clue, enter letters, test set from cur pos
        moveToPosition(board, 1, 2)
        board.playLetters("ABCDE")

        assertBoxBlank(puz, 1, 0)
        assertBoxBlank(puz, 1, 1)
        assertBoxLetter(puz, 1, 2, "A")
        assertBoxLetter(puz, 1, 3, "B")
        assertBoxLetter(puz, 1, 4, "C")

        assertPosition(board, 1, 4)
    }

    fun testPlayScratchLetters(blockObjects : Boolean) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // Move to middle of clue, enter scratch letters, test set from cur pos
        moveToPosition(board, 1, 2)
        board.playScratchLetters("ABCDE")

        assertBoxBlank(puz, 1, 0)
        assertBoxBlank(puz, 1, 1)
        assertBoxBlank(puz, 1, 2)
        assertBoxBlank(puz, 1, 3)
        assertBoxBlank(puz, 1, 4)
        assertPosition(board, 1, 4)

        Assertions.assertEquals(
            "  ABC",
            puz.getNote(board.clueID)!!.scratch,
        )
    }

    fun testPlayScratchLettersSpace(
        blockObjects : Boolean
    ) {
        val puz = loadTestPuz(blockObjects)
        val board = Playboard(puz)

        // Move to middle of clue, enter scratch letters, test set from cur pos
        // spaces should be ignored
        moveToPosition(board, 1, 2)
        board.playScratchLetters(" AB CDE")

        assertBoxBlank(puz, 1, 0)
        assertBoxBlank(puz, 1, 1)
        assertBoxBlank(puz, 1, 2)
        assertBoxBlank(puz, 1, 3)
        assertBoxBlank(puz, 1, 4)
        assertPosition(board, 1, 4)

        Assertions.assertEquals(
            "  ABC",
            puz.getNote(board.clueID)!!.scratch,
        )
    }

    private fun checkNoMoveFullGrid(
        blockObjects : Boolean, moveStrat : MovementStrategy
    ) {
        // pick a box this far from the edges to test
        val CHECK_OFFSET = 5

        val puz = loadTestPuz(blockObjects)

        val width = puz.width
        val height = puz.height
        val boxes : Array<Array<Box?>> = puz.boxes

        val board = Playboard(puz)
        board.movementStrategy = moveStrat
        board.isSkipCompletedLetters = true

        var checkPos : Position? = null

        // fill grid, find checkPos
        for (row in 0..<height) {
            for (col in 0..<width) {
                val box = boxes[row][col]
                if (box != null && !isBlock(box)) {
                    if (
                        checkPos == null
                        && row > CHECK_OFFSET
                        && col > CHECK_OFFSET
                    ) {
                        checkPos = Position(row, col)
                        box.setBlank()
                    } else {
                        box.setResponse('A')
                    }
                }
            }
        }

        board.setHighlightLetter(checkPos)
        board.playLetter('A')

        Assertions.assertEquals(checkPos, board.highlightLetter)
    }

    private fun assertBoxBlank(puz : Puzzle, row : Int, col : Int) {
        Assertions.assertTrue(puz.checkedGetBox(row, col)!!.isBlank())
    }

    private fun assertBoxLetter(
        puz : Puzzle, row : Int, col : Int, letter : String?
    ) {
        Assertions.assertEquals(puz.checkedGetBox(row, col)!!.response, letter)
    }

    private fun assertPosition(board : Playboard, row : Int, col : Int) {
        Assertions.assertEquals(board.highlightLetter, Position(row, col))
    }

    private fun assertClue(
        board : Playboard,
        listName : String?,
        number : String?,
    ) {
        val clue = board.clue
        Assertions.assertEquals(clue!!.clueID.listName, listName)
        Assertions.assertEquals(clue.clueNumber, number)
    }

    private fun assertAtRow(board : Playboard, row : Int) {
        Assertions.assertEquals(board.highlightLetter.row, row)
    }

    private fun moveToPosition(board : Playboard, row : Int, col : Int) {
        val pos = Position(row, col)
        if (pos != board.highlightLetter)
            board.setHighlightLetter(pos)
    }

    /**
     * Load test puzzle
     *
     * @param blockObjects whether to use objects for blank boxes
     * instead of null
     */
    private fun loadTestPuz(blockObjects : Boolean) : Puzzle {
        val puz = runBlocking {
            SourceUtils.getResource("/test.puz").use {
                IO.loadNative(it)
            }
        }

        if (blockObjects)
            insertBlockObjects(puz)

        return puz!!
    }

    /**
     * Load test puzzle with detached cells
     *
     * @param blockObjects whether to use objects for blank boxes
     * instead of null
     */
    private fun loadTestDetachedPuz(blockObjects : Boolean) : Puzzle {
        val puz = SourceUtils.getResource("/detachedCells.ipuz").use {
            IPuzIO.readPuzzle(it)
        }
        if (blockObjects)
            insertBlockObjects(puz!!)

        return puz!!
    }

    /**
     * Replace null boxes with a block object
     */
    private fun insertBlockObjects(puz : Puzzle?) {
        if (puz == null)
            return

        val block = Box()
        block.isBlock = true

        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in 0..<puz.height) {
            for (col in 0..<puz.width) {
                if (boxes[row][col] == null)
                    boxes[row][col] = block
            }
        }
    }
}

