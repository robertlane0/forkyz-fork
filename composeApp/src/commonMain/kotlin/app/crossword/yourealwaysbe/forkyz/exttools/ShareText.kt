
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_format_short_no_num_no_dir_with_count
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_blank_box
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_response_text
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_text
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_message_no_date
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_message_with_date
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_details_author_no_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_details_author_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_details_no_author_no_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_details_no_author_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_your_time
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unknown_hint
import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri

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
            val response = board.currentWordBoxes
            val shareMessage = getShareClueMessage(
                utils,
                settings,
                board.puzzle,
                clue,
                response,
                withResponse,
            )
            return ShareTextData(
                utils.getString(Res.string.share_clue_title),
                shareMessage,
            )
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
                utils.getQuantityString(
                    Res.plurals.share_message_with_date,
                    cheatedBoxes,
                    source,
                    date.day,
                    utils.getString(TimeUtils.getMonthNameID(date)),
                    date.year,
                    cheatedBoxes,
                    completionTime,
                )
            } else {
                utils.getQuantityString(
                    Res.plurals.share_message_no_date,
                    cheatedBoxes,
                    source,
                    cheatedBoxes,
                    completionTime,
                )
            }

            return ShareTextData(
                utils.getString(Res.string.share_your_time),
                message,
            )
        }

        private suspend fun getShareClueMessage(
            utils : NativeBackendUtils,
            settings : ForkyzSettings,
            puz : Puzzle,
            clue : Clue,
            response : List<Box>,
            withResponse : Boolean,
        ) : String {
            val clueText = getShareClueText(utils, settings, clue)
            val puzzleDetails = getSharePuzzleDetails(utils, puz)

            if (withResponse) {
                val responseText = getShareResponseText(utils, response)
                return utils.getString(
                    Res.string.share_clue_response_text,
                    clueText,
                    responseText,
                    puzzleDetails,
                )
            } else {
                return utils.getString(
                    Res.string.share_clue_text,
                    clueText,
                    puzzleDetails,
                )
            }
        }

        private suspend fun getShareResponseText(
            utils : NativeBackendUtils,
            boxes : List<Box>
        ) : String {
            val responseText = StringBuilder()
            for (box in boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        utils.getString(Res.string.share_clue_blank_box),
                    )
                } else {
                    responseText.append(box.response)
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
                return utils.getString(Res.string.unknown_hint)
            } else {
                val wordLen = if (clue.hasZone())
                    clue.zone.size()
                else
                    -1

                if (showCount && wordLen >= 0) {
                    return utils.getString(
                        Res.string
                            .clue_format_short_no_num_no_dir_with_count,
                        clue.hint,
                        wordLen,
                    )
                } else {
                    return clue.hint
                }
            }
        }

        private suspend fun getSharePuzzleDetails(
            utils : NativeBackendUtils,
            puz : Puzzle,
        ) : String {
            var source = puz.source
            var title = puz.title
            var author = puz.author

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

            val shareUrl = puz.shareUrl;

            if (shareUrl == null || shareUrl.isEmpty()) {
                return if (author != null) {
                    utils.getString(
                        Res.string.share_puzzle_details_author_no_url,
                        source,
                        title,
                        author,
                    )
                } else {
                    utils.getString(
                        Res.string.share_puzzle_details_no_author_no_url,
                        source,
                        title,
                    )
                }
            } else {
                return if (author != null) {
                    utils.getString(
                        Res.string.share_puzzle_details_author_url,
                        source,
                        title,
                        author,
                        shareUrl,
                    )
                } else {
                    utils.getString(
                        Res.string.share_puzzle_details_no_author_url,
                        source,
                        title,
                        shareUrl,
                    )
                }
            }
        }
    }

    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

@Throws(AppNotFoundException::class, CancellationException::class)
expect suspend fun ExternalToolLauncher.visit(data : ShareTextData)

