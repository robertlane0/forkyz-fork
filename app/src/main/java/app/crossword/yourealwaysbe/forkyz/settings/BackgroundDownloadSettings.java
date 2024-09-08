
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Set;

public record BackgroundDownloadSettings(
    boolean requireUnmetered,
    boolean allowRoaming,
    boolean requireCharging,
    boolean hourly,
    Set<String> days,
    int daysTime
) { }
