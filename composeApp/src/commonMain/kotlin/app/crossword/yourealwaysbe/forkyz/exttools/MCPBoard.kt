
package app.crossword.yourealwaysbe.forkyz.exttools

import org.jg.wordstonumbers.WordsToNumbersUtil

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONArray
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer

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
        val noArgs = buildJSONObject {
            put("type", "object")
            put("properties", buildJSONObject { })
        }
        val wordArg = buildJSONObject {
            put("type", "object")
            put(
                "properties",
                buildJSONObject {
                    put(
                        "word",
                        buildJSONObject {
                            put("type", "string")
                            put("description", "The word to enter")
                        },
                    )
                },
            )
            put("required", buildJSONArray { add("word") })
        }
        val letterArg = buildJSONObject {
            put("type", "object")
            put(
                "properties",
                buildJSONObject {
                    put(
                        "letter",
                        buildJSONObject {
                            put("type", "string")
                            put("description", "A single letter to enter")
                        },
                    )
                },
            )
            put("required", buildJSONArray { add("letter") })
        }
        val listArg = buildJSONObject {
            put("type", "object")
            put(
                "properties",
                buildJSONObject {
                    put(
                        "list_name",
                        buildJSONObject {
                            put("type", "string")
                            put("description", "The name of the clue list")
                        },
                    )
                },
            )
            put("required", buildJSONArray { add("list_name") })
        }
        val clueByNumberArgs = buildJSONObject {
            put("type", "object")
            put(
                "properties",
                buildJSONObject {
                    put(
                        "list_name",
                        buildJSONObject {
                            put("type", "string")
                            put(
                                "description",
                                "The name of the list containing the clue",
                            )
                        },
                    )
                    put(
                        "number",
                        buildJSONObject {
                            put("type", "string")
                            put("description", "The clue number")
                        },
                    )
                },
            )
            put(
                "required",
                buildJSONArray {
                    add("list_name")
                    add("number")
                },
            )
        }
        val clueByIndexArgs = buildJSONObject {
            put("type", "object")
            put(
                "properties",
                buildJSONObject {
                    put(
                        "list_name",
                        buildJSONObject {
                            put("type", "string")
                            put(
                                "description",
                                "The name of the list containing the clue",
                            )
                        },
                    )
                    put(
                        "index",
                        buildJSONObject {
                            put("type", "integer")
                            put(
                                "description",
                                "The 0-based index of the clue in the list",
                            )
                        },
                    )
                },
            )
            put(
                "required",
                buildJSONArray {
                    add("list_name")
                    add("index")
                },
            )
        }

        return listOf(
            MCPTool(
                "list_clue_lists",
                "List the names of all clue lists in the current puzzle",
                noArgs,
            ) {
                ifBoard { board ->
                    board.puzzle.clueListNames.joinToString("\n")
                }
            },
            MCPTool(
                "list_clues",
                "List all clues in a given clue list",
                listArg,
            ) { args ->
                ifBoard { board ->
                    val listName = args?.optString("list_name") ?: ""
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
                    val word = args?.optString("word") ?: ""
                    val prepped = word
                        .replace("\\W+".toRegex(), "")
                        .uppercase()
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
                    val letter = args?.optString("letter") ?: ""
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
                    val listName = args?.optString("list_name") ?: ""
                    val number = args?.optString("number") ?: ""
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
                    val listName = args?.optString("list_name") ?: ""
                    val index = args?.optInt("index") ?: -1
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

