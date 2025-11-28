
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import app.crossword.yourealwaysbe.forkyz.R
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
        inputConnectionKey(code, inputConnectionMediator.keyDown)
    }
    val onKeyUp = { code : Int ->
        inputConnectionKey(code, inputConnectionMediator.keyUp)
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
                text = stringResource(R.string.key_special_entry),
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
                TextKey(m1, inputConnectionMediator.keyQ, R.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, R.string.key_w)
                TextKey(m1, inputConnectionMediator.keyE, R.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, R.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, R.string.key_t)
                TextKey(m1, inputConnectionMediator.keyY, R.string.key_y)
                TextKey(m1, inputConnectionMediator.keyU, R.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, R.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, R.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, R.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyA, R.string.key_a)
                TextKey(m2, inputConnectionMediator.keyS, R.string.key_s)
                TextKey(m2, inputConnectionMediator.keyD, R.string.key_d)
                TextKey(m2, inputConnectionMediator.keyF, R.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, R.string.key_g)
                TextKey(m2, inputConnectionMediator.keyH, R.string.key_h)
                TextKey(m2, inputConnectionMediator.keyJ, R.string.key_j)
                TextKey(m2, inputConnectionMediator.keyK, R.string.key_k)
                TextKey(m2, inputConnectionMediator.keyL, R.string.key_l)
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
                TextKey(m2, inputConnectionMediator.keyZ, R.string.key_z)
                TextKey(m2, inputConnectionMediator.keyX, R.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, R.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, R.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, R.string.key_b)
                TextKey(m2, inputConnectionMediator.keyN, R.string.key_n)
                TextKey(m2, inputConnectionMediator.keyM, R.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    R.drawable.space,
                    R.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
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
                TextKey(m1, inputConnectionMediator.keyQ, R.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, R.string.key_w)
                TextKey(m1, inputConnectionMediator.keyE, R.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, R.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, R.string.key_t)
                TextKey(m1, inputConnectionMediator.keyZ, R.string.key_z)
                TextKey(m1, inputConnectionMediator.keyU, R.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, R.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, R.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, R.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(2.0F)
                Box(m1)
                TextKey(m2, inputConnectionMediator.keyA, R.string.key_a)
                TextKey(m2, inputConnectionMediator.keyS, R.string.key_s)
                TextKey(m2, inputConnectionMediator.keyD, R.string.key_d)
                TextKey(m2, inputConnectionMediator.keyF, R.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, R.string.key_g)
                TextKey(m2, inputConnectionMediator.keyH, R.string.key_h)
                TextKey(m2, inputConnectionMediator.keyJ, R.string.key_j)
                TextKey(m2, inputConnectionMediator.keyK, R.string.key_k)
                TextKey(m2, inputConnectionMediator.keyL, R.string.key_l)
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
                TextKey(m2, inputConnectionMediator.keyY, R.string.key_y)
                TextKey(m2, inputConnectionMediator.keyX, R.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, R.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, R.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, R.string.key_b)
                TextKey(m2, inputConnectionMediator.keyN, R.string.key_n)
                TextKey(m2, inputConnectionMediator.keyM, R.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    R.drawable.space,
                    R.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
                    R.drawable.arrow_right,
                    R.string.key_right,
                )
            }
        }
    }

    @Composable
    fun AzertyKeyboard() {
        Column(modifier.fillMaxWidth()) {
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, R.string.key_a)
                TextKey(m1, inputConnectionMediator.keyZ, R.string.key_z)
                TextKey(m1, inputConnectionMediator.keyE, R.string.key_e)
                TextKey(m1, inputConnectionMediator.keyR, R.string.key_r)
                TextKey(m1, inputConnectionMediator.keyT, R.string.key_t)
                TextKey(m1, inputConnectionMediator.keyY, R.string.key_y)
                TextKey(m1, inputConnectionMediator.keyU, R.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, R.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, R.string.key_o)
                TextKey(m1, inputConnectionMediator.keyP, R.string.key_p)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyQ, R.string.key_q)
                TextKey(m1, inputConnectionMediator.keyS, R.string.key_s)
                TextKey(m1, inputConnectionMediator.keyD, R.string.key_d)
                TextKey(m1, inputConnectionMediator.keyF, R.string.key_f)
                TextKey(m1, inputConnectionMediator.keyG, R.string.key_g)
                TextKey(m1, inputConnectionMediator.keyH, R.string.key_h)
                TextKey(m1, inputConnectionMediator.keyJ, R.string.key_j)
                TextKey(m1, inputConnectionMediator.keyK, R.string.key_k)
                TextKey(m1, inputConnectionMediator.keyL, R.string.key_l)
                TextKey(m1, inputConnectionMediator.keyM, R.string.key_m)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m2 = Modifier.weight(1.5F)
                SpecialIconKey(
                    m2,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                TextKey(m1, inputConnectionMediator.keyW, R.string.key_w)
                TextKey(m1, inputConnectionMediator.keyX, R.string.key_x)
                TextKey(m1, inputConnectionMediator.keyC, R.string.key_c)
                TextKey(m1, inputConnectionMediator.keyV, R.string.key_v)
                TextKey(m1, inputConnectionMediator.keyB, R.string.key_b)
                TextKey(m1, inputConnectionMediator.keyN, R.string.key_n)
                Box(m1)
                IconKey(
                    m2,
                    inputConnectionMediator.keyDelete,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    R.drawable.space,
                    R.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
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
                TextKey(m1, inputConnectionMediator.keyQ, R.string.key_q)
                TextKey(m1, inputConnectionMediator.keyW, R.string.key_w)
                TextKey(m1, inputConnectionMediator.keyF, R.string.key_f)
                TextKey(m1, inputConnectionMediator.keyP, R.string.key_p)
                TextKey(m1, inputConnectionMediator.keyG, R.string.key_g)
                TextKey(m1, inputConnectionMediator.keyJ, R.string.key_j)
                TextKey(m1, inputConnectionMediator.keyL, R.string.key_l)
                TextKey(m1, inputConnectionMediator.keyU, R.string.key_u)
                TextKey(m1, inputConnectionMediator.keyY, R.string.key_y)
                Box(m1)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, R.string.key_a)
                TextKey(m1, inputConnectionMediator.keyR, R.string.key_r)
                TextKey(m1, inputConnectionMediator.keyS, R.string.key_s)
                TextKey(m1, inputConnectionMediator.keyT, R.string.key_t)
                TextKey(m1, inputConnectionMediator.keyD, R.string.key_d)
                TextKey(m1, inputConnectionMediator.keyH, R.string.key_h)
                TextKey(m1, inputConnectionMediator.keyN, R.string.key_n)
                TextKey(m1, inputConnectionMediator.keyE, R.string.key_e)
                TextKey(m1, inputConnectionMediator.keyI, R.string.key_i)
                TextKey(m1, inputConnectionMediator.keyO, R.string.key_o)
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
                TextKey(m2, inputConnectionMediator.keyZ, R.string.key_z)
                TextKey(m2, inputConnectionMediator.keyX, R.string.key_x)
                TextKey(m2, inputConnectionMediator.keyC, R.string.key_c)
                TextKey(m2, inputConnectionMediator.keyV, R.string.key_v)
                TextKey(m2, inputConnectionMediator.keyB, R.string.key_b)
                TextKey(m2, inputConnectionMediator.keyK, R.string.key_k)
                TextKey(m2, inputConnectionMediator.keyM, R.string.key_m)
                IconKey(
                    m3,
                    inputConnectionMediator.keyDelete,
                    R.drawable.delete,
                    R.string.key_delete,
                )
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    R.drawable.space,
                    R.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
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
                    inputConnectionMediator.keyDelete,
                    R.drawable.delete,
                    R.string.key_delete,
                )
                SpecialIconKey(
                    m3,
                    SpecialKey.KEY_CHANGE_CLUE_DIRECTION,
                    R.drawable.ic_change_direction,
                    R.string.change_direction,
                )
                TextKey(m2, inputConnectionMediator.keyP, R.string.key_p)
                TextKey(m2, inputConnectionMediator.keyY, R.string.key_y)
                TextKey(m2, inputConnectionMediator.keyF, R.string.key_f)
                TextKey(m2, inputConnectionMediator.keyG, R.string.key_g)
                TextKey(m2, inputConnectionMediator.keyC, R.string.key_c)
                TextKey(m2, inputConnectionMediator.keyR, R.string.key_r)
                TextKey(m2, inputConnectionMediator.keyL, R.string.key_l)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                TextKey(m1, inputConnectionMediator.keyA, R.string.key_a)
                TextKey(m1, inputConnectionMediator.keyO, R.string.key_o)
                TextKey(m1, inputConnectionMediator.keyE, R.string.key_e)
                TextKey(m1, inputConnectionMediator.keyU, R.string.key_u)
                TextKey(m1, inputConnectionMediator.keyI, R.string.key_i)
                TextKey(m1, inputConnectionMediator.keyD, R.string.key_d)
                TextKey(m1, inputConnectionMediator.keyH, R.string.key_h)
                TextKey(m1, inputConnectionMediator.keyT, R.string.key_t)
                TextKey(m1, inputConnectionMediator.keyN, R.string.key_n)
                TextKey(m1, inputConnectionMediator.keyS, R.string.key_s)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                Box(m1)
                TextKey(m1, inputConnectionMediator.keyQ, R.string.key_q)
                TextKey(m1, inputConnectionMediator.keyJ, R.string.key_j)
                TextKey(m1, inputConnectionMediator.keyK, R.string.key_k)
                TextKey(m1, inputConnectionMediator.keyX, R.string.key_x)
                TextKey(m1, inputConnectionMediator.keyB, R.string.key_b)
                TextKey(m1, inputConnectionMediator.keyM, R.string.key_m)
                TextKey(m1, inputConnectionMediator.keyW, R.string.key_w)
                TextKey(m1, inputConnectionMediator.keyV, R.string.key_v)
                TextKey(m1, inputConnectionMediator.keyZ, R.string.key_z)
            }
            Row() {
                val m1 = Modifier.weight(1.0F)
                val m4 = Modifier.weight(4.0F)
                IconKey(
                    m1,
                    inputConnectionMediator.keyUp,
                    R.drawable.arrow_up,
                    R.string.key_up,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyDown,
                    R.drawable.arrow_down,
                    R.string.key_down,
                )
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_PREVIOUS_CLUE,
                    R.drawable.ic_prev_clue,
                    R.string.prev_clue,
                )
                IconKey(
                    m4,
                    inputConnectionMediator.keySpace,
                    R.drawable.space,
                    R.string.key_space,
                )
                SpecialEntryKey(m1)
                SpecialIconKey(
                    m1,
                    SpecialKey.KEY_NEXT_CLUE,
                    R.drawable.ic_next_clue,
                    R.string.next_clue,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyLeft,
                    R.drawable.arrow_left,
                    R.string.key_left,
                )
                IconKey(
                    m1,
                    inputConnectionMediator.keyRight,
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
        KeyboardLayout.KL_AZERTY -> AzertyKeyboard()
        KeyboardLayout.KL_COLEMAK -> ColemakKeyboard()
        KeyboardLayout.KL_DVORAK -> DvorakKeyboard()
        else -> { QwertyKeyboard() }
    }
    if (showHideButton)
        HideButtonRow()
}


