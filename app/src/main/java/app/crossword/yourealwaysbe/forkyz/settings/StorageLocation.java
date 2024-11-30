
package app.crossword.yourealwaysbe.forkyz.settings;

public enum StorageLocation implements EnumSetting {
    INTERNAL("App Internal Storage"),
    EXTERNAL_SAF("External directory"),
    EXTERNAL_LEGACY("External SD crosswords directory");

    private String settingsValue;

    StorageLocation(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }

    public static StorageLocation fromSettingsValue(String value) {
        return EnumSetting.getFromSettingsValueNonNull(
            value, StorageLocation.class, StorageLocation.INTERNAL
        );
    }
}


