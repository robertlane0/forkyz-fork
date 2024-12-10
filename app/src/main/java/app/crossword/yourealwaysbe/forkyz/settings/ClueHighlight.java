
package app.crossword.yourealwaysbe.forkyz.settings;

public enum ClueHighlight implements EnumSetting {
    NONE,
    RADIO_BUTTON,
    BACKGROUND,
    BOTH;

    @Override
    public String getSettingsValue() { return name(); }

    public boolean hasRadio() {
        return this == RADIO_BUTTON || this == BOTH;
    }

    public boolean hasBackground() {
        return this == BACKGROUND || this == BOTH;
    }
}
