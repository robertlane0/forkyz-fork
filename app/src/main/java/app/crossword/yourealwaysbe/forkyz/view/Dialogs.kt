
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val DIALOG_BUTTON_PADDING = 4.dp
private val DIALOG_PADDING = 20.dp
private val DIALOG_ROUNDED_CORNERS = RoundedCornerShape(26.dp)

/**
 * OK Cancel dialog with customisable body
 *
 * @param title the title resource id
 * @param summary the summary resource id or null
 * @param onCancel call back for cancel pressed
 * @param onOK call back for OK pressed
 * @param body the dialog body
 */
@Composable
fun OKCancelDialog(
    title : Int,
    summary : Int?,
    onCancel : () -> Unit,
    onOK : () -> Unit,
    body : @Composable () -> Unit = { },
) {
    PlainDialog(
        title = title,
        summary = summary?.let { stringResource(it) },
        onClose = onCancel,
    ) {
        body()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(
                    horizontal = DIALOG_BUTTON_PADDING,
                ),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                onClick = onOK,
                modifier = Modifier.padding(
                    horizontal = DIALOG_BUTTON_PADDING,
                ),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

/**
 * OK dialog with customisable body
 *
 * @param title the title resource id
 * @param summary the summary message in HTML
 * @param onClose call back for closing dialog
 * @param body the dialog body
 */
@Composable
fun OKDialog(
    title : Int,
    summary : String?,
    onClose : () -> Unit,
    body : @Composable () -> Unit = { },
) {
    PlainDialog(
        title = title,
        summary = summary,
        onClose = onClose
    ) {
        body()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.padding(
                    horizontal = DIALOG_BUTTON_PADDING,
                ),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

/**
 * Dialog with no buttons
 *
 * @param title the title resource id
 * @param summary the summary message in HTML
 * @param onClose call back for closing dialog by outside click
 * @param body the dialog body
 */
@Composable
private fun PlainDialog(
    title : Int,
    summary : String?,
    onClose : () -> Unit,
    body : @Composable () -> Unit = { },
) {
    Dialog(onDismissRequest = onClose) {
        Card(shape = DIALOG_ROUNDED_CORNERS) {
            Column(
                modifier = Modifier.padding(DIALOG_PADDING),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier.padding(bottom = DIALOG_PADDING),
                    text = stringResource(title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                summary?.let { summary ->
                    Text(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = DIALOG_PADDING),
                        text = AnnotatedString.fromHtml(summary),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                body()
            }
        }
    }
}

