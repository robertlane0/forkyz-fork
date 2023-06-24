
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

public enum BrowseSwipeAction {
    ARCHIVE("ARCHIVE"),
    DELETE("DELETE");

    private String settingsValue;

    BrowseSwipeAction(String settingsValue) {
        this.settingsValue = settingsValue;
    }

    public String getSettingsValue() { return settingsValue; }

    public static BrowseSwipeAction getFromSettingsValue(String value) {
        for (BrowseSwipeAction a : BrowseSwipeAction.values()) {
            if (Objects.equals(value, a.getSettingsValue()))
                return a;
        }
        return BrowseSwipeAction.DELETE;
    }
}
