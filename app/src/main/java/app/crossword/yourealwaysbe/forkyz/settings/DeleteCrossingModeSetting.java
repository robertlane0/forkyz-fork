
package app.crossword.yourealwaysbe.forkyz.settings;

import app.crossword.yourealwaysbe.puz.Playboard.DeleteCrossingMode;

public enum DeleteCrossingModeSetting implements EnumSetting {
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

    @Override
    public String getSettingsValue() { return settingsValue; }

    public DeleteCrossingMode getDeleteCrossingMode() {
        return deleteCrossingMode;
    }
}
