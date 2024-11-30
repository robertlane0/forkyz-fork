
package app.crossword.yourealwaysbe.forkyz.settings;

import app.crossword.yourealwaysbe.puz.MovementStrategy;

public enum MovementStrategySetting implements EnumSetting {
    MOVE_NEXT_ON_AXIS("MOVE_NEXT_ON_AXIS", MovementStrategy.MOVE_NEXT_ON_AXIS),
    STOP_ON_END("STOP_ON_END", MovementStrategy.STOP_ON_END),
    MOVE_NEXT_CLUE("MOVE_NEXT_CLUE", MovementStrategy.MOVE_NEXT_CLUE),
    MOVE_PARALLEL_WORD(
        "MOVE_PARALLEL_WORD", MovementStrategy.MOVE_PARALLEL_WORD
    );

    private String settingsValue;
    private MovementStrategy movementStrategy;

    MovementStrategySetting(
        String settingsValue, MovementStrategy movementStrategy
    ) {
        this.settingsValue = settingsValue;
        this.movementStrategy = movementStrategy;
    }

    @Override
    public String getSettingsValue() { return settingsValue; }

    public MovementStrategy getMovementStrategy() { return movementStrategy; }
}
