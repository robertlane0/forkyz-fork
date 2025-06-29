
package app.crossword.yourealwaysbe.forkyz.menu

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

import app.crossword.yourealwaysbe.forkyz.HTMLActivity
import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.SettingsActivity

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
                painter = painterResource(R.drawable.ic_submenu),
                contentDescription = null,
            )
        }

    )
}

@Composable
fun MenuDropdownHeading(
    titleId : Int,
    onClick : () -> Unit,
) {
    DropdownMenuItem(
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
    activity : Activity,
    helpHTMLAsset : String,
    onDismiss : () -> Unit = { },
) {
    DropdownMenuItem(
        text = { MenuText(stringResource(R.string.help)) },
        onClick = {
            actionHelp(activity, helpHTMLAsset)
            onDismiss()
        }
    )
}

@Composable
fun MenuSettings(
    activity : Activity,
    onDismiss : () -> Unit = { },
) {
    DropdownMenuItem(
        text = { MenuText(stringResource(R.string.settings)) },
        onClick = {
            actionSettings(activity)
            onDismiss()
        }
    )
}

private fun actionHelp(activity : Activity, helpHTMLAsset : String) {
    val helpIntent = Intent(
        Intent.ACTION_VIEW,
        helpHTMLAsset.toUri(),
        activity,
        HTMLActivity::class.java,
    )
    activity.startActivity(helpIntent)
}

private fun actionSettings(activity : Activity) {
    activity.startActivity(Intent(activity, SettingsActivity::class.java))
}


