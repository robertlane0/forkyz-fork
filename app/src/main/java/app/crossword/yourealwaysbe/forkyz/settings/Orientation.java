
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum Orientation {
    PORTRAIT("PORT"),
    LANDSCAPE("LAND"),
    UNLOCKED("UNLOCKED");

    private String settingsValue;

    Orientation(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static Orientation getFromSettingsValue(String value) {
        for (Orientation o : Orientation.values()) {
            if (Objects.equals(value, o.getSettingsValue()))
                return o;
        }
        return Orientation.UNLOCKED;
    }
}
