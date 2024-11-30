
package app.crossword.yourealwaysbe.forkyz.settings;

public enum ClueTabsDouble implements EnumSetting {
    NEVER("NEVER"),
    LANDSCAPE("LANDSCAPE"),
    WIDE("WIDE"),
    ALWAYS("ALWAYS");

    private String settingsValue;

    ClueTabsDouble(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
