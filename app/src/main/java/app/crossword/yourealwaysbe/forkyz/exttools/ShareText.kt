
package app.crossword.yourealwaysbe.forkyz.exttools

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.function.Consumer
import kotlinx.coroutines.flow.first

import android.net.Uri
import androidx.core.app.ShareCompat

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Puzzle

open class ShareTextData(
    val title : String,
    val message : String
) : ExternalToolData() {
    companion object {
        suspend fun buildShareClue(
            utils : NativeBackendUtils,
            settings : ForkyzSettings,
            board : Playboard,
            clue : Clue,
            withResponse : Boolean,
        ) : ShareTextData? {
            return board.getPuzzle()?.let { puz ->
                val response = board.getCurrentWordBoxes()
                val shareMessage = getShareClueMessage(
                    utils,
                    settings,
                    puz,
                    clue,
                    response,
                    withResponse,
                )
                return ShareTextData(
                    utils.getString(R.string.share_clue_title),
                    shareMessage,
                )
            }
        }

        suspend fun buildShareCompletion(
            utils : NativeBackendUtils,
            puz : Puzzle,
        ) : ShareTextData {
            val source = puz.source ?: puz.title ?: ""
            val cheatedBoxes = puz.numberOfCheatedBoxes

            val date = puz.date
            val completionTime = TimeUtils.convertMillisecondsToTime(puz.time)
            val message = if (date != null) {
                val dateFormat
                    = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                utils.getQuantityString(
                    R.plurals.share_message_with_date,
                    cheatedBoxes,
                    source,
                    dateFormat.format(date),
                    cheatedBoxes,
                    completionTime,
                )
            } else {
                utils.getQuantityString(
                    R.plurals.share_message_no_date,
                    cheatedBoxes,
                    source,
                    cheatedBoxes,
                    completionTime,
                )
            }

            return ShareTextData(
                utils.getString(R.string.share_your_time),
                message,
            )
        }

        private suspend fun getShareClueMessage(
            utils : NativeBackendUtils,
            settings : ForkyzSettings,
            puz : Puzzle,
            clue : Clue,
            response : Array<Box>,
            withResponse : Boolean,
        ) : String {
            val clueText = getShareClueText(utils, settings, clue)
            val puzzleDetails = getSharePuzzleDetails(utils, puz)

            if (withResponse) {
                val responseText = getShareResponseText(utils, response)
                return utils.getString(
                    R.string.share_clue_response_text,
                    clueText,
                    responseText,
                    puzzleDetails,
                )
            } else {
                return utils.getString(
                    R.string.share_clue_text,
                    clueText,
                    puzzleDetails,
                )
            }
        }

        private suspend fun getShareResponseText(
            utils : NativeBackendUtils,
            boxes : Array<Box>
        ) : String {
            val responseText = StringBuilder()
            for (box in boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        utils.getString(R.string.share_clue_blank_box),
                    )
                } else {
                    responseText.append(box.getResponse())
                }
            }
            return responseText.toString()
        }

        private suspend fun getShareClueText(
            utils : NativeBackendUtils,
            settings : ForkyzSettings,
            clue : Clue?,
        ) : String {
            val showCount = settings.livePlayShowCount.first()
            if (clue == null) {
                return utils.getString(R.string.unknown_hint)
            } else {
                val wordLen = if (clue.hasZone())
                    clue.getZone().size()
                else
                    -1

                if (showCount && wordLen >= 0) {
                    return utils.getString(
                        R.string
                            .clue_format_short_no_num_no_dir_with_count,
                        clue.getHint(),
                        wordLen,
                    )
                } else {
                    return clue.getHint()
                }
            }
        }

        private suspend fun getSharePuzzleDetails(
            utils : NativeBackendUtils,
            puz : Puzzle,
        ) : String {
            var source = puz.getSource()
            var title = puz.getTitle()
            var author = puz.getAuthor()

            if (source == null)
                source = ""
            if (title == null)
                title = ""
            if (author != null) {
                // add author if not already in title or caption
                // case insensitive trick:
                // https://www.baeldung.com/java-case-insensitive-string-matching
                val regex = ("(?i).*" + Regex.escape(author) + ".*").toRegex()
                val removeAuthor = author.isEmpty()
                    || title.matches(regex)
                    || source.matches(regex)

                if (removeAuthor)
                    author = null
            }

            val shareUrl = puz.getShareUrl();

            if (shareUrl == null || shareUrl.isEmpty()) {
                return if (author != null) {
                    utils.getString(
                        R.string.share_puzzle_details_author_no_url,
                        source,
                        title,
                        author,
                    )
                } else {
                    utils.getString(
                        R.string.share_puzzle_details_no_author_no_url,
                        source,
                        title,
                    )
                }
            } else {
                return if (author != null) {
                    utils.getString(
                        R.string.share_puzzle_details_author_url,
                        source,
                        title,
                        author,
                        shareUrl,
                    )
                } else {
                    utils.getString(
                        R.string.share_puzzle_details_no_author_url,
                        source,
                        title,
                        shareUrl,
                    )
                }
            }
        }
    }

    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

fun ExternalToolLauncher.visit(data : ShareTextData) {
    // ShareCompat from
    // https://stackoverflow.com/a/39619468/6882587
    // assume works better than the out-of-date android docs!
    val shareIntent = ShareCompat.IntentBuilder(this.activity)
        .setText(data.message)
        .setType("text/plain")
        .setChooserTitle(data.title)
        .createChooserIntent();
    this.activity.startActivity(shareIntent);
}
