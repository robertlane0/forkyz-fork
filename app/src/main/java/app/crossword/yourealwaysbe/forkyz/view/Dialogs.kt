
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import app.crossword.yourealwaysbe.forkyz.R

private val DIALOG_BUTTON_PADDING = 4.dp
private val DIALOG_PADDING = 20.dp
private val DIALOG_ROUNDED_CORNERS = RoundedCornerShape(26.dp)

private val DISABLED_ALPHA = 0.6F
private val ENABLED_ALPHA = 1.0F

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
    OKCancelDialog(
        title = stringResource(title),
        summary = summary?.let { stringResource(it) },
        onCancel = onCancel,
        onOK = onOK,
        body = body,
    )
}

@Composable
fun OKCancelDialog(
    title : String,
    summary : String?,
    onCancel : () -> Unit,
    onOK : () -> Unit,
    body : @Composable () -> Unit = { },
) {
    TwoButtonDialog(
        title = title,
        summary = summary,
        positiveText = stringResource(android.R.string.ok),
        onPositive = onOK,
        negativeText = stringResource(android.R.string.cancel),
        onNegative = onCancel,
        body = body,
    )
}

/**
 * A dialog with a positive and negative button
 *
 * @param title resource ID of title
 * @param summary resource ID of summary or null
 * @param postiveText resource ID of positive text
 * @param onPositive callback on positive
 * @param negativeText resource ID of negative text
 * @param onNegative callback on positive
 */
@Composable
fun TwoButtonDialog(
    title : Int,
    summary : Int?,
    positiveText : Int,
    onPositive : () -> Unit,
    negativeText : Int,
    onNegative: () -> Unit,
    body : @Composable () -> Unit = { },
) {
    TwoButtonDialog(
        title = stringResource(title),
        summary = summary?.let { stringResource(it) },
        positiveText = stringResource(positiveText),
        onPositive = onPositive,
        negativeText = stringResource(negativeText),
        onNegative = onNegative,
        body = body,
    )
}

@Composable
fun TwoButtonDialog(
    title : String,
    summary : String?,
    positiveText : String,
    onPositive : () -> Unit,
    negativeText : String,
    onNegative: () -> Unit,
    body : @Composable () -> Unit = { },
) {
    PlainDialog(
        title = title,
        summary = summary,
        onClose = onNegative,
    ) {
        body()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onNegative,
                modifier = Modifier.padding(
                    horizontal = DIALOG_BUTTON_PADDING,
                ),
            ) {
                Text(negativeText)
            }
            TextButton(
                onClick = onPositive,
                modifier = Modifier.padding(
                    horizontal = DIALOG_BUTTON_PADDING,
                ),
            ) {
                Text(positiveText)
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
 * List selection dialog
 *
 * @param labels list of labels to display by android resource id
 * @param selected index of item to be initially selected
 * @param onSelect call back when the indexth item is selected and oked
 * @param onDismissRequest call back when dismissed
 */
@Composable
fun ListDialog(
    title : Int,
    summary : Int?,
    labels : List<String>,
    selected : Int,
    onSelect : (Int) -> Unit,
    onDismissRequest : () -> Unit,
) {
    var selectedState by remember { mutableIntStateOf(selected) }
    // set again in case this is a relayout
    selectedState = selected

    OKCancelDialog(
        title = title,
        summary = summary,
        onCancel = onDismissRequest,
        onOK = { onSelect(selectedState) }
    ) {
        BoxWithConstraints() {
            LazyColumn(
                modifier = Modifier.heightIn(0.dp, maxHeight * 0.7f),
            ) {
                itemsIndexed(labels) { index, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { selectedState = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedState,
                            onClick = { selectedState = index },
                        )
                        Text(label)
                    }
                }
            }
        }
    }
}

/**
 * List multi-selection dialog
 *
 * @param allOrNoneLabel the label to give to the select all/none box,
 * no such box is displayed if null
 * @param numItems how many items to display
 * @param getItem composable function for displaying ith item
 * @param getEnabled true if item enabled
 * @param selected set of indices of items to be initially selected
 * @param onSelect call back when the set of selected is oked
 * @param onDismissRequest call back when dismissed
 * @param onSelectionChange when a new item is (de)selected
 * @param body additional stuff to display between title/summary and
 * list
 */
@Composable
fun MultiListDialog(
    title : Int,
    summary : Int? = null,
    allOrNoneLabel : Int? = null,
    numItems : Int,
    getItem : @Composable (Int) -> Unit,
    getEnabled : (Int) -> Boolean = { true },
    selected : Set<Int> = setOf(),
    onSelect : (Set<Int>) -> Unit = { },
    onDismissRequest : () -> Unit = { },
    onSelectionChange : (Set<Int>) -> Unit = { },
    body : @Composable () -> Unit = { },
) {
    var selectedState by remember { mutableStateOf(selected) }
    // set again in case relayout
    selectedState = selected

    fun setSelected(newSelected : Set<Int>) {
        selectedState = newSelected
        onSelectionChange(selectedState)
    }

    OKCancelDialog(
        title = title,
        summary = summary,
        onCancel = onDismissRequest,
        onOK = { onSelect(selectedState) }
    ) {
        body()

        allOrNoneLabel?.let { allOrNoneLabel ->
            val allOrNoneSet = if (selectedState.size > 0)
                setOf()
            else
                (0 until numItems).toSet()

            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { selectedState = allOrNoneSet },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checked = if (selectedState.size == 0)
                    ToggleableState.Off
                else if (selectedState.size >= numItems)
                    ToggleableState.On
                else
                    ToggleableState.Indeterminate

                TriStateCheckbox(
                    state = checked,
                    onClick = { setSelected(allOrNoneSet) },
                )
                Text(
                    text = stringResource(allOrNoneLabel),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            HorizontalDivider()
        }

        BoxWithConstraints() {
            LazyColumn(
                modifier = Modifier.heightIn(0.dp, maxHeight * 0.7f),
            ) {
                items(numItems) { index ->
                    val alpha = if (getEnabled(index))
                        ENABLED_ALPHA
                    else
                        DISABLED_ALPHA

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .alpha(alpha)
                            .clickable {
                                setSelected(
                                    setMembership(
                                        index,
                                        selectedState,
                                        !selectedState.contains(index),
                                    )
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedState.contains(index),
                            onCheckedChange = { checked ->
                                setSelected(
                                    setMembership(index, selectedState, checked)
                                )
                            },
                        )
                        getItem(index)
                    }
                }
            }
        }
    }
}

/**
 * New set with item added/removed to elements by add param
 */
private fun <T> setMembership(
    item : T, elements : Set<T>, add : Boolean
) : Set<T> = if (add)
        elements + item
    else
        elements - item

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
    PlainDialog(
        title = stringResource(title),
        summary = summary,
        onClose = onClose,
        body = body,
    )
}

@Composable
private fun PlainDialog(
    title : String,
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
                    text = title,
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

