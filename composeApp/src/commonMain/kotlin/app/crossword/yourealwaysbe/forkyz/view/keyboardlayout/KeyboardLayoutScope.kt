
package app.crossword.yourealwaysbe.forkyz.view.keyboardlayout

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_change_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_prev_clue
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
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.onMiddleClick
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey
import app.crossword.yourealwaysbe.forkyz.view.SpecialKey

// percent of display height
private val BUTTON_PADDING_FRAC = 0.003F
private val BUTTON_PADDING_FRAC_COMPACT = 0.0F
// used when repeat delay set to 0 (disabled)
private val DEFAULT_LONG_PRESS_DELAY = 300L // ms

internal class KeyboardLayoutScope(
    val modifier: Modifier,
    val compact: Boolean,
    val hapticFeedback: Boolean,
    val showSpecialKeys: Boolean,
    val inputConnectionMediator: InputConnectionMediator,
    val onSpecialKeyDown: (SpecialKey) -> Unit,
    val onSpecialKeyUp: (SpecialKey) -> Unit,
    val repeatDelay: Long,
    val repeatInterval: Long,
) {
    @Composable
    private fun Modifier.buttonVerticalPadding(): Modifier {
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
        modifier: Modifier = Modifier,
        onDown: () -> Unit,
        onUp: () -> Unit,
        onLongPress: (() -> Unit)? = null,
        content: @Composable (Modifier) -> Unit,
    ) {
        // key repeat with thanks to
        // https://proandroiddev.com/creating-a-repeating-button-with-jetpack-compose-b39c4f559f7d

        val currentOnDown by rememberUpdatedState(onDown)
        val currentOnUp by rememberUpdatedState(onUp)
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        content(
            modifier.focusProperties { canFocus = false }
                .onMiddleClick { onLongPress?.invoke() }
                .pointerInput(true) {
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

    fun min(x : TextUnit, y : TextUnit) : TextUnit {
        return if (x < y) x else y
    }

    fun max(x : TextUnit, y : TextUnit) : TextUnit {
        return if (x > y) x else y
    }

    /**
     * A single text key
     *
     * @param value the string value sent/displayed on key
     */
    @Composable
    fun StringKey(
        modifier: Modifier = Modifier,
        value: String,
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
                BoxWithConstraints {
                    val buttonTextStyle = with (LocalDensity.current) {
                            // scale if not much space assuming 1 char
                            // in string
                            val titleLarge = MaterialTheme.typography.titleLarge
                            val fontSize = max(
                                10.sp,
                                min(maxWidth.toSp(), titleLarge.fontSize),
                            )
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = fontSize,
                            )
                        }
                    Text(
                        text = value,
                        style = buttonTextStyle,
                    )
                }
            }
        }
    }

    /**
     * Special key for change direction or special entry on long press
     */
    @Composable
    fun ChangeDirectionSpecialEntryKey(modifier: Modifier = Modifier) {
        if (showSpecialKeys) {
            val changeDirLabel = stringResource(Res.string.change_direction)
            val longPressLabel = stringResource(Res.string.special_entry)
            BaseKey(
                modifier = modifier.semantics {
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
        modifier: Modifier = Modifier,
        symbol: DrawableResource,
        contentDescription: StringResource,
        onDown: () -> Unit,
        onUp: () -> Unit,
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
        modifier: Modifier = Modifier,
        key: OrdinaryKey,
        symbol: DrawableResource,
        contentDescription: StringResource,
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
        modifier: Modifier = Modifier,
        key: SpecialKey,
        symbol: DrawableResource,
        contentDescription: StringResource,
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
}
