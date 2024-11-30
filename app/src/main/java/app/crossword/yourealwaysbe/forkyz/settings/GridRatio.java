
package app.crossword.yourealwaysbe.forkyz.settings;

public enum GridRatio implements EnumSetting {
    ONE_TO_ONE("0"),
    THIRTY_PCNT("1"),
    FORTY_PCNT("2"),
    FIFTY_PCNT("3"),
    SIXTY_PCNT("4"),
    PUZZLE_SHAPE("5");

    private String settingsValue;

    GridRatio(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }
}
