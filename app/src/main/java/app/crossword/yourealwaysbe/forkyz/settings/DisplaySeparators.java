
package app.crossword.yourealwaysbe.forkyz.settings;

public enum DisplaySeparators implements EnumSetting {
    NEVER,
    SELECTED,
    ALWAYS;

    @Override
    public String getSettingsValue() { return name(); }
}
