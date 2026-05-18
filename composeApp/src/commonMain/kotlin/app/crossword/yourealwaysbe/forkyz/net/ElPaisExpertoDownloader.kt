
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.SmartGamesJSONIO
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

class ElPaisExpertoDownloader(
    downloaderID : DownloaderID,
    name : String,
) : AbstractDateDownloader(
    downloaderID,
    name,
    AbstractDateDownloader.DAYS_DAILY,
    -1.hours, // midnight, guessed
    "https://elpais.com/suscripciones",
    SmartGamesJSONIO(),
    "'https://backend.smartgames.media/api/game/crossword"
        + "/category/experto/'yyyy'-'MM'-'dd",
    "'https://elpais.com/juegos/crucigramas/experto/'yyyy'-'MM'-'dd",
    TimeUtils.offsetTodayUTC(-7.days),
) {
    override protected suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val fullHeaders = headers?.toMutableMap() ?: mutableMapOf()
        if (!fullHeaders.contains("Origin"))
            fullHeaders.put("Origin", "https://elpais.com")
        return super.download(date, fullHeaders)?.also {
            it.source = name
        }
    }
}
