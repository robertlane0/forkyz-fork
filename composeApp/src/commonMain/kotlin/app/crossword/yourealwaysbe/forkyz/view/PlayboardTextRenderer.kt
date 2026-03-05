package app.crossword.yourealwaysbe.forkyz.view

import java.util.Locale

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_box_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_word_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_first_box_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_last_box_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_only_box_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_word_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_long
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_long_no_num
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_long_no_num_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_long_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_dir
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_dir_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_num
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_num_no_dir
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_num_no_dir_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_num_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cur_box_blank
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unknown_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.word_all_blank
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.Word

object PlayboardTextRenderer {
    suspend fun getLongClueText(
        utils : NativeBackendUtils,
        clue : Clue?,
        showCount : Boolean,
    ) : String {
        if (clue == null)
            return utils.getString(Res.string.unknown_hint)

        val wordLen = if (clue.hasZone()) clue.zone.size() else -1

        return if (showCount && wordLen >= 0) {
            if (clue.hasClueNumber()) {
                utils.getString(
                    Res.string.clue_format_long_with_count,
                    clue.clueID.listName,
                    clue.clueNumber ?: "",
                    clue.hint,
                    wordLen
                )
            } else {
                utils.getString(
                    Res.string.clue_format_long_no_num_with_count,
                    clue.clueID.listName,
                    clue.hint,
                    wordLen
                )
            }
        } else {
            if (clue.hasClueNumber()) {
                utils.getString(
                    Res.string.clue_format_long,
                    clue.clueID.listName,
                    clue.clueNumber ?: "",
                    clue.hint
                )
            } else {
                utils.getString(
                    Res.string.clue_format_long_no_num,
                    clue.clueID.listName,
                    clue.hint
                )
            }
        }
    }

    suspend fun getShortClueText(
        utils : NativeBackendUtils,
        clue : Clue,
        showNumber : Boolean,
        showDirection : Boolean,
        showCount : Boolean,
    ) : String {
        val listName = getShortListName(clue)
        val displayNum = if (showNumber) clue.displayNumber else null
        val hint = clue.hint
        val hasCount = clue.hasZone()
        val count = if (hasCount) clue.zone.size() else -1

        return if (showDirection) {
            if (hasCount && showCount) {
                if (displayNum != null) {
                    utils.getString(
                        Res.string.clue_format_short_with_count,
                        displayNum, listName, hint, count
                    )
                } else {
                    utils.getString(
                        Res.string.clue_format_short_no_num_with_count,
                        listName, hint, count
                    )
                }
            } else {
                if (displayNum != null) {
                    utils.getString(
                        Res.string.clue_format_short,
                        displayNum, listName, hint
                    )
                } else {
                    utils.getString(
                        Res.string.clue_format_short_no_num,
                        listName, hint
                    )
                }
            }
        } else {
            if (hasCount && showCount) {
                if (displayNum != null) {
                    utils.getString(
                        Res.string.clue_format_short_no_dir_with_count,
                        displayNum, hint, count
                    )
                } else {
                    utils.getString(
                        Res.string.clue_format_short_no_num_no_dir_with_count,
                        hint, count
                    )
                }
            } else {
                if (displayNum != null) {
                    utils.getString(
                        Res.string.clue_format_short_no_dir,
                        displayNum, hint
                    )
                } else {
                    utils.getString(
                        Res.string.clue_format_short_no_num_no_dir,
                        hint
                    )
                }
            }
        }
    }

    suspend fun getAccessibleCurrentClueWord(
        utils : NativeBackendUtils,
        board : Playboard?,
        showCount : Boolean,
    ) : CharSequence? {
        if (board == null)
            return null

        val clue = getAccessibleCurrentClue(utils, board, showCount)
        val word
            = getAccessibleWordDescription(utils, board, board.currentWord)

        return when {
            clue == null && word == null -> null
            word == null -> clue
            clue == null -> word
            else -> utils.getString(
                Res.string.announce_clue_word_response,
                clue,
                word,
            )
        }
    }

   suspend fun getAccessibleCurrentClue(
        utils : NativeBackendUtils,
        board : Playboard?,
        showCount : Boolean,
    ) : CharSequence? {
        if (board == null)
            return null

        val clue = getLongClueText(utils, board.clue, showCount)

        return utils.getString(Res.string.announce_clue, clue)
    }

    suspend fun getAccessibleCurrentBox(
        utils : NativeBackendUtils,
        board : Playboard?,
    ) : CharSequence? {
        if (board == null)
            return null

        val box = board.currentBox
        val word = board.currentWord
        val pos = board.highlightLetter

        val index = word.indexOf(pos)

        val firstBox = (index == 0)
        val lastBox = (index == word.length - 1)

        return getAccessibleBoxDescription(
            utils,
            board.currentBox,
            firstBox,
            lastBox,
        )
    }

    suspend fun getAccessibleCurrentWord(
        utils : NativeBackendUtils,
        board : Playboard?,
    ) : CharSequence? {
        if (board == null)
            return null
        return getAccessibleWordDescription(
            utils,
            board,
            board.currentWord,
        )
    }

    /**
     * Description of box
     *
     * @param firstBox is the first box of highlighted clue
     * @param lastBox is the last box of highlighted clue
     */
    suspend fun getAccessibleBoxDescription(
        utils : NativeBackendUtils,
        box : Box?,
        firstBox : Boolean,
        lastBox : Boolean,
    ) : CharSequence? {
        if (box == null)
            return null

        val response = getAccessibleBoxResponseText(
            box,
            utils.getString(Res.string.cur_box_blank),
        )

        return when {
            firstBox && lastBox -> utils.getString(
                Res.string.announce_only_box_response,
                response,
            )
            firstBox -> utils.getString(
                Res.string.announce_first_box_response,
                response,
            )
            lastBox -> utils.getString(
                Res.string.announce_last_box_response,
                response,
            )
            else -> utils.getString(Res.string.announce_box_response, response)
        }
    }

    suspend fun getAccessibleWordDescription(
        utils : NativeBackendUtils,
        board : Playboard?,
        word : Word?,
    ) : CharSequence? {
        if (word == null || board == null)
            return null

        return board.getWordBoxes(word)?.let { boxes ->
            getAccessibleBoxesDescription(utils, boxes)
        }
    }

    suspend fun getAccessibleBoxesDescription(
        utils : NativeBackendUtils,
        boxes : Array<Box>?,
    ) : CharSequence? {
        if (boxes == null)
            return null

        val announceText = if (isAllBlank(boxes))
            utils.getString(Res.string.word_all_blank)
        else
            getAccessibleBoxesResponseText(utils, boxes)

        return utils.getString(Res.string.announce_word_response, announceText)
    }

    private suspend fun getAccessibleBoxesResponseText(
        utils : NativeBackendUtils,
        boxes : Array<Box>?,
    ) : String {
        val blank = utils.getString(Res.string.cur_box_blank)
        return buildString {
            boxes?.forEach { box ->
                if (length > 0)
                    append(" ")
                append(getAccessibleBoxResponseText(box, blank))
            }
        }
    }

    private fun getAccessibleBoxResponseText(
        box : Box?,
        blankText : String,
    ) : String {
        return when {
            box == null -> blankText
            box.isBlank() -> blankText
            else -> box.response
        }
    }

    private fun isAllBlank(boxes : Array<Box>) : Boolean {
        return boxes.all { box -> Box.isBlock(box) || box.isBlank() }
    }

    private fun getShortListName(clue : Clue?) : String {
        val listName = clue?.clueID?.listName
        return if (listName.isNullOrEmpty())
            ""
        else
            listName.substring(0, 1).lowercase(Locale.getDefault())
    }
}
