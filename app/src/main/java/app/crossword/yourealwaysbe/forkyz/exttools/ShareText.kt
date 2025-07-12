
package app.crossword.yourealwaysbe.forkyz.exttools

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.function.Consumer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Puzzle

open class ShareTextData(
    val title : String,
    val message : String
) : ExternalToolData() {
    companion object {
        fun buildShareClue(
            context : Context,
            settings : ForkyzSettings,
            board : Playboard,
            clue : Clue,
            withResponse : Boolean,
            cb : Consumer<ShareTextData>,
        ) {
            val puz = board.getPuzzle()
            if (puz != null) {
                val response = board.getCurrentWordBoxes()
                getShareClueMessage(
                    context,
                    settings,
                    puz,
                    clue,
                    response,
                    withResponse,
                ) { shareMessage : String ->
                     cb.accept(
                         ShareTextData(
                            context.getString(R.string.share_clue_title),
                            shareMessage,
                        )
                    )
                }
            }
        }

        fun buildShareCompletion(
            context : Context,
            puz : Puzzle,
        ) : ShareTextData {
            val source = puz.source ?: puz.title ?: ""
            val cheatedBoxes = puz.numberOfCheatedBoxes

            val date = puz.date
            val message = if (date != null) {
                val dateFormat
                    = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                context.resources.getQuantityString(
                    R.plurals.share_message_with_date,
                    cheatedBoxes,
                    source,
                    dateFormat.format(date),
                    cheatedBoxes
                )
            } else {
                context.resources.getQuantityString(
                    R.plurals.share_message_no_date,
                    cheatedBoxes,
                    source,
                    cheatedBoxes
                )
            }

            return ShareTextData(
                context.getString(R.string.share_your_time),
                message,
            )
        }

        private fun getShareClueMessage(
            context : Context,
            settings : ForkyzSettings,
            puz : Puzzle,
            clue : Clue,
            response : Array<Box>,
            withResponse : Boolean,
            cb : (String) -> Unit,
        ) {
            getShareClueText(context, settings, clue) { clueText ->
                val responseText
                    = if (withResponse) getShareResponseText(context, response)
                    else null

                val puzzleDetails = getSharePuzzleDetails(context, puz)

                if (withResponse) {
                    cb(
                        context.getString(
                            R.string.share_clue_response_text,
                            clueText,
                            responseText,
                            puzzleDetails,
                        )
                    )
                } else {
                    cb(
                        context.getString(
                            R.string.share_clue_text,
                            clueText,
                            puzzleDetails,
                        )
                    )
                }
            }
        }

        private fun getShareResponseText(
            context : Context,
            boxes : Array<Box>
        ) : String {
            val responseText = StringBuilder()
            for (box in boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        context.getString(R.string.share_clue_blank_box),
                    )
                } else {
                    responseText.append(box.getResponse())
                }
            }
            return responseText.toString()
        }

        private fun getShareClueText(
            context : Context,
            settings : ForkyzSettings,
            clue : Clue?,
            cb : (String) -> Unit,
        ) {
            settings.getPlayShowCount() { showCount ->
                if (clue == null) {
                    cb(context.getString(R.string.unknown_hint))
                } else {
                    val wordLen = if (clue.hasZone()) clue.getZone().size() else -1

                    if (showCount && wordLen >= 0) {
                        cb(
                            context.getString(
                                R.string.clue_format_short_no_num_no_dir_with_count,
                                clue.getHint(),
                                wordLen,
                            )
                        )
                    } else {
                        cb(clue.getHint())
                    }
                }
            }
        }

        private fun getSharePuzzleDetails(
            context : Context,
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
                    context.getString(
                        R.string.share_puzzle_details_author_no_url,
                        source,
                        title,
                        author,
                    )
                } else {
                    context.getString(
                        R.string.share_puzzle_details_no_author_no_url,
                        source,
                        title,
                    )
                }
            } else {
                return if (author != null) {
                    context.getString(
                        R.string.share_puzzle_details_author_url,
                        source,
                        title,
                        author,
                        shareUrl,
                    )
                } else {
                    context.getString(
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
