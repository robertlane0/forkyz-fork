
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum DayNightMode {
    DAY,
    NIGHT,
    SYSTEM;

    public String getSettingsValue() { return name(); }

    public static DayNightMode getFromSettingsValue(String value) {
        for (DayNightMode m : DayNightMode.values()) {
            if (Objects.equals(value, m.getSettingsValue()))
                return m;
        }
        return DayNightMode.DAY;
    }
}
