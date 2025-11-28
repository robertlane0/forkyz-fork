
package app.crossword.yourealwaysbe.forkyz.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme

data class BoardColorScheme(
    val cellColor : Color,
    val blockColor : Color,
    val onBlockColor : Color,
    val errorColor : Color,
    val errorHighlightColor : Color,
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
)

private val lightScheme = lightColorScheme(
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

private val darkScheme = darkColorScheme(
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
    errorHighlightColor = errorHighlightColorLight,
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
    errorHighlightColor = errorHighlightColorDark,
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
)

private val darkCircleProgressBarColorScheme = CircleProgressBarColorScheme(
    progressNull = progressNullDark,
    progressInProgress = progressInProgressDark,
    progressDone = progressDoneDark,
    progressError = progressErrorDark,
)

@Composable
fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when (appTheme) {
        // only diff is app bar, see ForkyzTopAppBar
        Theme.T_LEGACY_LIKE -> if (darkMode) darkScheme else lightScheme
        Theme.T_DYNAMIC ->
            if (DynamicColors.isDynamicColorAvailable()) {
                if (darkMode) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkMode) darkScheme else lightScheme
            }
        // Theme.T_STANDARD
        else -> if (darkMode) darkScheme else lightScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val isLight = !darkMode && appTheme != Theme.T_LEGACY_LIKE
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

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
                        stringResource(R.string.back),
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
