
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.arrow_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hide_keyboard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_keyboard_hide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_a
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_a
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_a
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_a
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_a
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_b
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_b
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_b
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_b
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_b
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_c
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_c
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_c
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_c
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_c
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_d
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_d
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_d
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_d
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_d
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_e
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_e
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_e
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_e
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_e
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_f
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_f
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_f
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_f
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_f
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_g
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_g
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_g
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_g
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_g
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_h
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_h
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_h
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_h
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_h
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_i
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_i
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_i
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_i
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_i
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_j
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_j
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_j
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_j
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_j
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_k
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_k
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_k
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_k
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_k
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_l
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_l
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_l
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_l
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_l
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_m
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_m
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_m
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_m
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_m
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_n
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_n
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_n
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_n
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_n
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_o
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_o
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_o
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_o
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_o
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_p
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_p
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_p
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_p
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_p
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_q
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_q
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_q
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_q
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_q
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_r
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_r
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_r
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_r
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_r
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_s
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_s
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_s
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_s
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_s
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_special_entry
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_t
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_t
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_t
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_t
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_t
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_u
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_u
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_u
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_u
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_u
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_up
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_v
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_v
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_v
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_v
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_v
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_w
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_w
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_w
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_w
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_w
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_x
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_x
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_x
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_x
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_x
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_y
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_y
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_y
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_y
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_y
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_z
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_z
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_z
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_z
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_z
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator

// percent of display height
private val BUTTON_PADDING_FRAC = 0.003F
private val BUTTON_PADDING_FRAC_COMPACT = 0.0F

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
    fun inputConnectionKey(keyCode : Int, action : Int) {
        inputConnectionMediator.sendForkyzKey(keyCode, action)
    }

    val onKeyDown = { code : Int ->
        inputConnectionKey(code, inputConnectionMediator.actionDown)
    }
    val onKeyUp = { code : Int ->
        inputConnectionKey(code, inputConnectionMediator.actionUp)
    }

    @Composable
    fun HideButtonRow() {
        Row(Modifier.fillMaxWidth().background(Color.Black)) {
            IconButton(onClick = onHideClick) {
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
     * @param content the actual button, takes a modifier. The passed
     * modifier handles presses (set your content's onClick to no-op)
     */
    @Composable
    fun BaseKey(
        modifier : Modifier = Modifier,
        onDown : () -> Unit,
        onUp : () -> Unit,
        content : @Composable (Modifier) -> Unit,
    ) {
        // key repeat with thanks to
        // https://proandroiddev.com/creating-a-repeating-button-with-jetpack-compose-b39c4f559f7d

        val currentOnDown by rememberUpdatedState(onDown)
        val currentOnUp by rememberUpdatedState(onUp)
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        content(
            modifier.pointerInput(true) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                        .also { it.consume() }

                    currentOnDown()
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.VirtualKey,
                        )
                    }

                    val heldButtonJob = scope.launch {
                        if (repeatDelay > 0 && repeatInterval > 0) {
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
                    currentOnUp()
                }
            }.buttonVerticalPadding()
        )
    }

    /**
     * A single text key
     *
     * @param keyCode standard key code of key for input connection
     * @param symbol id of string to display on key
     * @param onDown callback on key down
     * @param onUp callback on key up
     */
    @Composable
    fun TextKey(
        modifier : Modifier = Modifier,
        keyCode : Int,
        symbol : StringResource,
    ) {
        BaseKey(
            modifier,
            onDown = { onKeyDown(keyCode) },
            onUp = { onKeyUp(keyCode) },
        ) { keyModifier ->
            TextButton(
                modifier = keyModifier,
                onClick = { /* handled by BaseKey */ },
                colors = ButtonDefaults.textButtonColors().copy(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = stringResource(symbol),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }

    /**
     * For sending a special entry key event
     *
     * @param key the special key this represents
     * @param symbol id of drawable to display on key
     * @param contentDescription id of icon description
     */
    @Composable
    fun SpecialEntryKey(modifier : Modifier = Modifier) {
        TextButton(
            modifier = modifier.buttonVerticalPadding(),
            onClick = {
                onSpecialKeyDown(SpecialKey.KEY_SPECIAL_ENTRY)
                onSpecialKeyUp(SpecialKey.KEY_SPECIAL_ENTRY)
            },
            colors = ButtonDefaults.textButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(
                text = stringResource(Res.string.key_special_entry),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
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
     * @param keyCode standard key code of key for input connection
     * @param symbol id of drawable to display on key
     * @param contentDescription id of icon description
     */
    @Composable
    fun IconKey(
        modifier : Modifier = Modifier,
        keyCode : Int,
        symbol : DrawableResource,
        contentDescription : StringResource,
    ) {
        BaseIconKey(
            modifier = modifier,
            symbol = symbol,
            contentDescription = contentDescription,
            onDown = { onKeyDown(keyCode) },
            onUp = { onKeyUp(keyCode) },
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
    fun QwertyKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyQ, Res.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, Res.string.key_w)
                TextKey(m1, inputConnectionMediator.keyE, Res.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, Res.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, Res.string.key_t)
                TextKey(m1, inputConnectionMediator.keyY, Res.string.key_y)
                TextKey(m1, inputConnectionMediator.keyU, Res.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, Res.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, Res.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, Res.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyA, Res.string.key_a)
                TextKey(m2, inputConnectionMediator.keyS, Res.string.key_s)
                TextKey(m2, inputConnectionMediator.keyD, Res.string.key_d)
                TextKey(m2, inputConnectionMediator.keyF, Res.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, Res.string.key_g)
                TextKey(m2, inputConnectionMediator.keyH, Res.string.key_h)
                TextKey(m2, inputConnectionMediator.keyJ, Res.string.key_j)
                TextKey(m2, inputConnectionMediator.keyK, Res.string.key_k)
                TextKey(m2, inputConnectionMediator.keyL, Res.string.key_l)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    Res.drawable.ic_change_direction,
                    Res.string.change_direction,
                )
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyZ, Res.string.key_z)
                TextKey(m2, inputConnectionMediator.keyX, Res.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, Res.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, Res.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, Res.string.key_b)
                TextKey(m2, inputConnectionMediator.keyN, Res.string.key_n)
                TextKey(m2, inputConnectionMediator.keyM, Res.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    Res.drawable.arrow_up,
                    Res.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    Res.drawable.arrow_down,
                    Res.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    Res.drawable.ic_prev_clue,
                    Res.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    Res.drawable.space,
                    Res.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    Res.drawable.ic_next_clue,
                    Res.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    Res.drawable.arrow_left,
                    Res.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    Res.drawable.arrow_right,
                    Res.string.key_right,
                )
            }
        }
    }

    @Composable
    fun QwertzKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyQ, Res.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, Res.string.key_w)
                TextKey(m1, inputConnectionMediator.keyE, Res.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, Res.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, Res.string.key_t)
                TextKey(m1, inputConnectionMediator.keyZ, Res.string.key_z)
                TextKey(m1, inputConnectionMediator.keyU, Res.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, Res.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, Res.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, Res.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyA, Res.string.key_a)
                TextKey(m2, inputConnectionMediator.keyS, Res.string.key_s)
                TextKey(m2, inputConnectionMediator.keyD, Res.string.key_d)
                TextKey(m2, inputConnectionMediator.keyF, Res.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, Res.string.key_g)
                TextKey(m2, inputConnectionMediator.keyH, Res.string.key_h)
                TextKey(m2, inputConnectionMediator.keyJ, Res.string.key_j)
                TextKey(m2, inputConnectionMediator.keyK, Res.string.key_k)
                TextKey(m2, inputConnectionMediator.keyL, Res.string.key_l)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    Res.drawable.ic_change_direction,
                    Res.string.change_direction,
                )
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyY, Res.string.key_y)
                TextKey(m2, inputConnectionMediator.keyX, Res.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, Res.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, Res.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, Res.string.key_b)
                TextKey(m2, inputConnectionMediator.keyN, Res.string.key_n)
                TextKey(m2, inputConnectionMediator.keyM, Res.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    Res.drawable.arrow_up,
                    Res.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    Res.drawable.arrow_down,
                    Res.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    Res.drawable.ic_prev_clue,
                    Res.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    Res.drawable.space,
                    Res.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    Res.drawable.ic_next_clue,
                    Res.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    Res.drawable.arrow_left,
                    Res.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    Res.drawable.arrow_right,
                    Res.string.key_right,
                )
            }
        }
    }

    @Composable
    fun AzertyKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, Res.string.key_a)
                TextKey(m1, inputConnectionMediator.keyZ, Res.string.key_z)
                TextKey(m1, inputConnectionMediator.keyE, Res.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, Res.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, Res.string.key_t)
                TextKey(m1, inputConnectionMediator.keyY, Res.string.key_y)
                TextKey(m1, inputConnectionMediator.keyU, Res.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, Res.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, Res.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, Res.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyQ, Res.string.key_q)
                TextKey(m1, inputConnectionMediator.keyS, Res.string.key_s)
                TextKey(m1, inputConnectionMediator.keyD, Res.string.key_d)
                TextKey(m1, inputConnectionMediator.keyF, Res.string.key_f)
                TextKey(m1, inputConnectionMediator.keyG, Res.string.key_g)
                TextKey(m1, inputConnectionMediator.keyH, Res.string.key_h)
                TextKey(m1, inputConnectionMediator.keyJ, Res.string.key_j)
                TextKey(m1, inputConnectionMediator.keyK, Res.string.key_k)
                TextKey(m1, inputConnectionMediator.keyL, Res.string.key_l)
                TextKey(m1, inputConnectionMediator.keyM, Res.string.key_m)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(1.5F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    Res.drawable.ic_change_direction,
                    Res.string.change_direction,
                )
                TextKey(m1, inputConnectionMediator.keyW, Res.string.key_w)
                TextKey(m1, inputConnectionMediator.keyX, Res.string.key_x)
                TextKey(m1, inputConnectionMediator.keyC, Res.string.key_c)
                TextKey(m1, inputConnectionMediator.keyV, Res.string.key_v)
                TextKey(m1, inputConnectionMediator.keyB, Res.string.key_b)
                TextKey(m1, inputConnectionMediator.keyN, Res.string.key_n)
                Box(m1)
                IconKey(
                    m2,
                    inputConnectionMediator.keyDelete,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    Res.drawable.arrow_up,
                    Res.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    Res.drawable.arrow_down,
                    Res.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    Res.drawable.ic_prev_clue,
                    Res.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    Res.drawable.space,
                    Res.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    Res.drawable.ic_next_clue,
                    Res.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    Res.drawable.arrow_left,
                    Res.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    Res.drawable.arrow_right,
                    Res.string.key_right,
                )
            }
        }
    }

    @Composable
    fun ColemakKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyQ, Res.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, Res.string.key_w)
                TextKey(m1, inputConnectionMediator.keyF, Res.string.key_f)
                TextKey(m1, inputConnectionMediator.keyP, Res.string.key_p)
                TextKey(m1, inputConnectionMediator.keyG, Res.string.key_g)
                TextKey(m1, inputConnectionMediator.keyJ, Res.string.key_j)
                TextKey(m1, inputConnectionMediator.keyL, Res.string.key_l)
                TextKey(m1, inputConnectionMediator.keyU, Res.string.key_u)
                TextKey(m1, inputConnectionMediator.keyY, Res.string.key_y)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, Res.string.key_a)
                TextKey(m1, inputConnectionMediator.keyR, Res.string.key_r)
                TextKey(m1, inputConnectionMediator.keyS, Res.string.key_s)
                TextKey(m1, inputConnectionMediator.keyT, Res.string.key_t)
                TextKey(m1, inputConnectionMediator.keyD, Res.string.key_d)
                TextKey(m1, inputConnectionMediator.keyH, Res.string.key_h)
                TextKey(m1, inputConnectionMediator.keyN, Res.string.key_n)
                TextKey(m1, inputConnectionMediator.keyE, Res.string.key_e)
                TextKey(m1, inputConnectionMediator.keyI, Res.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, Res.string.key_o)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    Res.drawable.ic_change_direction,
                    Res.string.change_direction,
                )
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyZ, Res.string.key_z)
                TextKey(m2, inputConnectionMediator.keyX, Res.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, Res.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, Res.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, Res.string.key_b)
                TextKey(m2, inputConnectionMediator.keyK, Res.string.key_k)
                TextKey(m2, inputConnectionMediator.keyM, Res.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    Res.drawable.arrow_up,
                    Res.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    Res.drawable.arrow_down,
                    Res.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    Res.drawable.ic_prev_clue,
                    Res.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    Res.drawable.space,
                    Res.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    Res.drawable.ic_next_clue,
                    Res.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    Res.drawable.arrow_left,
                    Res.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    Res.drawable.arrow_right,
                    Res.string.key_right,
                )
            }
        }
    }

    @Composable
    fun DvorakKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    Res.drawable.delete,
                    Res.string.key_delete,
                )
                SpecialIconKey(
                    m3,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    Res.drawable.ic_change_direction,
                    Res.string.change_direction,
                )
                TextKey(m2, inputConnectionMediator.keyP, Res.string.key_p)
                TextKey(m2, inputConnectionMediator.keyY, Res.string.key_y)
                TextKey(m2, inputConnectionMediator.keyF, Res.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, Res.string.key_g)
                TextKey(m2, inputConnectionMediator.keyC, Res.string.key_c)
                TextKey(m2, inputConnectionMediator.keyR, Res.string.key_r)
                TextKey(m2, inputConnectionMediator.keyL, Res.string.key_l)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, Res.string.key_a)
                TextKey(m1, inputConnectionMediator.keyO, Res.string.key_o)
                TextKey(m1, inputConnectionMediator.keyE, Res.string.key_e)
                TextKey(m1, inputConnectionMediator.keyU, Res.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, Res.string.key_i)
                TextKey(m1, inputConnectionMediator.keyD, Res.string.key_d)
                TextKey(m1, inputConnectionMediator.keyH, Res.string.key_h)
                TextKey(m1, inputConnectionMediator.keyT, Res.string.key_t)
                TextKey(m1, inputConnectionMediator.keyN, Res.string.key_n)
                TextKey(m1, inputConnectionMediator.keyS, Res.string.key_s)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                Box(m1)
                TextKey(m1, inputConnectionMediator.keyQ, Res.string.key_q)
                TextKey(m1, inputConnectionMediator.keyJ, Res.string.key_j)
                TextKey(m1, inputConnectionMediator.keyK, Res.string.key_k)
                TextKey(m1, inputConnectionMediator.keyX, Res.string.key_x)
                TextKey(m1, inputConnectionMediator.keyB, Res.string.key_b)
                TextKey(m1, inputConnectionMediator.keyM, Res.string.key_m)
                TextKey(m1, inputConnectionMediator.keyW, Res.string.key_w)
                TextKey(m1, inputConnectionMediator.keyV, Res.string.key_v)
                TextKey(m1, inputConnectionMediator.keyZ, Res.string.key_z)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    Res.drawable.arrow_up,
                    Res.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    Res.drawable.arrow_down,
                    Res.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    Res.drawable.ic_prev_clue,
                    Res.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    Res.drawable.space,
                    Res.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    Res.drawable.ic_next_clue,
                    Res.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    Res.drawable.arrow_left,
                    Res.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    Res.drawable.arrow_right,
                    Res.string.key_right,
                )
            }
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


