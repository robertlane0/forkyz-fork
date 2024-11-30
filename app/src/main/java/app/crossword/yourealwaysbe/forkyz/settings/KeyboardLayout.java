
package app.crossword.yourealwaysbe.forkyz.settings;

public enum KeyboardLayout implements EnumSetting {
    QWERTY("0"),
    QWERTZ("1"),
    DVORAK("2"),
    COLEMAK("3");

    private String settingsValue;

    KeyboardLayout(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
