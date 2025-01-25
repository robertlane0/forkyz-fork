
package app.crossword.yourealwaysbe.forkyz.settings;

public enum ExternalDictionarySetting implements EnumSetting {
    NONE, QUICK, AARD2, FREE;

    @Override
    public String getSettingsValue() { return name(); }
}
