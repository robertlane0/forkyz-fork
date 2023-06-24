
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum KeyboardMode {
    ALWAYS_SHOW("Always show"),
    HIDE_MANUAL("Hide keyboard manually"),
    SHOW_SPARINGLY("Show sparingly"),
    NEVER_SHOW("Never show");

    private String settingsValue;

    KeyboardMode(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static KeyboardMode getFromSettingsValue(String value) {
        for (KeyboardMode m : KeyboardMode.values()) {
            if (Objects.equals(value, m.getSettingsValue()))
                return m;
        }
        return KeyboardMode.HIDE_MANUAL;
    }
}
