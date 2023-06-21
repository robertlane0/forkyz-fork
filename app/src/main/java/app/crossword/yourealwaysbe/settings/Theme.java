
package app.crossword.yourealwaysbe.settings;

import java.util.Objects;

public enum Theme {
    STANDARD("S"),
    DYNAMIC("D"),
    LEGACY_LIKE("L");

    private String settingsValue;

    Theme(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() {
        return settingsValue;
    }

    public static Theme getFromSettingsValue(String value) {
        for (Theme t : Theme.values()) {
            if (Objects.equals(value, t.getSettingsValue()))
                return t;
        }
        return Theme.STANDARD;
    }
}


