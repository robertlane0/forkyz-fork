
package app.crossword.yourealwaysbe.forkyz.view

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.completed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.completed_in
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.done
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.elapsed_time
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hinted_boxes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.introduction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.num_hinted_boxes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_finished_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_info
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.total_boxes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.total_clues
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.view.TwoButtonDialog

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
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val showRating by remember { derivedStateOf { state.showRating } }
            val rating by remember { derivedStateOf { state.rating } }
            PuzzleRating(
                showRating,
                rating,
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
    TwoButtonDialog(
        title = Res.string.puzzle_finished_title,
        positiveText = Res.string.done,
        onPositive = onDone,
        negativeText = Res.string.share,
        onNegative = onShare,
        onCancel = onDone,
    ) {
        Column() {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val showRating by remember {
                derivedStateOf { state?.showRating ?: true }
            }
            val rating by remember { derivedStateOf { state?.rating ?: 0 } }
            PuzzleRating(
                showRating,
                rating,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val title by remember { derivedStateOf { state.title } }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title ?: stringResource(Res.string.puzzle_info),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        if (state.author.length > 0) {
            val author by remember { derivedStateOf { state.author } }
            Text(
                text = remember(author) { htmlToAnnotatedString(author) },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (state.copyright.length > 0) {
            val copyright by remember { derivedStateOf { state.copyright } }
            Text(
                text = remember(copyright) { htmlToAnnotatedString(copyright) },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val elapsed by remember { derivedStateOf { state.elapsed } }
        Text(
            text = stringResource(
                Res.string.elapsed_time,
                TimeUtils.convertMillisecondsToTime(elapsed),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        val progress by remember { derivedStateOf { state.progress / 100F } }
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            progress = { progress },
        )
        Text(
            text = state.filename,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PuzzleInfo(
    viewModel : PuzzleInfoDialogViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val introduction by remember { derivedStateOf { state.introduction } }
    introduction?.let { introduction ->
        InfoSection(Res.string.introduction, introduction)
    }
    val notes by remember { derivedStateOf { state.notes } }
    notes?.let { notes ->
        InfoSection(Res.string.notes, notes)
    }
}

@Composable
private fun InfoSection(
    titleID : StringResource,
    contentHTML: String,
) {
    if (contentHTML.length > 0) {
        InfoSection(
            titleID = titleID,
            content = {
                Text(
                    text = remember(contentHTML) {
                        htmlToAnnotatedString(contentHTML)
                    },
                )
            },
        )
    }
}

@Composable
private fun InfoSection(
    titleID : StringResource?,
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

@Composable
private fun FinishedInfo(
    viewModel : PuzzleFinishedDialogViewModel,
    showTitle : Boolean = true,
) {
    val finishedState by viewModel.uiState.collectAsStateWithLifecycle()

    finishedState?.let { finishedState ->
        InfoSection(titleID = if (showTitle) Res.string.completed else null) {
            Column(
                modifier = Modifier.padding(vertical = COMPLETION_TABLE_PADDING),
            ) {
                CompletionDataRow(
                    Res.string.completed_in,
                    TimeUtils.convertMillisecondsToTime(
                        finishedState.completionTime
                    ),
                )
                CompletionDataRow(
                    Res.string.total_clues,
                    finishedState.numberOfClues.toString(),
                )
                CompletionDataRow(
                    Res.string.total_boxes,
                    finishedState.numberOfBoxes.toString(),
                )

                val cheatedString = stringResource(
                    Res.string.num_hinted_boxes,
                    finishedState.numberOfCheatedBoxes,
                    finishedState.percentCheated,
                )
                CompletionDataRow(
                    Res.string.hinted_boxes,
                    cheatedString,
                )
            }
            finishedState.completionMessage?.let { completionMessage ->
                Text(
                    text = remember(completionMessage) {
                        htmlToAnnotatedString(completionMessage)
                    },
                )
            }
        }
    }
}

@Composable
private fun CompletionDataRow(
    title : StringResource,
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
                stepSize = 1.0F,
                onSet = { rating -> onSet(rating.toInt()) },
                rating = rating.toFloat(),
            )
        }
    }
}

