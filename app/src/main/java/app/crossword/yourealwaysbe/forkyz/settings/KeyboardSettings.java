
package app.crossword.yourealwaysbe.forkyz.settings;

public class KeyboardSettings {
    private boolean compact;
    private boolean haptic;
    private boolean hideButton;
    private KeyboardLayout layout;
    private KeyboardMode mode;
    private int repeatDelay;
    private int repeatInterval;
    private boolean useNative;

    public KeyboardSettings(
        boolean compact,
        boolean haptic,
        boolean hideButton,
        KeyboardLayout layout,
        KeyboardMode mode,
        int repeatDelay,
        int repeatInterval,
        boolean useNative
    ) {
        this.compact = compact;
        this.haptic = haptic;
        this.hideButton = hideButton;
        this.layout = layout;
        this.mode = mode;
        this.repeatDelay = repeatDelay;
        this.repeatInterval = repeatInterval;
        this.useNative = useNative;
    }

    public boolean getCompact() { return compact; }
    public boolean getHaptic() { return haptic; }
    public boolean getHideButton() { return hideButton; }
    public KeyboardLayout getLayout() { return layout; }
    public KeyboardMode getMode() { return mode; }
    public int getRepeatDelay() { return repeatDelay; }
    public int getRepeatInterval() { return repeatInterval; }
    public boolean getUseNative() { return useNative; }
}
