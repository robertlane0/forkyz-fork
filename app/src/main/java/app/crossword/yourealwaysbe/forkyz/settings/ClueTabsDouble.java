
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum ClueTabsDouble {
    NEVER("NEVER"),
    LANDSCAPE("LANDSCAPE"),
    WIDE("WIDE");

    private String settingsValue;

    ClueTabsDouble(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static ClueTabsDouble getFromSettingsValue(String value) {
        for (ClueTabsDouble ctd : values()) {
            if (Objects.equals(value, ctd.getSettingsValue()))
                return ctd;
        }
        return ClueTabsDouble.NEVER;
    }
}
