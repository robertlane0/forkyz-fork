
package app.crossword.yourealwaysbe.settings;

import java.util.Objects;

public enum KeyboardLayout {
    QWERTY("0"),
    QWERTZ("1"),
    DVORAK("2"),
    COLEMAK("3");

    private String settingsValue;

    KeyboardLayout(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static KeyboardLayout getFromSettingsValue(String value) {
        for (KeyboardLayout l : KeyboardLayout.values()) {
            if (Objects.equals(value, l.getSettingsValue()))
                return l;
        }
        return KeyboardLayout.QWERTY;
    }
}
