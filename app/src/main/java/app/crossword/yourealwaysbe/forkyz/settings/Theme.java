
package app.crossword.yourealwaysbe.forkyz.settings;

public enum Theme implements EnumSetting {
    STANDARD("S"),
    DYNAMIC("D"),
    LEGACY_LIKE("L");

    private String settingsValue;

    Theme(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}


