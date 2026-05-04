
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hide_keyboard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_keyboard_hide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_special_entry
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.special_entry
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator

// percent of display height
private val BUTTON_PADDING_FRAC = 0.003F
private val BUTTON_PADDING_FRAC_COMPACT = 0.0F
// used when repeat delay set to 0 (disabled)
private val DEFAULT_LONG_PRESS_DELAY = 300L // ms

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

    @Composable
    fun Modifier.buttonVerticalPadding() : Modifier {
        val screenHeight = LocalWindowInfo.current.containerSize.height.dp
        val paddingFrac = if (compact)
            BUTTON_PADDING_FRAC_COMPACT
        else
            BUTTON_PADDING_FRAC
        return this.padding(vertical = screenHeight * paddingFrac)
    }

    /**
     * A single key
     *
     * @param onDown callback on key down
     * @param onUp callback on key up
     * @param onLongPress called when long press (determined by repeat
     * delay or default if repeat disabled). If set to non-null, will
     * disable key repeat and onUp callback.
     * @param content the actual button, takes a modifier. The passed
     * modifier handles presses (set your content's onClick to no-op)
     */
    @Composable
    fun BaseKey(
        modifier : Modifier = Modifier,
        onDown : () -> Unit,
        onUp : () -> Unit,
        onLongPress : (() -> Unit)? = null,
        content : @Composable (Modifier) -> Unit,
    ) {
        // key repeat with thanks to
        // https://proandroiddev.com/creating-a-repeating-button-with-jetpack-compose-b39c4f559f7d

        val currentOnDown by rememberUpdatedState(onDown)
        val currentOnUp by rememberUpdatedState(onUp)
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        content(
            modifier.focusProperties { canFocus = false }.pointerInput(true) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                        .also { it.consume() }

                    currentOnDown()
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.VirtualKey,
                        )
                    }

                    var wasLongPress = false

                    val heldButtonJob = scope.launch {
                        if (onLongPress != null) {
                            val longDelay = if (repeatDelay > 0) {
                                repeatDelay
                            } else {
                                DEFAULT_LONG_PRESS_DELAY
                            }
                            delay(longDelay)
                            wasLongPress = true
                            onLongPress()
                        } else if (repeatDelay > 0 && repeatInterval > 0) {
                            delay(repeatDelay)
                            while (down.pressed) {
                                delay(repeatInterval)
                                currentOnUp()
                                currentOnDown()
                            }
                        }
                    }

                    waitForUpOrCancellation()?.also { it.consume() }
                    heldButtonJob.cancel()
                    if (!wasLongPress)
                        currentOnUp()
                    inputConnectionMediator.focusReceiver()
                }
            }.buttonVerticalPadding()
        )
    }

    /**
     * A single text key
     *
     * @param value the string value sent/displayed on key
     */
    @Composable
    fun StringKey(
        modifier : Modifier = Modifier,
        value : String,
    ) {
        BaseKey(
            modifier,
            onDown = { inputConnectionMediator.sendStringValueKeyDown(value) },
            onUp = { inputConnectionMediator.sendStringValueKeyUp(value) },
        ) { keyModifier ->
            TextButton(
                modifier = keyModifier,
                onClick = { /* handled by BaseKey */ },
                colors = ButtonDefaults.textButtonColors().copy(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }

    /**
     * Special key for change direction or special entry on long press
     */
    @Composable
    fun ChangeDirectionSpecialEntryKey(modifier : Modifier = Modifier) {
        if (showSpecialKeys) {
            val changeDirLabel = stringResource(Res.string.change_direction)
            val longPressLabel = stringResource(Res.string.special_entry)
            BaseKey(
                modifier = Modifier.semantics {
                    onLongClick(
                        // handled by pointer input
                        action = null,
                        label = longPressLabel,
                    )
                    contentDescription = changeDirLabel
                },
                onDown = { /* do nothing, wait for long or release */ },
                onUp = {
                    onSpecialKeyDown(SpecialKey.KEY_CHANGE_CLUE_DIRECTION)
                    onSpecialKeyUp(SpecialKey.KEY_CHANGE_CLUE_DIRECTION)
                },
                onLongPress = {
                    onSpecialKeyDown(SpecialKey.KEY_SPECIAL_ENTRY)
                    onSpecialKeyUp(SpecialKey.KEY_SPECIAL_ENTRY)
                },
            ) { keyModifier ->
                IconButton(
                    modifier = keyModifier,
                    onClick = { /* handled above */ },
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Icon(
                            modifier = Modifier.buttonVerticalPadding(),
                            painter = painterResource(
                                Res.drawable.ic_change_direction,
                            ),
                            contentDescription = changeDirLabel,
                        )
                        Text(
                            text = stringResource(Res.string.key_special_entry),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        } else {
            Box(modifier = modifier)
        }
    }

    /**
     * A single icon key
     *
     * @param symbol id of drawable to display on key
     * @param contentDescription id of icon description
     * @param onDown callback on key down
     * @param onUp callback on key up
     */
    @Composable
    fun BaseIconKey(
        modifier : Modifier = Modifier,
        symbol : DrawableResource,
        contentDescription : StringResource,
        onDown : () -> Unit,
        onUp : () -> Unit,
    ) {
        BaseKey(
            modifier,
            onDown = onDown,
            onUp = onUp,
        ) { keyModifier ->
            IconButton(
                modifier = keyModifier,
                onClick = { /* handled by ForkyzKey */ },
            ) {
                Icon(
                    painter = painterResource(symbol),
                    contentDescription = stringResource(contentDescription),
                )
            }
        }
    }

    /**
     * A single icon key with standard key code for input connection
     *
     * @param key which key this is
     * @param symbol id of drawable to display on key
     * @param contentDescription id of icon description
     */
    @Composable
    fun OrdinaryIconKey(
        modifier : Modifier = Modifier,
        key : OrdinaryKey,
        symbol : DrawableResource,
        contentDescription : StringResource,
    ) {
        BaseIconKey(
            modifier = modifier,
            symbol = symbol,
            contentDescription = contentDescription,
            onDown = { inputConnectionMediator.sendOrdinaryKeyDown(key) },
            onUp = { inputConnectionMediator.sendOrdinaryKeyUp(key) },
        )
    }

    /**
     * A single icon key with special key
     *
     * @param key the special key this represents
     * @param symbol id of drawable to display on key
     * @param contentDescription id of icon description
     */
    @Composable
    fun SpecialIconKey(
        modifier : Modifier = Modifier,
        key : SpecialKey,
        symbol : DrawableResource,
        contentDescription : StringResource,
    ) {
        if (showSpecialKeys) {
            BaseIconKey(
                modifier = modifier,
                symbol = symbol,
                contentDescription = contentDescription,
                onDown = { onSpecialKeyDown(key) },
                onUp = { onSpecialKeyUp(key) },
            )
        } else {
            Box(modifier = modifier)
        }
    }

    @Composable
    fun SpaceRow() {
        Row() {
            val mAction = Modifier.weight(1.25F)
            val mSpace = Modifier.weight(2.5F)
            OrdinaryIconKey(
                mAction,
                OrdinaryKey.KEY_UP,
                Res.drawable.arrow_up,
                Res.string.key_up,
            )
            OrdinaryIconKey(
                mAction,
                OrdinaryKey.KEY_DOWN,
                Res.drawable.arrow_down,
                Res.string.key_down,
            )
            SpecialIconKey(
                mAction,
                SpecialKey.KEY_PREVIOUS_CLUE,
                Res.drawable.ic_prev_clue,
                Res.string.prev_clue,
            )
            OrdinaryIconKey(
                mSpace,
                OrdinaryKey.KEY_SPACE,
                Res.drawable.space,
                Res.string.key_space,
            )
            SpecialIconKey(
                mAction,
                SpecialKey.KEY_NEXT_CLUE,
                Res.drawable.ic_next_clue,
                Res.string.next_clue,
            )
            OrdinaryIconKey(
                mAction,
                OrdinaryKey.KEY_LEFT,
                Res.drawable.arrow_left,
                Res.string.key_left,
            )
            OrdinaryIconKey(
                mAction,
                OrdinaryKey.KEY_RIGHT,
                Res.drawable.arrow_right,
                Res.string.key_right,
            )
        }
    }

    @Composable
    fun QwertyKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "Q")
                StringKey(m1, "W")
                StringKey(m1, "E")
                StringKey(m1, "R")
                StringKey(m1, "T")
                StringKey(m1, "Y")
                StringKey(m1, "U")
                StringKey(m1, "I")
                StringKey(m1, "O")
                StringKey(m1, "P")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                StringKey(m2, "A")
                StringKey(m2, "S")
                StringKey(m2, "D")
                StringKey(m2, "F")
                StringKey(m2, "G")
                StringKey(m2, "H")
                StringKey(m2, "J")
                StringKey(m2, "K")
                StringKey(m2, "L")
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                ChangeDirectionSpecialEntryKey(m2)
                Box(m1)
                StringKey(m2, "Z")
                StringKey(m2, "X")
                StringKey(m2, "C")
                StringKey(m2, "V")
                StringKey(m2, "B")
                StringKey(m2, "N")
                StringKey(m2, "M")
                OrdinaryIconKey(
                    m3,
                    OrdinaryKey.KEY_DELETE,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            SpaceRow()
        }
    }

    @Composable
    fun QwertzKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "Q")
                StringKey(m1, "W")
                StringKey(m1, "E")
                StringKey(m1, "R")
                StringKey(m1, "T")
                StringKey(m1, "Z")
                StringKey(m1, "U")
                StringKey(m1, "I")
                StringKey(m1, "O")
                StringKey(m1, "P")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                StringKey(m2, "A")
                StringKey(m2, "S")
                StringKey(m2, "D")
                StringKey(m2, "F")
                StringKey(m2, "G")
                StringKey(m2, "H")
                StringKey(m2, "J")
                StringKey(m2, "K")
                StringKey(m2, "L")
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                ChangeDirectionSpecialEntryKey(m2)
                Box(m1)
                StringKey(m2, "Y")
                StringKey(m2, "X")
                StringKey(m2, "C")
                StringKey(m2, "V")
                StringKey(m2, "B")
                StringKey(m2, "N")
                StringKey(m2, "M")
                OrdinaryIconKey(
                    m3,
                    OrdinaryKey.KEY_DELETE,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            SpaceRow()
        }
    }

    @Composable
    fun AzertyKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "A")
                StringKey(m1, "Z")
                StringKey(m1, "E")
                StringKey(m1, "R")
                StringKey(m1, "T")
                StringKey(m1, "Y")
                StringKey(m1, "U")
                StringKey(m1, "I")
                StringKey(m1, "O")
                StringKey(m1, "P")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "Q")
                StringKey(m1, "S")
                StringKey(m1, "D")
                StringKey(m1, "F")
                StringKey(m1, "G")
                StringKey(m1, "H")
                StringKey(m1, "J")
                StringKey(m1, "K")
                StringKey(m1, "L")
                StringKey(m1, "M")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(1.5F)
                ChangeDirectionSpecialEntryKey(m2)
                StringKey(m1, "W")
                StringKey(m1, "X")
                StringKey(m1, "C")
                StringKey(m1, "V")
                StringKey(m1, "B")
                StringKey(m1, "N")
                Box(m1)
                OrdinaryIconKey(
                    m2,
                    OrdinaryKey.KEY_DELETE,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            SpaceRow()
        }
    }

    @Composable
    fun ColemakKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "Q")
                StringKey(m1, "W")
                StringKey(m1, "F")
                StringKey(m1, "P")
                StringKey(m1, "G")
                StringKey(m1, "J")
                StringKey(m1, "L")
                StringKey(m1, "U")
                StringKey(m1, "Y")
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "A")
                StringKey(m1, "R")
                StringKey(m1, "S")
                StringKey(m1, "T")
                StringKey(m1, "D")
                StringKey(m1, "H")
                StringKey(m1, "N")
                StringKey(m1, "E")
                StringKey(m1, "I")
                StringKey(m1, "O")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                ChangeDirectionSpecialEntryKey(m2)
                Box(m1)
                StringKey(m2, "Z")
                StringKey(m2, "X")
                StringKey(m2, "C")
                StringKey(m2, "V")
                StringKey(m2, "B")
                StringKey(m2, "K")
                StringKey(m2, "M")
                OrdinaryIconKey(
                    m3,
                    OrdinaryKey.KEY_DELETE,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            SpaceRow()
        }
    }

    @Composable
    fun DvorakKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                OrdinaryIconKey(
                    m3,
                    OrdinaryKey.KEY_DELETE,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
                ChangeDirectionSpecialEntryKey(m2)
                StringKey(m2, "P")
                StringKey(m2, "Y")
                StringKey(m2, "F")
                StringKey(m2, "G")
                StringKey(m2, "C")
                StringKey(m2, "R")
                StringKey(m2, "L")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                StringKey(m1, "A")
                StringKey(m1, "O")
                StringKey(m1, "E")
                StringKey(m1, "U")
                StringKey(m1, "I")
                StringKey(m1, "D")
                StringKey(m1, "H")
                StringKey(m1, "T")
                StringKey(m1, "N")
                StringKey(m1, "S")
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                Box(m1)
                StringKey(m1, "Q")
                StringKey(m1, "J")
                StringKey(m1, "K")
                StringKey(m1, "X")
                StringKey(m1, "B")
                StringKey(m1, "M")
                StringKey(m1, "W")
                StringKey(m1, "V")
                StringKey(m1, "Z")
            }
            SpaceRow()
        }
    }

    HorizontalDivider()
    when (layout) {
        KeyboardLayout.KL_QWERTY -> QwertyKeyboard()
        KeyboardLayout.KL_QWERTZ -> QwertzKeyboard()
        KeyboardLayout.KL_AZERTY -> AzertyKeyboard()
        KeyboardLayout.KL_COLEMAK -> ColemakKeyboard()
        KeyboardLayout.KL_DVORAK -> DvorakKeyboard()
    }
    if (showHideButton)
        HideButtonRow()
}


