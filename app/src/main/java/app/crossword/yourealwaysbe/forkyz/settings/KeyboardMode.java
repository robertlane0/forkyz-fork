
package app.crossword.yourealwaysbe.forkyz.settings;

public enum KeyboardMode implements EnumSetting {
    ALWAYS_SHOW("Always show"),
    HIDE_MANUAL("Hide keyboard manually"),
    SHOW_SPARINGLY("Show sparingly"),
    NEVER_SHOW("Never show");

    private String settingsValue;

    KeyboardMode(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }

    public static KeyboardMode getFromSettingsValue(String value) {
        return EnumSetting.getFromSettingsValueNonNull(
            value, KeyboardMode.class, KeyboardMode.HIDE_MANUAL
        );
    }
}
