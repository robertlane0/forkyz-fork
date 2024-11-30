
package app.crossword.yourealwaysbe.forkyz.settings;

public enum DayNightMode implements EnumSetting {
    DAY,
    NIGHT,
    SYSTEM;

    @Override
    public String getSettingsValue() { return name(); }
}
