
package app.crossword.yourealwaysbe.forkyz.settings;

public enum CycleUnfilledMode implements EnumSetting {
    NEVER("NEVER"),
    FORWARDS("FORWARDS"),
    ALWAYS("ALWAYS");

    private String settingsValue;

    CycleUnfilledMode(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
