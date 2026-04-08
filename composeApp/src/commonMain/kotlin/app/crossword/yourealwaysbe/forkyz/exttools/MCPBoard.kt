
package app.crossword.yourealwaysbe.forkyz.exttools

import java.util.Locale
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

import org.jg.wordstonumbers.WordsToNumbersUtil

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Playboard

class MCPBoard(
    hostPort : String,
    private val boardProvider : () -> Playboard?,
    private val utils : NativeBackendUtils,
) {
    companion object {
        fun getUrlFromHostPort(hostPort : String) : String {
            return MCPServer.getUrlFromHostPort(hostPort)
        }
    }

    internal val tools : List<MCPTool> = buildMCPTools()
    private val server = MCPServer(hostPort, tools)

    val hostPort : String
        get() { return server.hostPort }

    suspend fun start() {
        server.start()
    }

    suspend fun stop() {
        server.stop()
    }

    /**
     * Builds the list of MCP tools exposing puzzle commands.
     */
    private fun buildMCPTools() : List<MCPTool> {
        val noArgs = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {}
        }
        val wordArg = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("word") {
                    put("type", "string")
                    put("description", "The word to enter")
                }
            }
            putJsonArray("required") { add(JsonPrimitive("word")) }
        }
        val letterArg = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("letter") {
                    put("type", "string")
                    put("description", "A single letter to enter")
                }
            }
            putJsonArray("required") { add(JsonPrimitive("letter")) }
        }
        val listArg = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("list_name") {
                    put("type", "string")
                    put("description", "The name of the clue list")
                }
            }
            putJsonArray("required") { add(JsonPrimitive("list_name")) }
        }
        val clueByNumberArgs = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("list_name") {
                    put("type", "string")
                    put(
                        "description",
                        "The name of the list containing the clue",
                    )
                }
                putJsonObject("number") {
                    put("type", "string")
                    put("description", "The clue number")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("list_name"))
                add(JsonPrimitive("number"))
            }
        }
        val clueByIndexArgs = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("list_name") {
                    put("type", "string")
                    put(
                        "description",
                        "The name of the list containing the clue",
                    )
                }
                putJsonObject("index") {
                    put("type", "integer")
                    put(
                        "description",
                        "The 0-based index of the clue in the list",
                    )
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("list_name"))
                add(JsonPrimitive("index"))
            }
        }

        return listOf(
            MCPTool(
                "list_clue_lists",
                "List the names of all clue lists in the current puzzle",
                noArgs,
            ) { ifBoard { board ->
                board.puzzle.clueListNames.joinToString("\n")
            } },
            MCPTool(
                "list_clues",
                "List all clues in a given clue list",
                listArg,
            ) { args ->
                ifBoard { board ->
                    val listName = args?.jsonObject
                        ?.get("list_name")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    val clueList = board.puzzle.getClues(listName)
                    if (clueList == null) {
                        "No clue list named: $listName"
                    } else {
                        clueList.getClues().joinToString("\n") { clue -> (
                                "[number: ${clue.clueNumber}; "
                                + "index: ${clue.index}]: "
                                + "${clue.hint}"
                            )
                        }
                    }
                }
            },
            MCPTool(
                "get_grid_shape",
                "Get the shape of the puzzle grid: each cell is 'block', "
                    + "'cell', or 'cell(N)' where N is the clue number",
                noArgs,
            ) { ifBoard { board ->
                val puzzle = board.puzzle
                (0 until puzzle.height).joinToString("\n") { row ->
                    (0 until puzzle.width).joinToString(" ") { col ->
                        val box = puzzle.checkedGetBox(row, col)
                        when {
                            Box.isBlock(box) -> "block"
                            box?.clueNumber != null -> "cell(${box.clueNumber})"
                            else -> "cell"
                        }
                    }
                }
            } },
            MCPTool(
                "get_grid_responses",
                "Get the current letter responses in the puzzle grid. "
                    + "Each cell is shown as its letter, '_' if blank, "
                    + "or '#' if a block",
                noArgs,
            ) { ifBoard { board ->
                val puzzle = board.puzzle
                (0 until puzzle.height).joinToString("\n") { row ->
                    (0 until puzzle.width).joinToString(" ") { col ->
                        val box = puzzle.checkedGetBox(row, col)
                        when {
                            Box.isBlock(box) -> "#"
                            box?.isBlank() == true -> "_"
                            else -> box?.response ?: "#"
                        }
                    }
                }
            } },
            MCPTool(
                "get_current_clue",
                "Get the current clue and word state",
                noArgs,
            ) { getCurrentClueInfo() ?: "No current clue" },
            MCPTool(
                "play_answer",
                "Enter a word into the current word slot",
                wordArg,
            ) { args ->
                ifBoard { board ->
                    val word = args?.jsonObject
                        ?.get("word")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    val prepped = word
                        .replace("\\W+".toRegex(), "")
                        .uppercase(Locale.getDefault())
                    board.playAnswer(prepped)
                    "Played: $prepped"
                }
            },
            MCPTool(
                "play_letter",
                "Enter a single letter at the current position",
                letterArg,
            ) { args ->
                ifBoard { board ->
                    val letter = args?.jsonObject
                        ?.get("letter")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    if (letter.isNotEmpty()) {
                        board.playLetter(Character.toUpperCase(letter[0]))
                    }
                    "ok"
                }
            },
            MCPTool(
                "jump_to_clue_by_number",
                "Jump to a clue by number",
                clueByNumberArgs,
            ) { args ->
                ifBoard { board ->
                    val listName = args?.jsonObject
                        ?.get("list_name")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    val number = args?.jsonObject
                        ?.get("number")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    val clues = board.puzzle.getClues(listName)
                    val clue = clues?.getClueByNumber(number)
                    if (clue != null) {
                        board.jumpToClue(clue)
                        "ok"
                    } else {
                        "Clue not found."
                    }
                }
            },
            MCPTool(
                "jump_to_clue_by_index",
                "Jump to a clue by index in clue list",
                clueByIndexArgs,
            ) { args ->
                ifBoard { board ->
                    val listName = args?.jsonObject
                        ?.get("list_name")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    val index = args?.jsonObject
                        ?.get("index")
                        ?.jsonPrimitive
                        ?.intOrNull
                        ?: -1
                    val clues = board.puzzle.getClues(listName)
                    val clue = clues?.getClueByIndex(index)
                    if (clue != null) {
                        board.jumpToClue(clue)
                        "ok"
                    } else {
                        "Clue not found."
                    }
                }
            },
            MCPTool(
                "clear_word",
                "Clear all letters in the current word",
                noArgs,
            ) { ifBoard { it.clearWord(); "ok" } },
            MCPTool(
                "delete",
                "Delete or undo the last entered letter",
                noArgs,
            ) { ifBoard { it.deleteOrUndoResponse(); "ok" } },
            MCPTool(
                "toggle_direction",
                "Toggle between across and down",
                noArgs,
            ) { ifBoard { it.toggleSelection(); "ok" } },
            MCPTool(
                "next_clue",
                "Move to the next clue",
                noArgs,
            ) { ifBoard { it.nextWord(); "ok" } },
            MCPTool(
                "previous_clue",
                "Move to the previous clue",
                noArgs,
            ) { ifBoard { it.previousWord(); "ok" } },
            MCPTool(
                "move_left",
                "Move the cursor left one cell",
                noArgs,
            ) { ifBoard { it.moveLeft(); "ok" } },
            MCPTool(
                "move_right",
                "Move the cursor right one cell",
                noArgs,
            ) { ifBoard { it.moveRight(); "ok" } },
            MCPTool(
                "move_up",
                "Move the cursor up one cell",
                noArgs,
            ) { ifBoard { it.moveUp(); "ok" } },
            MCPTool(
                "move_down",
                "Move the cursor down one cell",
                noArgs,
            ) { ifBoard { it.moveDown(); "ok" } },
            MCPTool(
                "jump_random",
                "Jump to a random unfilled clue",
                noArgs,
            ) { ifBoard { it.pickRandomUnfilledClue(); "ok" } },
        )
    }

    private suspend fun getCurrentClueInfo() : String? {
        return board?.let { board ->
            return PlayboardTextRenderer.getAccessibleCurrentClueWord(
                utils,
                board,
                true,
            )?.toString()
        }
    }

    /**
     * Run execute if board, else return "no board" string
     */
    private fun ifBoard(execute : (Playboard) -> String) : String {
        return board?.let { execute(it) } ?: "No playboard loaded."
    }

    private val board : Playboard?
        get() { return boardProvider() }
}

