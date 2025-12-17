
package app.crossword.yourealwaysbe.forkyz.menu

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_submenu
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_submenu
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.settings

val BaseMenuModifier = Modifier.defaultMinSize(minWidth = 200.dp)

@Composable
fun MenuText(text : String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun MenuSubEntry(
    text : @Composable () -> Unit,
    onClick : () -> Unit,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        trailingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_submenu),
                contentDescription = null,
            )
        }

    )
}

@Composable
fun MenuDropdownHeading(
    titleId : StringResource,
    onClick : () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_submenu),
                    modifier = Modifier.rotate(180f),
                    contentDescription = null,
                )
        },
        text = {
            Text(
                stringResource(titleId),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun MenuHelp(
    openHelp : () -> Unit = { },
    onDismiss : () -> Unit = { },
) {
    DropdownMenuItem(
        text = { MenuText(stringResource(Res.string.help)) },
        onClick = {
            openHelp()
            onDismiss()
        }
    )
}

@Composable
fun MenuSettings(
    openSettings : () -> Unit = { },
    onDismiss : () -> Unit = { },
) {
    DropdownMenuItem(
        text = { MenuText(stringResource(Res.string.settings)) },
        onClick = {
            openSettings()
            onDismiss()
        }
    )
}

