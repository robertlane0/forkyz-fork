
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum GridRatio {
    ONE_TO_ONE("0"),
    THIRTY_PCNT("1"),
    FORTY_PCNT("2"),
    FIFTY_PCNT("3"),
    SIXTY_PCNT("4");

    private String settingsValue;

    GridRatio(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static GridRatio getFromSettingsValue(String value) {
        for (GridRatio l : GridRatio.values()) {
            if (Objects.equals(value, l.getSettingsValue()))
                return l;
        }
        return GridRatio.ONE_TO_ONE;
    }
}
