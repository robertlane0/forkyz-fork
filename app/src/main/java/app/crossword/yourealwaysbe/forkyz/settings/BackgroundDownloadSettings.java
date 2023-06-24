
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Set;

// TODO: convert to record class when possible
public class BackgroundDownloadSettings {
    private boolean requireUnmetered;
    private boolean allowRoaming;
    private boolean requireCharging;
    private boolean hourly;
    private Set<String> days;
    private int daysTime;

    public BackgroundDownloadSettings(
        boolean requireUnmetered,
        boolean allowRoaming,
        boolean requireCharging,
        boolean hourly,
        Set<String> days,
        int daysTime
    ) {
        this.requireUnmetered = requireUnmetered;
        this.allowRoaming = allowRoaming;
        this.requireCharging = requireCharging;
        this.hourly = hourly;
        this.days = days;
        this.daysTime = daysTime;
    }

    public boolean getRequireUnmetered() { return requireUnmetered; }
    public boolean getAllowRoaming() { return allowRoaming; }
    public boolean getRequireCharging() { return requireCharging; }
    public boolean getHourly() { return hourly; }
    public Set<String> getDays() { return days; }
    public int getDaysTime() { return daysTime; }
}
