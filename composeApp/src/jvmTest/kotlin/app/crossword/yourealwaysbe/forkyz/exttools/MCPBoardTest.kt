
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.runBlocking
import kotlinx.io.asSource
import kotlinx.io.buffered

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.IO
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONObject

class MCPBoardTest {
    private lateinit var puzzle : Puzzle
    private lateinit var board : Playboard
    private lateinit var mcpBoard : MCPBoard
    private lateinit var noBoard : MCPBoard

    @BeforeEach
    fun setUp() {
        puzzle = IO.loadNative(
            MCPBoardTest::class.java
                .getResourceAsStream("/test.puz")!!.asSource().buffered(),
        )!!

        board = Playboard(puzzle)

        val utils = mock<NativeBackendUtils>()
        runBlocking {
            whenever(utils.getString(any())).thenReturn("stub")
            whenever(utils.getString(any(), any())).thenReturn("stub")
        }

        mcpBoard = MCPBoard("localhost:0", { board }, utils)
        noBoard = MCPBoard("localhost:0", { null }, mock())
    }

    private suspend fun invoke(
        mcp : MCPBoard,
        name : String,
        args : JSONObject? = null,
    ) : String {
        return mcp.tools.first { it.name == name }.execute(args)
    }

    @Test
    fun testNoBoardListClueLists() = runBlocking {
        assertEquals("No playboard loaded.", invoke(noBoard, "list_clue_lists"))
    }

    @Test
    fun testNoBoardListClues() = runBlocking {
        assertEquals(
            "No playboard loaded.",
            invoke(
                noBoard,
                "list_clues",
                buildJSONObject { put("list_name", "Across") },
            ),
        )
    }

    @Test
    fun testNoBoardGetGridShape() = runBlocking {
        assertEquals("No playboard loaded.", invoke(noBoard, "get_grid_shape"))
    }

    @Test
    fun testNoBoardGetGridResponses() = runBlocking {
        assertEquals(
            "No playboard loaded.",
            invoke(noBoard, "get_grid_responses"),
        )
    }

    @Test
    fun testNoBoardGetCurrentClue() = runBlocking {
        assertEquals("No current clue", invoke(noBoard, "get_current_clue"))
    }

    @Test
    fun testNoBoardPlayAnswer() = runBlocking {
        assertEquals(
            "No playboard loaded.",
            invoke(
                noBoard,
                "play_answer",
                buildJSONObject { put("word", "test") },
            ),
        )
    }

    @Test
    fun testNoBoardPlayLetter() = runBlocking {
        assertEquals(
            "No playboard loaded.",
            invoke(
                noBoard,
                "play_letter",
                buildJSONObject { put("letter", "A") },
            ),
        )
    }

    @Test
    fun testNoBoardNavigation() = runBlocking {
        for (
            name in listOf(
                "jump_random",
                "clear_word",
                "delete",
                "toggle_direction",
                "next_clue",
                "previous_clue",
                "move_left",
                "move_right",
                "move_up",
                "move_down",
            )
        ) {
            assertEquals(
                "No playboard loaded.",
                invoke(noBoard, name),
                "Expected 'No playboard loaded.' for $name",
            )
        }
    }

    @Test
    fun testListClueLists() = runBlocking {
        val result = invoke(mcpBoard, "list_clue_lists")
        assertTrue(result.contains("Across"))
        assertTrue(result.contains("Down"))
    }

    @Test
    fun testListCluesValid() = runBlocking {
        val result = invoke(
            mcpBoard,
            "list_clues",
            buildJSONObject { put("list_name", "Across") },
        )
        assertTrue(result.contains("[number:"))
        assertTrue(result.contains("index:"))
        // first across clue from test puzzle
        assertTrue(result.contains("ring to perfection"))
    }

    @Test
    fun testListCluesInvalidList() = runBlocking {
        val result = invoke(
            mcpBoard,
            "list_clues",
            buildJSONObject { put("list_name", "Diagonal") },
        )
        assertEquals("No clue list named: Diagonal", result)
    }

    @Test
    fun testListCluesMissingArg() = runBlocking {
        val result = invoke(mcpBoard, "list_clues", null)
        assertEquals("No clue list named: ", result)
    }

    @Test
    fun testGetGridShape() = runBlocking {
        val result = invoke(mcpBoard, "get_grid_shape")
        val rows = result.lines()
        assertEquals(15, rows.size)
        assertEquals(15, rows[0].split(" ").size)
        // first cell has clue number 1
        assertTrue(rows[0].startsWith("cell(1)"))
        // known block at row 5, col 14
        assertTrue(rows[5].split(" ").last() == "block")
    }

    @Test
    fun testGetGridResponsesInitiallyBlank() = runBlocking {
        val result = invoke(mcpBoard, "get_grid_responses")
        val rows = result.lines()
        assertEquals(15, rows.size)
        // first cell is blank (no response entered yet)
        assertTrue(rows[0].startsWith("_"))
        // known block at row 5, col 14
        assertTrue(rows[5].split(" ").last() == "#")
    }

    @Test
    fun testGetGridResponsesAfterPlay() = runBlocking {
        board.playAnswer("ABCDE")
        val result = invoke(mcpBoard, "get_grid_responses")
        val firstRow = result.lines()[0].split(" ")
        assertEquals("A", firstRow[0])
        assertEquals("B", firstRow[1])
        assertEquals("C", firstRow[2])
        assertEquals("D", firstRow[3])
        assertEquals("E", firstRow[4])
    }

    @Test
    fun testPlayAnswerUppercasesAndStripsNonWord() = runBlocking {
        val result = invoke(
            mcpBoard,
            "play_answer",
            buildJSONObject { put("word", "test word") },
        )
        assertEquals("Played: TESTWORD", result)
        assertEquals("T", puzzle.checkedGetBox(0, 0)?.response)
    }

    @Test
    fun testPlayAnswerEmpty() = runBlocking {
        val result = invoke(
            mcpBoard,
            "play_answer",
            buildJSONObject { put("word", "") },
        )
        assertEquals("Played: ", result)
        assertTrue(puzzle.checkedGetBox(0, 0)?.isBlank() ?: false)
    }

    @Test
    fun testPlayLetterUppercases() = runBlocking {
        val result = invoke(
            mcpBoard,
            "play_letter",
            buildJSONObject { put("letter", "a") },
        )
        assertEquals("ok", result)
        assertEquals("A", puzzle.checkedGetBox(0, 0)?.response)
        // cursor should have advanced
        assertNotEquals(Position(0, 0), board.highlightLetter)
    }

    @Test
    fun testPlayLetterEmpty() = runBlocking {
        val posBefore = board.highlightLetter.copy()
        val result = invoke(
            mcpBoard,
            "play_letter",
            buildJSONObject { put("letter", "") },
        )
        assertEquals("ok", result)
        assertEquals(posBefore, board.highlightLetter)
        assertTrue(puzzle.checkedGetBox(0, 0)?.isBlank() ?: false)
    }

    @Test
    fun testJumpToClueByNumberFound() = runBlocking {
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_number",
            buildJSONObject {
                put("list_name", "Down")
                put("number", "5")
            },
        )
        assertEquals("ok", result)
        assertEquals("Down", board.currentWord.clueID?.listName)
        assertEquals("5", board.clue?.clueNumber)
    }

    @Test
    fun testJumpToClueByNumberNotFound() = runBlocking {
        val clueIdBefore = board.currentWord.clueID
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_number",
            buildJSONObject {
                put("list_name", "Across")
                put("number", "999")
            },
        )
        assertEquals("Clue not found.", result)
        assertEquals(clueIdBefore, board.currentWord.clueID)
    }

    @Test
    fun testJumpToClueByNumberBadList() = runBlocking {
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_number",
            buildJSONObject {
                put("list_name", "Diagonal")
                put("number", "1")
            },
        )
        assertEquals("Clue not found.", result)
    }

    @Test
    fun testJumpToClueByIndexFound() = runBlocking {
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_index",
            buildJSONObject {
                put("list_name", "Across")
                put("index", 1)
            },
        )
        assertEquals("ok", result)
        assertEquals(ClueID("Across", 1), board.currentWord.clueID)
    }

    @Test
    fun testJumpToClueByIndexNotFound() = runBlocking {
        val clueIdBefore = board.currentWord.clueID
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_index",
            buildJSONObject {
                put("list_name", "Across")
                put("index", 9999)
            },
        )
        assertEquals("Clue not found.", result)
        assertEquals(clueIdBefore, board.currentWord.clueID)
    }

    @Test
    fun testJumpToClueByIndexNegative() = runBlocking {
        val result = invoke(
            mcpBoard,
            "jump_to_clue_by_index",
            buildJSONObject {
                put("list_name", "Across")
                put("index", -1)
            },
        )
        assertEquals("Clue not found.", result)
    }

    @Test
    fun testClearWord() = runBlocking {
        board.playAnswer("RINGO")
        assertEquals("R", puzzle.checkedGetBox(0, 0)?.response)

        assertEquals("ok", invoke(mcpBoard, "clear_word"))

        assertTrue(puzzle.checkedGetBox(0, 0)?.isBlank() ?: false)
        assertTrue(puzzle.checkedGetBox(0, 1)?.isBlank() ?: false)
        assertTrue(puzzle.checkedGetBox(0, 2)?.isBlank() ?: false)
    }

    @Test
    fun testDelete() = runBlocking {
        board.playLetter('Z')
        val posBefore = board.highlightLetter.copy()

        assertEquals("ok", invoke(mcpBoard, "delete"))

        assertNotEquals(posBefore, board.highlightLetter)
    }

    @Test
    fun testToggleDirection() = runBlocking {
        val listBefore = board.currentWord.clueID?.listName
        assertEquals("ok", invoke(mcpBoard, "toggle_direction"))
        assertNotEquals(listBefore, board.currentWord.clueID?.listName)
    }

    @Test
    fun testNextClue() = runBlocking {
        val clueIdBefore = board.currentWord.clueID
        assertEquals("ok", invoke(mcpBoard, "next_clue"))
        assertNotEquals(clueIdBefore, board.currentWord.clueID)
    }

    @Test
    fun testPreviousClue() = runBlocking {
        board.nextWord()
        val clueIdBefore = board.currentWord.clueID
        assertEquals("ok", invoke(mcpBoard, "previous_clue"))
        assertNotEquals(clueIdBefore, board.currentWord.clueID)
    }

    @Test
    fun testMoveRight() = runBlocking {
        val posBefore = board.highlightLetter.copy()
        assertEquals("ok", invoke(mcpBoard, "move_right"))
        assertEquals(posBefore.row, board.highlightLetter.row)
        assertTrue(board.highlightLetter.col > posBefore.col)
    }

    @Test
    fun testMoveLeft() = runBlocking {
        board.setHighlightLetter(Position(0, 1))
        val posBefore = board.highlightLetter.copy()
        assertEquals("ok", invoke(mcpBoard, "move_left"))
        assertEquals(posBefore.row, board.highlightLetter.row)
        assertTrue(board.highlightLetter.col < posBefore.col)
    }

    @Test
    fun testMoveDown() = runBlocking {
        val posBefore = board.highlightLetter.copy()
        assertEquals("ok", invoke(mcpBoard, "move_down"))
        assertTrue(board.highlightLetter.row > posBefore.row)
        assertEquals(posBefore.col, board.highlightLetter.col)
    }

    @Test
    fun testMoveUp() = runBlocking {
        board.setHighlightLetter(Position(1, 0))
        val posBefore = board.highlightLetter.copy()
        assertEquals("ok", invoke(mcpBoard, "move_up"))
        assertTrue(board.highlightLetter.row < posBefore.row)
        assertEquals(posBefore.col, board.highlightLetter.col)
    }

    @Test
    fun testJumpRandom() = runBlocking {
        assertEquals("ok", invoke(mcpBoard, "jump_random"))
    }

    @Test
    fun testGetCurrentClueWithBoard() = runBlocking {
        val result = invoke(mcpBoard, "get_current_clue")
        assertNotEquals("No playboard loaded.", result)
        assertNotEquals("No current clue", result)
    }
}

