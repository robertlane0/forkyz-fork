
package app.crossword.yourealwaysbe.settings;

public enum StorageLocation {
    INTERNAL("App Internal Storage"),
    EXTERNAL_SAF("External directory"),
    EXTERNAL_LEGACY("External SD crosswords directory");

    private String settingsValue;

    StorageLocation(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static StorageLocation fromSettingsValue(String value) {
        if (INTERNAL.getSettingsValue().equals(value))
            return INTERNAL;
        else if (EXTERNAL_SAF.getSettingsValue().equals(value))
            return EXTERNAL_SAF;
        else if (EXTERNAL_LEGACY.getSettingsValue().equals(value))
            return EXTERNAL_LEGACY;
        else
            throw new IllegalArgumentException(
                "Unrecognised StorageLocation: " + value
            );
    }
}


