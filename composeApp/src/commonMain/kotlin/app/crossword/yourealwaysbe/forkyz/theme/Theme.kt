
package app.crossword.yourealwaysbe.forkyz.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.back
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme

data class BoardColorScheme(
    val cellColor : Color,
    val blockColor : Color,
    val onBlockColor : Color,
    val errorColor : Color,
    val currentWordHighlightColor : Color,
    val currentLetterHighlightColor : Color,
    val cheatedColor : Color,
    val boardLetterColor : Color,
    val boardNoteColor : Color,
    val boardShapeColor : Color,
    val blockShapeColor : Color,
    val flagColor : Color,
    val flagColor1 : Color,
    val flagColor2 : Color,
    val flagColor3 : Color,
    val flagColor4 : Color,
)

data class CircleProgressBarColorScheme(
    val progressNull : Color,
    val progressInProgress : Color,
    val progressDone : Color,
    val progressError : Color,
    val progressHinted : Color,
)

internal val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    //outlineVariant = outlineVariantLight,
    //scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    //surfaceDim = surfaceDimLight,
    //surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

internal val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    //outlineVariant = outlineVariantDark,
    //scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    //surfaceDim = surfaceDimDark,
    //surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

internal val greenLightScheme = lightColorScheme(
    primary = primaryGreenLight,
    onPrimary = onPrimaryGreenLight,
    primaryContainer = primaryContainerGreenLight,
    onPrimaryContainer = onPrimaryContainerGreenLight,
    secondary = secondaryGreenLight,
    onSecondary = onSecondaryGreenLight,
    secondaryContainer = secondaryContainerGreenLight,
    onSecondaryContainer = onSecondaryContainerGreenLight,
    tertiary = tertiaryGreenLight,
    onTertiary = onTertiaryGreenLight,
    tertiaryContainer = tertiaryContainerGreenLight,
    onTertiaryContainer = onTertiaryContainerGreenLight,
    error = errorGreenLight,
    onError = onErrorGreenLight,
    errorContainer = errorContainerGreenLight,
    onErrorContainer = onErrorContainerGreenLight,
    background = backgroundGreenLight,
    onBackground = onBackgroundGreenLight,
    surface = surfaceGreenLight,
    onSurface = onSurfaceGreenLight,
    surfaceVariant = surfaceVariantGreenLight,
    onSurfaceVariant = onSurfaceVariantGreenLight,
    outline = outlineGreenLight,
    //outlineVariant = outlineVariantGreenLight,
    //scrim = scrimGreenLight,
    inverseSurface = inverseSurfaceGreenLight,
    inverseOnSurface = inverseOnSurfaceGreenLight,
    inversePrimary = inversePrimaryGreenLight,
    //surfaceDim = surfaceDimGreenLight,
    //surfaceBright = surfaceBrightGreenLight,
    surfaceContainerLowest = surfaceContainerLowestGreenLight,
    surfaceContainerLow = surfaceContainerLowGreenLight,
    surfaceContainer = surfaceContainerGreenLight,
    surfaceContainerHigh = surfaceContainerHighGreenLight,
    surfaceContainerHighest = surfaceContainerHighestGreenLight,
)

internal val greenDarkScheme = darkColorScheme(
    primary = primaryGreenDark,
    onPrimary = onPrimaryGreenDark,
    primaryContainer = primaryContainerGreenDark,
    onPrimaryContainer = onPrimaryContainerGreenDark,
    secondary = secondaryGreenDark,
    onSecondary = onSecondaryGreenDark,
    secondaryContainer = secondaryContainerGreenDark,
    onSecondaryContainer = onSecondaryContainerGreenDark,
    tertiary = tertiaryGreenDark,
    onTertiary = onTertiaryGreenDark,
    tertiaryContainer = tertiaryContainerGreenDark,
    onTertiaryContainer = onTertiaryContainerGreenDark,
    error = errorGreenDark,
    onError = onErrorGreenDark,
    errorContainer = errorContainerGreenDark,
    onErrorContainer = onErrorContainerGreenDark,
    background = backgroundGreenDark,
    onBackground = onBackgroundGreenDark,
    surface = surfaceGreenDark,
    onSurface = onSurfaceGreenDark,
    surfaceVariant = surfaceVariantGreenDark,
    onSurfaceVariant = onSurfaceVariantGreenDark,
    outline = outlineGreenDark,
    //outlineVariant = outlineVariantGreenDark,
    //scrim = scrimGreenDark,
    inverseSurface = inverseSurfaceGreenDark,
    inverseOnSurface = inverseOnSurfaceGreenDark,
    inversePrimary = inversePrimaryGreenDark,
    //surfaceDim = surfaceDimGreenDark,
    //surfaceBright = surfaceBrightGreenDark,
    surfaceContainerLowest = surfaceContainerLowestGreenDark,
    surfaceContainerLow = surfaceContainerLowGreenDark,
    surfaceContainer = surfaceContainerGreenDark,
    surfaceContainerHigh = surfaceContainerHighGreenDark,
    surfaceContainerHighest = surfaceContainerHighestGreenDark,
)

internal val blueLightScheme = lightColorScheme(
    primary = primaryBlueLight,
    onPrimary = onPrimaryBlueLight,
    primaryContainer = primaryContainerBlueLight,
    onPrimaryContainer = onPrimaryContainerBlueLight,
    secondary = secondaryBlueLight,
    onSecondary = onSecondaryBlueLight,
    secondaryContainer = secondaryContainerBlueLight,
    onSecondaryContainer = onSecondaryContainerBlueLight,
    tertiary = tertiaryBlueLight,
    onTertiary = onTertiaryBlueLight,
    tertiaryContainer = tertiaryContainerBlueLight,
    onTertiaryContainer = onTertiaryContainerBlueLight,
    error = errorBlueLight,
    onError = onErrorBlueLight,
    errorContainer = errorContainerBlueLight,
    onErrorContainer = onErrorContainerBlueLight,
    background = backgroundBlueLight,
    onBackground = onBackgroundBlueLight,
    surface = surfaceBlueLight,
    onSurface = onSurfaceBlueLight,
    surfaceVariant = surfaceVariantBlueLight,
    onSurfaceVariant = onSurfaceVariantBlueLight,
    outline = outlineBlueLight,
    //outlineVariant = outlineVariantBlueLight,
    //scrim = scrimBlueLight,
    inverseSurface = inverseSurfaceBlueLight,
    inverseOnSurface = inverseOnSurfaceBlueLight,
    inversePrimary = inversePrimaryBlueLight,
    //surfaceDim = surfaceDimBlueLight,
    //surfaceBright = surfaceBrightBlueLight,
    surfaceContainerLowest = surfaceContainerLowestBlueLight,
    surfaceContainerLow = surfaceContainerLowBlueLight,
    surfaceContainer = surfaceContainerBlueLight,
    surfaceContainerHigh = surfaceContainerHighBlueLight,
    surfaceContainerHighest = surfaceContainerHighestBlueLight,
)

internal val blueDarkScheme = darkColorScheme(
    primary = primaryBlueDark,
    onPrimary = onPrimaryBlueDark,
    primaryContainer = primaryContainerBlueDark,
    onPrimaryContainer = onPrimaryContainerBlueDark,
    secondary = secondaryBlueDark,
    onSecondary = onSecondaryBlueDark,
    secondaryContainer = secondaryContainerBlueDark,
    onSecondaryContainer = onSecondaryContainerBlueDark,
    tertiary = tertiaryBlueDark,
    onTertiary = onTertiaryBlueDark,
    tertiaryContainer = tertiaryContainerBlueDark,
    onTertiaryContainer = onTertiaryContainerBlueDark,
    error = errorBlueDark,
    onError = onErrorBlueDark,
    errorContainer = errorContainerBlueDark,
    onErrorContainer = onErrorContainerBlueDark,
    background = backgroundBlueDark,
    onBackground = onBackgroundBlueDark,
    surface = surfaceBlueDark,
    onSurface = onSurfaceBlueDark,
    surfaceVariant = surfaceVariantBlueDark,
    onSurfaceVariant = onSurfaceVariantBlueDark,
    outline = outlineBlueDark,
    //outlineVariant = outlineVariantBlueDark,
    //scrim = scrimBlueDark,
    inverseSurface = inverseSurfaceBlueDark,
    inverseOnSurface = inverseOnSurfaceBlueDark,
    inversePrimary = inversePrimaryBlueDark,
    //surfaceDim = surfaceDimBlueDark,
    //surfaceBright = surfaceBrightBlueDark,
    surfaceContainerLowest = surfaceContainerLowestBlueDark,
    surfaceContainerLow = surfaceContainerLowBlueDark,
    surfaceContainer = surfaceContainerBlueDark,
    surfaceContainerHigh = surfaceContainerHighBlueDark,
    surfaceContainerHighest = surfaceContainerHighestBlueDark,
)

internal val orangeLightScheme = lightColorScheme(
    primary = primaryOrangeLight,
    onPrimary = onPrimaryOrangeLight,
    primaryContainer = primaryContainerOrangeLight,
    onPrimaryContainer = onPrimaryContainerOrangeLight,
    secondary = secondaryOrangeLight,
    onSecondary = onSecondaryOrangeLight,
    secondaryContainer = secondaryContainerOrangeLight,
    onSecondaryContainer = onSecondaryContainerOrangeLight,
    tertiary = tertiaryOrangeLight,
    onTertiary = onTertiaryOrangeLight,
    tertiaryContainer = tertiaryContainerOrangeLight,
    onTertiaryContainer = onTertiaryContainerOrangeLight,
    error = errorOrangeLight,
    onError = onErrorOrangeLight,
    errorContainer = errorContainerOrangeLight,
    onErrorContainer = onErrorContainerOrangeLight,
    background = backgroundOrangeLight,
    onBackground = onBackgroundOrangeLight,
    surface = surfaceOrangeLight,
    onSurface = onSurfaceOrangeLight,
    surfaceVariant = surfaceVariantOrangeLight,
    onSurfaceVariant = onSurfaceVariantOrangeLight,
    outline = outlineOrangeLight,
    //outlineVariant = outlineVariantOrangeLight,
    //scrim = scrimOrangeLight,
    inverseSurface = inverseSurfaceOrangeLight,
    inverseOnSurface = inverseOnSurfaceOrangeLight,
    inversePrimary = inversePrimaryOrangeLight,
    //surfaceDim = surfaceDimOrangeLight,
    //surfaceBright = surfaceBrightOrangeLight,
    surfaceContainerLowest = surfaceContainerLowestOrangeLight,
    surfaceContainerLow = surfaceContainerLowOrangeLight,
    surfaceContainer = surfaceContainerOrangeLight,
    surfaceContainerHigh = surfaceContainerHighOrangeLight,
    surfaceContainerHighest = surfaceContainerHighestOrangeLight,
)

internal val orangeDarkScheme = darkColorScheme(
    primary = primaryOrangeDark,
    onPrimary = onPrimaryOrangeDark,
    primaryContainer = primaryContainerOrangeDark,
    onPrimaryContainer = onPrimaryContainerOrangeDark,
    secondary = secondaryOrangeDark,
    onSecondary = onSecondaryOrangeDark,
    secondaryContainer = secondaryContainerOrangeDark,
    onSecondaryContainer = onSecondaryContainerOrangeDark,
    tertiary = tertiaryOrangeDark,
    onTertiary = onTertiaryOrangeDark,
    tertiaryContainer = tertiaryContainerOrangeDark,
    onTertiaryContainer = onTertiaryContainerOrangeDark,
    error = errorOrangeDark,
    onError = onErrorOrangeDark,
    errorContainer = errorContainerOrangeDark,
    onErrorContainer = onErrorContainerOrangeDark,
    background = backgroundOrangeDark,
    onBackground = onBackgroundOrangeDark,
    surface = surfaceOrangeDark,
    onSurface = onSurfaceOrangeDark,
    surfaceVariant = surfaceVariantOrangeDark,
    onSurfaceVariant = onSurfaceVariantOrangeDark,
    outline = outlineOrangeDark,
    //outlineVariant = outlineVariantOrangeDark,
    //scrim = scrimOrangeDark,
    inverseSurface = inverseSurfaceOrangeDark,
    inverseOnSurface = inverseOnSurfaceOrangeDark,
    inversePrimary = inversePrimaryOrangeDark,
    //surfaceDim = surfaceDimOrangeDark,
    //surfaceBright = surfaceBrightOrangeDark,
    surfaceContainerLowest = surfaceContainerLowestOrangeDark,
    surfaceContainerLow = surfaceContainerLowOrangeDark,
    surfaceContainer = surfaceContainerOrangeDark,
    surfaceContainerHigh = surfaceContainerHighOrangeDark,
    surfaceContainerHighest = surfaceContainerHighestOrangeDark,
)

private val lightBoardColorScheme = BoardColorScheme(
    currentWordHighlightColor = currentWordHighlightColorLight,
    currentLetterHighlightColor = currentLetterHighlightColorLight,
    cheatedColor = cheatedColorLight,
    boardLetterColor = boardLetterColorLight,
    boardNoteColor = boardNoteColorLight,
    boardShapeColor = boardShapeColorLight,
    blockShapeColor = blockShapeColorLight,
    blockColor = blockColorLight,
    onBlockColor = onBlockColorLight,
    errorColor = errorColorLight,
    cellColor = cellColorLight,
    flagColor = flagColorLight,
    flagColor1 = flagColor1,
    flagColor2 = flagColor2,
    flagColor3 = flagColor3,
    flagColor4 = flagColor4,
)

private val darkBoardColorScheme = BoardColorScheme(
    currentWordHighlightColor = currentWordHighlightColorDark,
    currentLetterHighlightColor = currentLetterHighlightColorDark,
    cheatedColor = cheatedColorDark,
    boardLetterColor = boardLetterColorDark,
    boardNoteColor = boardNoteColorDark,
    boardShapeColor = boardShapeColorDark,
    blockShapeColor = blockShapeColorDark,
    blockColor = blockColorDark,
    onBlockColor = onBlockColorDark,
    errorColor = errorColorDark,
    cellColor = cellColorDark,
    flagColor = flagColorDark,
    flagColor1 = flagColor1,
    flagColor2 = flagColor2,
    flagColor3 = flagColor3,
    flagColor4 = flagColor4,
)

private val lightCircleProgressBarColorScheme = CircleProgressBarColorScheme(
    progressNull = progressNullLight,
    progressInProgress = progressInProgressLight,
    progressDone = progressDoneLight,
    progressError = progressErrorLight,
    progressHinted = cheatedColorLight,
)

private val darkCircleProgressBarColorScheme = CircleProgressBarColorScheme(
    progressNull = progressNullDark,
    progressInProgress = progressInProgressDark,
    progressDone = progressDoneDark,
    progressError = progressErrorDark,
    progressHinted = cheatedColorDark,
)

@Composable
expect fun getDynamicTheme(darkMode : Boolean) : ColorScheme

@Composable
internal fun getColorScheme(
    appTheme : Theme,
    darkMode : Boolean,
) : ColorScheme {
    return when (appTheme) {
        // only diff is app bar, see ForkyzTopAppBar
        Theme.T_LEGACY_LIKE -> if (darkMode) darkScheme else lightScheme
        Theme.T_DYNAMIC -> getDynamicTheme(darkMode)
        Theme.T_GREEN -> if (darkMode) greenDarkScheme else greenLightScheme
        Theme.T_BLUE -> if (darkMode) blueDarkScheme else blueLightScheme
        Theme.T_ORANGE -> if (darkMode) orangeDarkScheme else orangeLightScheme
        // Theme.T_STANDARD
        else -> if (darkMode) darkScheme else lightScheme
    }
}

@Composable
expect fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun statusBarColor(appTheme : Theme, darkMode : Boolean) : Color {
    return if (appTheme == Theme.T_LEGACY_LIKE && !darkMode)
        return MaterialTheme.colorScheme.primary
    else
        // not ideal and relies on top bar not changing on scroll
        // on puzzle activity where this is used
        return getTopAppBarColors(appTheme, darkMode).containerColor
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ForkyzTopAppBar(
    modifier : Modifier = Modifier,
    appTheme : Theme,
    darkMode : Boolean,
    title : @Composable () -> Unit,
    actions : @Composable RowScope.() -> Unit = { },
    onBack : (() -> Unit)? = null,
    scrollBehavior : TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        colors = getTopAppBarColors(appTheme, darkMode),
        title = title,
        navigationIcon = {
            onBack?.let { onBack ->
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(Res.string.back),
                    )
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

fun getBoardColorScheme(darkMode : Boolean) : BoardColorScheme {
    return if (darkMode) darkBoardColorScheme else lightBoardColorScheme
}

fun getCircleProgressBarColorScheme(
    darkMode : Boolean,
) : CircleProgressBarColorScheme {
    return if (darkMode)
        darkCircleProgressBarColorScheme
    else
        lightCircleProgressBarColorScheme
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun getTopAppBarColors(
    appTheme : Theme,
    darkMode : Boolean,
) : TopAppBarColors {
    return if (appTheme == Theme.T_LEGACY_LIKE && !darkMode) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            scrolledContainerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        TopAppBarDefaults.topAppBarColors()
    }
}
