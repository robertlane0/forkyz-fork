
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

import app.crossword.yourealwaysbe.forkyz.settings.KeyboardBindings
import app.crossword.yourealwaysbe.forkyz.util.ForkyzKey
import app.crossword.yourealwaysbe.forkyz.util.ForkyzKeySymbol

// Returns the printable character for a key event, uppercased, or null.
fun keyEventToResponse(event : KeyEvent) : String? {
    val codePoint = event.utf16CodePoint

    // C0 controls (U+0000–U+001F), DEL (U+007F), C1 controls (U+0080–U+009F)
    if (codePoint in 0x00..0x1F || codePoint in 0x7F..0x9F)
        return null

    return if (codePoint <= 0xFFFF) {
        val char = codePoint.toChar()
        val cat = char.category
        if (cat == CharCategory.UNASSIGNED || cat == CharCategory.SURROGATE)
            null
        else
            char.toString().uppercase()
    } else {
        val shifted = codePoint - 0x10000
        val high = ((shifted shr 10) + 0xD800).toChar()
        val low = ((shifted and 0x3FF) + 0xDC00).toChar()
        charArrayOf(high, low).concatToString().uppercase()
    }
}

/**
 * Checks event against given keyboard shortcuts
 *
 * Calls back on action, returns true if handled. Always assumed
 * handled if matches action for which handler given as arg.
 */
fun onKeyEventKeyboardBindings(
    event : KeyEvent,
    binds : KeyboardBindings,
    onAnnounceBoxDown : (() -> Unit)? = null,
    onAnnounceClueDown : (() -> Unit)? = null,
    onBackDown : (() -> Unit)? = null,
    onClueListDown : (() -> Unit)? = null,
    onClueNotesDown : (() -> Unit)? = null,
    onExitDown : (() -> Unit)? = null,
    onFitToScreenDown : (() -> Unit)? = null,
    onHelpDown : (() -> Unit)? = null,
    onInsertSpecialDown : (() -> Unit)? = null,
    onNextClueDown : (() -> Unit)? = null,
    onOpenCrosswordSolverDown : (() -> Unit)? = null,
    onOpenDictionaryDown : (() -> Unit)? = null,
    onOpenInfoDown : (() -> Unit)? = null,
    onOpenShareUrlDown : (() -> Unit)? = null,
    onPrevClueDown : (() -> Unit)? = null,
    onPuzzleNotesDown : (() -> Unit)? = null,
    onRandomClueDown : (() -> Unit)? = null,
    onRevealErrorsDown : (() -> Unit)? = null,
    onRevealLetterDown : (() -> Unit)? = null,
    onRevealPuzzleDown : (() -> Unit)? = null,
    onRevealWordDown : (() -> Unit)? = null,
    onSearchDuckduckgoDown : (() -> Unit)? = null,
    onSearchFifteenSquaredDown : (() -> Unit)? = null,
    onSettingsDown : (() -> Unit)? = null,
    onShareClueDown : (() -> Unit)? = null,
    onShareClueWithBoardDown : (() -> Unit)? = null,
    onSharePuzzleDown : (() -> Unit)? = null,
    onSharePuzzleBlankDown : (() -> Unit)? = null,
    onSharePuzzleNoIpuzDown : (() -> Unit)? = null,
    onSharePuzzleOriginalDown : (() -> Unit)? = null,
    onShuffleAnagramDown : (() -> Unit)? = null,
    onToggleCellFlagDown : (() -> Unit)? = null,
    onToggleClueFlagDown : (() -> Unit)? = null,
    onToggleScratchModeDown : (() -> Unit)? = null,
    onToggleSelectionDown : (() -> Unit)? = null,
    onToggleShowErrorsCursorDown : (() -> Unit)? = null,
    onToggleShowErrorsGridDown : (() -> Unit)? = null,
    onToggleShowErrorsWordCellsDown : (() -> Unit)? = null,
    onToggleShowErrorsWordWordDown : (() -> Unit)? = null,
    onVoiceInputDown : (() -> Unit)? = null,
    onZoomInDown : (() -> Unit)? = null,
    onZoomInMaxDown : (() -> Unit)? = null,
    onZoomOutDown : (() -> Unit)? = null,
    onZoomResetDown : (() -> Unit)? = null,
) : Boolean {
    return getForkyzKey(event)?.let { key ->
        val isDown = event.type == KeyEventType.KeyDown

        fun ifKey(boundKey : ForkyzKey?, action : (() -> Unit)?) : Boolean {
            if (action != null && key == boundKey) {
                if (isDown)
                    action()
                return true
            }
            return false
        }

        return (
            ifKey(binds.announceBox, onAnnounceBoxDown)
            || ifKey(binds.announceClue, onAnnounceClueDown)
            || ifKey(binds.back, onBackDown)
            || ifKey(binds.clueList, onClueListDown)
            || ifKey(binds.clueNotes, onClueNotesDown)
            || ifKey(binds.exit, onExitDown)
            || ifKey(binds.fitToScreen, onFitToScreenDown)
            || ifKey(binds.help, onHelpDown)
            || ifKey(binds.insertSpecial, onInsertSpecialDown)
            || ifKey(binds.nextClue, onNextClueDown)
            || ifKey(binds.openCrosswordSolver, onOpenCrosswordSolverDown)
            || ifKey(binds.openDictionary, onOpenDictionaryDown)
            || ifKey(binds.openInfo, onOpenInfoDown)
            || ifKey(binds.openShareUrl, onOpenShareUrlDown)
            || ifKey(binds.prevClue, onPrevClueDown)
            || ifKey(binds.puzzleNotes, onPuzzleNotesDown)
            || ifKey(binds.randomClue, onRandomClueDown)
            || ifKey(binds.revealErrors, onRevealErrorsDown)
            || ifKey(binds.revealLetter, onRevealLetterDown)
            || ifKey(binds.revealPuzzle, onRevealPuzzleDown)
            || ifKey(binds.revealWord, onRevealWordDown)
            || ifKey(binds.searchDuckduckgo, onSearchDuckduckgoDown)
            || ifKey(binds.searchFifteenSquared, onSearchFifteenSquaredDown)
            || ifKey(binds.settings, onSettingsDown)
            || ifKey(binds.shareClue, onShareClueDown)
            || ifKey(binds.shareClueWithBoard, onShareClueWithBoardDown)
            || ifKey(binds.sharePuzzle, onSharePuzzleDown)
            || ifKey(binds.sharePuzzleBlank, onSharePuzzleBlankDown)
            || ifKey(binds.sharePuzzleNoIpuz, onSharePuzzleNoIpuzDown)
            || ifKey(binds.sharePuzzleOriginal, onSharePuzzleOriginalDown)
            || ifKey(binds.shuffleAnagram, onShuffleAnagramDown)
            || ifKey(binds.toggleCellFlag, onToggleCellFlagDown)
            || ifKey(binds.toggleClueFlag, onToggleClueFlagDown)
            || ifKey(binds.toggleScratchMode, onToggleScratchModeDown)
            || ifKey(binds.toggleSelection, onToggleSelectionDown)
            || ifKey(
                binds.toggleShowErrorsCursor,
                onToggleShowErrorsCursorDown,
            )
            || ifKey(
                binds.toggleShowErrorsGrid,
                onToggleShowErrorsGridDown,
            )
            || ifKey(
                binds.toggleShowErrorsWordCells,
                onToggleShowErrorsWordCellsDown,
            )
            || ifKey(
                binds.toggleShowErrorsWordWord,
                onToggleShowErrorsWordWordDown,
            )
            || ifKey(binds.voiceInput, onVoiceInputDown)
            || ifKey(binds.zoomIn, onZoomInDown)
            || ifKey(binds.zoomInMax, onZoomInMaxDown)
            || ifKey(binds.zoomOut, onZoomOutDown)
            || ifKey(binds.zoomReset, onZoomResetDown)
        )
    } ?: false
}

/**
 * Translate Compose key event to Forkyz or null
 */
private fun getForkyzKey(event : KeyEvent) : ForkyzKey?  {
    return when (event.key) {
        Key.A -> ForkyzKeySymbol.A
        Key.Apostrophe -> ForkyzKeySymbol.APOSTROPHE
        Key.At -> ForkyzKeySymbol.AT
        Key.B -> ForkyzKeySymbol.B
        Key.Back -> ForkyzKeySymbol.BACK
        Key.Backslash -> ForkyzKeySymbol.BACKSLASH
        Key.Backspace -> ForkyzKeySymbol.BACKSPACE
        Key.Bookmark -> ForkyzKeySymbol.BOOKMARK
        Key.Break -> ForkyzKeySymbol.BREAK
        Key.C -> ForkyzKeySymbol.C
        Key.CapsLock -> ForkyzKeySymbol.CAPS_LOCK
        Key.Comma -> ForkyzKeySymbol.COMMA
        Key.D -> ForkyzKeySymbol.D
        Key.Delete -> ForkyzKeySymbol.DELETE
        Key.DirectionDown -> ForkyzKeySymbol.DOWN
        Key.DirectionLeft -> ForkyzKeySymbol.LEFT
        Key.DirectionRight -> ForkyzKeySymbol.RIGHT
        Key.DirectionUp -> ForkyzKeySymbol.UP
        Key.E -> ForkyzKeySymbol.E
        Key.Eight -> ForkyzKeySymbol.EIGHT
        Key.Enter -> ForkyzKeySymbol.ENTER
        Key.Equals -> ForkyzKeySymbol.EQUALS
        Key.Escape -> ForkyzKeySymbol.ESCAPE
        Key.F -> ForkyzKeySymbol.F
        Key.F1 -> ForkyzKeySymbol.F1
        Key.F10 -> ForkyzKeySymbol.F10
        Key.F11 -> ForkyzKeySymbol.F11
        Key.F12 -> ForkyzKeySymbol.F12
        Key.F2 -> ForkyzKeySymbol.F2
        Key.F3 -> ForkyzKeySymbol.F3
        Key.F4 -> ForkyzKeySymbol.F4
        Key.F5 -> ForkyzKeySymbol.F5
        Key.F6 -> ForkyzKeySymbol.F6
        Key.F7 -> ForkyzKeySymbol.F7
        Key.F8 -> ForkyzKeySymbol.F8
        Key.F9 -> ForkyzKeySymbol.F9
        Key.Five -> ForkyzKeySymbol.FIVE
        Key.Forward -> ForkyzKeySymbol.FORWARD
        Key.Four -> ForkyzKeySymbol.FOUR
        Key.G -> ForkyzKeySymbol.G
        Key.Grave -> ForkyzKeySymbol.GRAVE
        Key.H -> ForkyzKeySymbol.H
        Key.I -> ForkyzKeySymbol.I
        Key.Insert -> ForkyzKeySymbol.INSERT
        Key.J -> ForkyzKeySymbol.J
        Key.K -> ForkyzKeySymbol.K
        Key.L -> ForkyzKeySymbol.L
        Key.LeftBracket -> ForkyzKeySymbol.LEFT_BRACKET
        Key.M -> ForkyzKeySymbol.M
        Key.Menu -> ForkyzKeySymbol.MENU
        Key.Minus -> ForkyzKeySymbol.MINUS
        Key.MoveEnd -> ForkyzKeySymbol.END
        Key.MoveHome -> ForkyzKeySymbol.HOME
        Key.N -> ForkyzKeySymbol.N
        Key.Nine -> ForkyzKeySymbol.NINE
        Key.O -> ForkyzKeySymbol.O
        Key.One -> ForkyzKeySymbol.ONE
        Key.P -> ForkyzKeySymbol.P
        Key.PageDown -> ForkyzKeySymbol.PAGE_DOWN
        Key.PageUp -> ForkyzKeySymbol.PAGE_UP
        Key.Period -> ForkyzKeySymbol.PERIOD
        Key.Plus -> ForkyzKeySymbol.PLUS
        Key.Pound -> ForkyzKeySymbol.POUND
        Key.PrintScreen -> ForkyzKeySymbol.PRINT_SCREEN
        Key.Q -> ForkyzKeySymbol.Q
        Key.R -> ForkyzKeySymbol.R
        Key.RightBracket -> ForkyzKeySymbol.RIGHT_BRACKET
        Key.S -> ForkyzKeySymbol.S
        Key.Semicolon -> ForkyzKeySymbol.SEMI_COLON
        Key.Seven -> ForkyzKeySymbol.SEVEN
        Key.Six -> ForkyzKeySymbol.SIX
        Key.Spacebar -> ForkyzKeySymbol.SPACE
        Key.T -> ForkyzKeySymbol.T
        Key.Tab -> ForkyzKeySymbol.TAB
        Key.Three -> ForkyzKeySymbol.THREE
        Key.Two -> ForkyzKeySymbol.TWO
        Key.U -> ForkyzKeySymbol.U
        Key.V -> ForkyzKeySymbol.V
        Key.W -> ForkyzKeySymbol.W
        Key.X -> ForkyzKeySymbol.X
        Key.Y -> ForkyzKeySymbol.Y
        Key.Z -> ForkyzKeySymbol.Z
        Key.Zero -> ForkyzKeySymbol.ZERO
        else -> null
    }?.let { symbol ->
        ForkyzKey(
            symbol,
            event.isAltPressed,
            event.isCtrlPressed,
            event.isMetaPressed,
            event.isShiftPressed,
        )
    }
}

