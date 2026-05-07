
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hide_keyboard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_keyboard_hide
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.AzertyKeyboard
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.ColemakKeyboard
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.DvorakKeyboard
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.KeyboardLayoutScope
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.QwertyKeyboard
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.QwertzKeyboard
import app.crossword.yourealwaysbe.forkyz.view.keyboardlayout.RussianKeyboard

enum class OrdinaryKey {
    KEY_DELETE,
    KEY_DOWN,
    KEY_LEFT,
    KEY_RIGHT,
    KEY_SPACE,
    KEY_UP,
}

enum class SpecialKey {
    KEY_CHANGE_CLUE_DIRECTION,
    KEY_NEXT_CLUE,
    KEY_PREVIOUS_CLUE,
    KEY_SPECIAL_ENTRY,
}

/**
 * Software Keyboard
 *
 * @param modifier modifier
 * @param inputConnectionMediator connection to send regular key events on
 * @param onSpecialKeyUp when a special key (such as change dir) is
 * unpressed
 * @param onSpecialKeyDown when a special key (such as change dir) is
 * pressed down
 * @param onSpecialKeyUp when a special key (such as change dir) is
 * unpressed
 * @param onHideClick when hide button pressed
 * @param compact reduce vertical size of keyboard
 * @param hapticFeedback haptic feedback for key presses
 * @param showHideButton display the keyboard hide button on bottom row
 * @param showSpecialKeys show the Forkyz-specific non-standard keys
 * @param layout which layout to use
 * @param repeatDelay how long in millis before key repeat starts when
 * held down (0 means never)
 * @param repeatInterval how long between repeated keypresses when held
 * down (0 means never)
 */
@Composable
fun ForkyzKeyboard(
    modifier : Modifier = Modifier,
    inputConnectionMediator : InputConnectionMediator,
    onSpecialKeyDown : (SpecialKey) -> Unit = { },
    onSpecialKeyUp : (SpecialKey) -> Unit = { },
    onHideClick : () -> Unit = { },
    compact : Boolean = false,
    hapticFeedback : Boolean = false,
    showHideButton : Boolean = true,
    showSpecialKeys : Boolean = true,
    layout : KeyboardLayout = KeyboardLayout.KL_QWERTY,
    repeatDelay : Long = 300,
    repeatInterval : Long = 75,
) {
    @Composable
    fun HideButtonRow() {
        Row(Modifier.fillMaxWidth().background(Color.Black)) {
            IconButton(
                modifier = Modifier.focusProperties { canFocus = false },
                onClick = onHideClick,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_hide),
                    contentDescription = stringResource(Res.string.hide_keyboard),
                    tint = Color.White,
                )
            }
        }
    }

    val scope = KeyboardLayoutScope(
        modifier = modifier,
        compact = compact,
        hapticFeedback = hapticFeedback,
        showSpecialKeys = showSpecialKeys,
        inputConnectionMediator = inputConnectionMediator,
        onSpecialKeyDown = onSpecialKeyDown,
        onSpecialKeyUp = onSpecialKeyUp,
        repeatDelay = repeatDelay,
        repeatInterval = repeatInterval,
    )

    HorizontalDivider()
    when (layout) {
        KeyboardLayout.KL_QWERTY -> scope.QwertyKeyboard()
        KeyboardLayout.KL_QWERTZ -> scope.QwertzKeyboard()
        KeyboardLayout.KL_AZERTY -> scope.AzertyKeyboard()
        KeyboardLayout.KL_COLEMAK -> scope.ColemakKeyboard()
        KeyboardLayout.KL_DVORAK -> scope.DvorakKeyboard()
        KeyboardLayout.KL_RUSSIAN -> scope.RussianKeyboard()
    }
    if (showHideButton)
        HideButtonRow()
}
