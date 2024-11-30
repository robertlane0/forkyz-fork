
package app.crossword.yourealwaysbe.forkyz.settings;

public enum BrowseSwipeAction implements EnumSetting {
    ARCHIVE("ARCHIVE"),
    DELETE("DELETE");

    private String settingsValue;

    BrowseSwipeAction(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
