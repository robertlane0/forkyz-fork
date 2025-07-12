
package app.crossword.yourealwaysbe.forkyz.view

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.view.TwoButtonDialog

private val SECONDS = 1000L
private val MINUTES = SECONDS * 60
private val HOURS = MINUTES * 60

private val COMPLETION_TABLE_PADDING = 16.dp

/**
 * Dialog showing puzzle details
 *
 * Shows completion details if puzzle finished.
 */
@Composable
fun PuzzleInfoDialog(
    viewModel : PuzzleInfoDialogViewModel,
    onClose : () -> Unit = { },
) {
    val state by viewModel.uiState.observeAsState()

    PlainDialog(
        onClose = onClose,
        usePlatformDefaultWidth = false,
    ) {
        Column() {
            InfoHeadingData(viewModel)
            BoxWithConstraints() {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(0.dp, maxHeight * 0.6f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PuzzleInfo(viewModel)
                    FinishedInfo(viewModel.finishedViewModel)
                }
            }
            PuzzleRating(
                state?.showRating ?: true,
                state?.rating ?: 0,
                onSet = viewModel::setRating,
            )
        }
    }
}

@Composable
fun PuzzleFinishedDialog(
    viewModel : PuzzleFinishedDialogViewModel,
    onDone : () -> Unit = { },
    onShare : () -> Unit = { },
) {
    val state by viewModel.uiState.observeAsState()

    TwoButtonDialog(
        title = R.string.puzzle_finished_title,
        positiveText = R.string.done,
        onPositive = onDone,
        negativeText = R.string.share,
        onNegative = onShare,
    ) {
        Column() {
            PuzzleRating(
                state?.showRating ?: true,
                state?.rating ?: 0,
                onSet = viewModel::setRating,
            )
            BoxWithConstraints() {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(0.dp, maxHeight * 0.6f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FinishedInfo(
                        viewModel = viewModel,
                        showTitle = false,
                    )
                }
            }
        }
    }

}

@Composable
private fun InfoHeadingData(
    viewModel : PuzzleInfoDialogViewModel,
) {
    val state by viewModel.uiState.observeAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state?.title ?: stringResource(R.string.puzzle_info),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        state?.author?.let { author ->
            if (author.length > 0) {
                Text(
                    text = AnnotatedString.fromHtml(author),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        state?.copyright?.let { copyright ->
            if (copyright.length > 0) {
                Text(
                    text = AnnotatedString.fromHtml(state?.copyright ?: ""),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            text = stringResource(
                R.string.elapsed_time,
                getElapsedString(state?.elapsed ?: 0L)
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            progress = { (state?.progress ?: 0) / 100F },
        )
        Text(
            text = state?.filename ?: "",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PuzzleInfo(
    viewModel : PuzzleInfoDialogViewModel,
) {
    val state by viewModel.uiState.observeAsState()

    state?.introduction?.let { introduction ->
        InfoSection(R.string.introduction, introduction)
    }
    state?.notes?.let { notes ->
        InfoSection(R.string.notes, notes)
    }
}

@Composable
private fun InfoSection(
    titleID : Int,
    contentHTML: String,
) {
    if (contentHTML.length > 0) {
        InfoSection(
            titleID = titleID,
            content = { Text(text = AnnotatedString.fromHtml(contentHTML)) },
        )
    }
}

@Composable
private fun InfoSection(
    titleID : Int?,
    content: @Composable () -> Unit,
) {
    titleID?.let { titleID ->
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(titleID),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    content()
}

//        // with apologies to the Material guidelines..
//        String shareMessage = getShareMessage(puz)
//        builder.setNegativeButton(
//            R.string.share,
//            new DialogInterface.OnClickListener(){
//                public void onClick(DialogInterface dialog, int which) {
//                    Intent sendIntent = new Intent(Intent.ACTION_SEND)
//                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
//                    sendIntent.setType("text/plain")
//                    activity.startActivity(Intent.createChooser(
//                        sendIntent, activity.getString(R.string.share_your_time)
//                    ))
//                }
//            }
//        )

@Composable
private fun FinishedInfo(
    viewModel : PuzzleFinishedDialogViewModel,
    showTitle : Boolean = true,
) {
    val finishedState by viewModel.uiState.observeAsState()

    finishedState?.let { finishedState ->
        InfoSection(titleID = if (showTitle) R.string.completed else null) {
            Column(
                modifier = Modifier.padding(vertical = COMPLETION_TABLE_PADDING),
            ) {
                CompletionDataRow(
                    R.string.completed_in,
                    getElapsedString(finishedState.completionTime),
                )
                CompletionDataRow(
                    R.string.total_clues,
                    finishedState.numberOfClues.toString(),
                )
                CompletionDataRow(
                    R.string.total_boxes,
                    finishedState.numberOfBoxes.toString(),
                )

                val cheatedString = stringResource(
                    R.string.num_hinted_boxes,
                    finishedState.numberOfCheatedBoxes,
                    finishedState.percentCheated,
                )
                CompletionDataRow(
                    R.string.hinted_boxes,
                    cheatedString,
                )
            }
            finishedState.completionMessage?.let { completionMessage ->
                Text(text = AnnotatedString.fromHtml(completionMessage))
            }
        }
    }
}

@Composable
private fun CompletionDataRow(
    title : Int,
    value : String,
) {
    Row(
        modifier = Modifier.padding(horizontal = COMPLETION_TABLE_PADDING)
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.weight(1.0F)
                .padding(end=8.dp),
            text = stringResource(title),
            textAlign = TextAlign.End,
        )
        Text(
            modifier = Modifier.weight(1.0F)
                .padding(start=8.dp),
            text = value
        )
    }
}

/**
 * Convert long time duration into readable for humans
 */
@Composable
private fun getElapsedString(elapsed : Long) : String {
    var remainingTime = elapsed
    val hours = remainingTime / HOURS
    remainingTime = remainingTime % HOURS

    val minutes = remainingTime / MINUTES
    remainingTime = remainingTime % MINUTES

    val seconds = remainingTime / SECONDS

    return if (hours > 0) {
        stringResource(
            R.string.completed_time_format_with_hours,
            hours,
            minutes,
            seconds,
        )
    } else {
        stringResource(
            R.string.completed_time_format_no_hours,
            minutes,
            seconds,
        )
    }
}

@Composable
private fun PuzzleRating(
    showRating : Boolean,
    rating : Int,
    onSet : (Int) -> Unit,
) {
    if (showRating) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            RatingBar(
                modifier = Modifier.padding(top=10.dp),
                numStars = integerResource(R.integer.max_rating),
                stepSize = 1.0F,
                onSet = { rating -> onSet(rating.toInt()) },
                rating = rating.toFloat(),
            )
        }
    }
}
