
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum CycleUnfilledMode {
    NEVER("NEVER"),
    FORWARDS("FORWARDS"),
    ALWAYS("ALWAYS");

    private String settingsValue;

    CycleUnfilledMode(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static CycleUnfilledMode getFromSettingsValue(String value) {
        for (CycleUnfilledMode m : CycleUnfilledMode.values()) {
            if (Objects.equals(value, m.getSettingsValue()))
                return m;
        }
        return CycleUnfilledMode.NEVER;
    }
}
