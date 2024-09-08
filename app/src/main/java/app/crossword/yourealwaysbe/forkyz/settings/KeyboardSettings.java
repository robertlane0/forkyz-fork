
package app.crossword.yourealwaysbe.forkyz.settings;

public record KeyboardSettings(
    boolean compact,
    boolean haptic,
    boolean hideButton,
    KeyboardLayout layout,
    KeyboardMode mode,
    int repeatDelay,
    int repeatInterval,
    boolean useNative
) { }

