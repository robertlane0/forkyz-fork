
package app.crossword.yourealwaysbe.forkyz.net

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

import app.crossword.yourealwaysbe.puz.io.SmartGamesJSONIO
import app.crossword.yourealwaysbe.puz.Puzzle

class ElPaisExpertoDownloader(
    internalName : String,
    name : String,
) : AbstractDateDownloader(
    internalName,
    name,
    AbstractDateDownloader.DAYS_DAILY,
    Duration.ofHours(-1), // midnight, guessed
    "https://elpais.com/suscripciones",
    SmartGamesJSONIO(),
    "'https://backend.smartgames.media/api/game/crossword"
        + "/category/experto/'yyyy'-'MM'-'dd",
    "'https://elpais.com/juegos/crucigramas/experto/'yyyy'-'MM'-'dd",
    ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
        .toLocalDate()
        .minusDays(7),
) {
    override protected fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val fullHeaders = headers?.toMutableMap() ?: mutableMapOf()
        fullHeaders.putIfAbsent("Origin", "https://elpais.com")
        return super.download(date, fullHeaders)?.also {
            it.source = name
        }
    }
}
