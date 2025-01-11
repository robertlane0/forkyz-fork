
package app.crossword.yourealwaysbe.forkyz.settings;

public enum FitToScreenMode implements EnumSetting {
    NEVER,
    START,
    LOCKED;

    @Override
    public String getSettingsValue() { return name(); }
}
