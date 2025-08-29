package app.crossword.yourealwaysbe.forkyz.util

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Locale

class TimeUtils {
    companion object {
        fun convertMillisecondsToTime(milliseconds: Long): String {
            val duration: Duration = Duration.of(milliseconds, ChronoUnit.MILLIS)
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()
            if (hours == 0L) {
                return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}