
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout

// percent of display height
private val BUTTON_PADDING_FRAC = 0.003F
private val BUTTON_PADDING_FRAC_COMPACT = 0.0F

enum class SpecialKey {
    KEY_CHANGE_CLUE_DIRECTION,
    KEY_NEXT_CLUE,
    KEY_PREVIOUS_CLUE
}

fun getForkyzKeyboardEditorInfo() : EditorInfo {
    return EditorInfo()
}

/**
 * Software Keyboard
 *
 * @param modifier modifier
 * @param inputConnection connection to send regular key events on
 * @param onSpecialKeyDown when a special key (such as change dir) is
 * pressed down
 * @param onSpecialKeyDown when a special key (such as change dir) is
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
    inputConnection : InputConnection? = null,
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
        val event = KeyEvent(action, keyCode)
        val flagged = KeyEvent.changeFlags(
            event,
            KeyEvent.FLAG_KEEP_TOUCH_MODE or KeyEvent.FLAG_SOFT_KEYBOARD,
        )
        inputConnection?.sendKeyEvent(flagged)
    }

    val onKeyDown = { code : Int ->
        inputConnectionKey(code, KeyEvent.ACTION_DOWN)
    }
    val onKeyUp = { code : Int ->
        inputConnectionKey(code, KeyEvent.ACTION_UP)
    }

    @Composable
    fun HideButtonRow() {
        Row(Modifier.fillMaxWidth().background(Color.Black)) {
            IconButton(onClick = onHideClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_hide),
                    contentDescription = stringResource(R.string.hide_keyboard),
                    tint = Color.White,
                )
            }
        }
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

        val screenHeight = LocalWindowInfo.current.containerSize.height.dp
        val paddingFrac = if (compact)
            BUTTON_PADDING_FRAC_COMPACT
        else
            BUTTON_PADDING_FRAC
        val verticalPadding = screenHeight * paddingFrac

        content(
            modifier.pointerInput(true) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val heldButtonJob = scope.launch {
                        currentOnDown()
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.VirtualKey,
                            )
                        }

                        if (repeatDelay > 0 && repeatInterval > 0) {
                            delay(repeatDelay)
                            while (down.pressed) {
                                delay(repeatInterval)
                                currentOnUp()
                                currentOnDown()
                            }
                        }
                    }

                    waitForUpOrCancellation()
                    heldButtonJob.cancel()
                    currentOnUp()
                }
            }.padding(vertical = verticalPadding)
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
        symbol : Int,
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
        symbol : Int,
        contentDescription : Int,
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
        symbol : Int,
        contentDescription : Int,
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
        symbol : Int,
        contentDescription : Int,
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
                TextKey(m1, KeyEvent.KEYCODE_Q, R.string.key_q)
                TextKey(m1, KeyEvent.KEYCODE_W, R.string.key_w)
                TextKey(m1, KeyEvent.KEYCODE_E, R.string.key_e)
                TextKey(m1, KeyEvent.KEYCODE_R, R.string.key_r)
                TextKey(m1, KeyEvent.KEYCODE_T, R.string.key_t)
                TextKey(m1, KeyEvent.KEYCODE_Y, R.string.key_y)
                TextKey(m1, KeyEvent.KEYCODE_U, R.string.key_u)
                TextKey(m1, KeyEvent.KEYCODE_I, R.string.key_i)
                TextKey(m1, KeyEvent.KEYCODE_O, R.string.key_o)
                TextKey(m1, KeyEvent.KEYCODE_P, R.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, KeyEvent.KEYCODE_A, R.string.key_a)
                TextKey(m2, KeyEvent.KEYCODE_S, R.string.key_s)
                TextKey(m2, KeyEvent.KEYCODE_D, R.string.key_d)
                TextKey(m2, KeyEvent.KEYCODE_F, R.string.key_f)
                TextKey(m2, KeyEvent.KEYCODE_G, R.string.key_g)
                TextKey(m2, KeyEvent.KEYCODE_H, R.string.key_h)
                TextKey(m2, KeyEvent.KEYCODE_J, R.string.key_j)
                TextKey(m2, KeyEvent.KEYCODE_K, R.string.key_k)
                TextKey(m2, KeyEvent.KEYCODE_L, R.string.key_l)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                Box(m1)
                TextKey(m2, KeyEvent.KEYCODE_Z, R.string.key_z)
                TextKey(m2, KeyEvent.KEYCODE_X, R.string.key_x)
                TextKey(m2, KeyEvent.KEYCODE_C, R.string.key_c)
                TextKey(m2, KeyEvent.KEYCODE_V, R.string.key_v)
                TextKey(m2, KeyEvent.KEYCODE_B, R.string.key_b)
                TextKey(m2, KeyEvent.KEYCODE_N, R.string.key_n)
                TextKey(m2, KeyEvent.KEYCODE_M, R.string.key_m)
                IconKey(
                    m3,
                    KeyEvent.KEYCODE_DEL,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m6 = Modifier.weight(2.0F)
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_UP,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                Box(m1)
                IconKey(
                    m6,
                    KeyEvent.KEYCODE_SPACE,
                    R.drawable.space,
                    R.string.key_space,
                )
                Box(m1)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    R.drawable.arrow_right,
                    R.string.key_right,
                )
            }
        }
    }

    @Composable
    fun QwertzKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, KeyEvent.KEYCODE_Q, R.string.key_q)
                TextKey(m1, KeyEvent.KEYCODE_W, R.string.key_w)
                TextKey(m1, KeyEvent.KEYCODE_E, R.string.key_e)
                TextKey(m1, KeyEvent.KEYCODE_R, R.string.key_r)
                TextKey(m1, KeyEvent.KEYCODE_T, R.string.key_t)
                TextKey(m1, KeyEvent.KEYCODE_Z, R.string.key_z)
                TextKey(m1, KeyEvent.KEYCODE_U, R.string.key_u)
                TextKey(m1, KeyEvent.KEYCODE_I, R.string.key_i)
                TextKey(m1, KeyEvent.KEYCODE_O, R.string.key_o)
                TextKey(m1, KeyEvent.KEYCODE_P, R.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, KeyEvent.KEYCODE_A, R.string.key_a)
                TextKey(m2, KeyEvent.KEYCODE_S, R.string.key_s)
                TextKey(m2, KeyEvent.KEYCODE_D, R.string.key_d)
                TextKey(m2, KeyEvent.KEYCODE_F, R.string.key_f)
                TextKey(m2, KeyEvent.KEYCODE_G, R.string.key_g)
                TextKey(m2, KeyEvent.KEYCODE_H, R.string.key_h)
                TextKey(m2, KeyEvent.KEYCODE_J, R.string.key_j)
                TextKey(m2, KeyEvent.KEYCODE_K, R.string.key_k)
                TextKey(m2, KeyEvent.KEYCODE_L, R.string.key_l)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                Box(m1)
                TextKey(m2, KeyEvent.KEYCODE_Y, R.string.key_y)
                TextKey(m2, KeyEvent.KEYCODE_X, R.string.key_x)
                TextKey(m2, KeyEvent.KEYCODE_C, R.string.key_c)
                TextKey(m2, KeyEvent.KEYCODE_V, R.string.key_v)
                TextKey(m2, KeyEvent.KEYCODE_B, R.string.key_b)
                TextKey(m2, KeyEvent.KEYCODE_N, R.string.key_n)
                TextKey(m2, KeyEvent.KEYCODE_M, R.string.key_m)
                IconKey(
                    m3,
                    KeyEvent.KEYCODE_DEL,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m6 = Modifier.weight(2.0F)
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_UP,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                Box(m1)
                IconKey(
                    m6,
                    KeyEvent.KEYCODE_SPACE,
                    R.drawable.space,
                    R.string.key_space,
                )
                Box(m1)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    R.drawable.arrow_right,
                    R.string.key_right,
                )
            }
        }
    }

    @Composable
    fun ColemakKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, KeyEvent.KEYCODE_Q, R.string.key_q)
                TextKey(m1, KeyEvent.KEYCODE_W, R.string.key_w)
                TextKey(m1, KeyEvent.KEYCODE_F, R.string.key_f)
                TextKey(m1, KeyEvent.KEYCODE_P, R.string.key_p)
                TextKey(m1, KeyEvent.KEYCODE_G, R.string.key_g)
                TextKey(m1, KeyEvent.KEYCODE_J, R.string.key_j)
                TextKey(m1, KeyEvent.KEYCODE_L, R.string.key_l)
                TextKey(m1, KeyEvent.KEYCODE_U, R.string.key_u)
                TextKey(m1, KeyEvent.KEYCODE_Y, R.string.key_y)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, KeyEvent.KEYCODE_A, R.string.key_a)
                TextKey(m1, KeyEvent.KEYCODE_R, R.string.key_r)
                TextKey(m1, KeyEvent.KEYCODE_S, R.string.key_s)
                TextKey(m1, KeyEvent.KEYCODE_T, R.string.key_t)
                TextKey(m1, KeyEvent.KEYCODE_D, R.string.key_d)
                TextKey(m1, KeyEvent.KEYCODE_H, R.string.key_h)
                TextKey(m1, KeyEvent.KEYCODE_N, R.string.key_n)
                TextKey(m1, KeyEvent.KEYCODE_E, R.string.key_e)
                TextKey(m1, KeyEvent.KEYCODE_I, R.string.key_i)
                TextKey(m1, KeyEvent.KEYCODE_O, R.string.key_o)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m3 = Modifier.weight(3.0F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                Box(m1)
                TextKey(m2, KeyEvent.KEYCODE_Z, R.string.key_z)
                TextKey(m2, KeyEvent.KEYCODE_X, R.string.key_x)
                TextKey(m2, KeyEvent.KEYCODE_C, R.string.key_c)
                TextKey(m2, KeyEvent.KEYCODE_V, R.string.key_v)
                TextKey(m2, KeyEvent.KEYCODE_B, R.string.key_b)
                TextKey(m2, KeyEvent.KEYCODE_K, R.string.key_k)
                TextKey(m2, KeyEvent.KEYCODE_M, R.string.key_m)
                IconKey(
                    m3,
                    KeyEvent.KEYCODE_DEL,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m6 = Modifier.weight(2.0F)
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_UP,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                Box(m1)
                IconKey(
                    m6,
                    KeyEvent.KEYCODE_SPACE,
                    R.drawable.space,
                    R.string.key_space,
                )
                Box(m1)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    R.drawable.arrow_right,
                    R.string.key_right,
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
                    KeyEvent.KEYCODE_DEL,
                    R.drawable.delete,
                    R.string.key_delete,
                )
                SpecialIconKey(
                    m3,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                TextKey(m2, KeyEvent.KEYCODE_P, R.string.key_p)
                TextKey(m2, KeyEvent.KEYCODE_Y, R.string.key_y)
                TextKey(m2, KeyEvent.KEYCODE_F, R.string.key_f)
                TextKey(m2, KeyEvent.KEYCODE_G, R.string.key_g)
                TextKey(m2, KeyEvent.KEYCODE_C, R.string.key_c)
                TextKey(m2, KeyEvent.KEYCODE_R, R.string.key_r)
                TextKey(m2, KeyEvent.KEYCODE_L, R.string.key_l)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, KeyEvent.KEYCODE_A, R.string.key_a)
                TextKey(m1, KeyEvent.KEYCODE_O, R.string.key_o)
                TextKey(m1, KeyEvent.KEYCODE_E, R.string.key_e)
                TextKey(m1, KeyEvent.KEYCODE_U, R.string.key_u)
                TextKey(m1, KeyEvent.KEYCODE_I, R.string.key_i)
                TextKey(m1, KeyEvent.KEYCODE_D, R.string.key_d)
                TextKey(m1, KeyEvent.KEYCODE_H, R.string.key_h)
                TextKey(m1, KeyEvent.KEYCODE_T, R.string.key_t)
                TextKey(m1, KeyEvent.KEYCODE_N, R.string.key_n)
                TextKey(m1, KeyEvent.KEYCODE_S, R.string.key_s)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                Box(m1)
                TextKey(m1, KeyEvent.KEYCODE_Q, R.string.key_q)
                TextKey(m1, KeyEvent.KEYCODE_J, R.string.key_j)
                TextKey(m1, KeyEvent.KEYCODE_K, R.string.key_k)
                TextKey(m1, KeyEvent.KEYCODE_X, R.string.key_x)
                TextKey(m1, KeyEvent.KEYCODE_B, R.string.key_b)
                TextKey(m1, KeyEvent.KEYCODE_M, R.string.key_m)
                TextKey(m1, KeyEvent.KEYCODE_W, R.string.key_w)
                TextKey(m1, KeyEvent.KEYCODE_V, R.string.key_v)
                TextKey(m1, KeyEvent.KEYCODE_Z, R.string.key_z)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                val m6 = Modifier.weight(2.0F)
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_UP,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                Box(m1)
                IconKey(
                    m6,
                    KeyEvent.KEYCODE_SPACE,
                    R.drawable.space,
                    R.string.key_space,
                )
                Box(m1)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m2,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    R.drawable.arrow_right,
                    R.string.key_right,
                )
            }
        }
    }

    HorizontalDivider()
    when (layout) {
        KeyboardLayout.KL_QWERTY -> QwertyKeyboard()
        KeyboardLayout.KL_QWERTZ -> QwertzKeyboard()
        KeyboardLayout.KL_COLEMAK -> ColemakKeyboard()
        KeyboardLayout.KL_DVORAK -> DvorakKeyboard()
        else -> { QwertyKeyboard() }
    }
    if (showHideButton)
        HideButtonRow()
}


