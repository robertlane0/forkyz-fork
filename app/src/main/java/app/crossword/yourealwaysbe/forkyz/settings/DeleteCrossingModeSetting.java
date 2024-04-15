
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

import app.crossword.yourealwaysbe.puz.Playboard.DeleteCrossingMode;

public enum DeleteCrossingModeSetting {
    DELETE("DELETE", DeleteCrossingMode.DELETE),
    PRESERVE_FILLED_WORDS(
        "PRESERVE_FILLED_WORDS",
        DeleteCrossingMode.PRESERVE_FILLED_WORDS
    ),
    PRESERVE_FILLED_CELLS(
        "PRESERVE_FILLED_CELLS",
        DeleteCrossingMode.PRESERVE_FILLED_CELLS
    );

    private String settingsValue;
    private DeleteCrossingMode deleteCrossingMode;

    DeleteCrossingModeSetting(
        String settingsValue, DeleteCrossingMode deleteCrossingMode
    ) {
        this.settingsValue = settingsValue;
        this.deleteCrossingMode = deleteCrossingMode;
    }

    public String getSettingsValue() { return settingsValue; }
    public DeleteCrossingMode getDeleteCrossingMode() {
        return deleteCrossingMode;
    }

    public static DeleteCrossingModeSetting getFromSettingsValue(String value) {
        for (DeleteCrossingModeSetting s : DeleteCrossingModeSetting.values()) {
            if (Objects.equals(value, s.getSettingsValue()))
                return s;
        }
        return DeleteCrossingModeSetting.DELETE;
    }
}
