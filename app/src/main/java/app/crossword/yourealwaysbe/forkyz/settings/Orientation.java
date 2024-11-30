
package app.crossword.yourealwaysbe.forkyz.settings;

public enum Orientation implements EnumSetting {
    PORTRAIT("PORT"),
    LANDSCAPE("LAND"),
    UNLOCKED("UNLOCKED");

    private String settingsValue;

    Orientation(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
