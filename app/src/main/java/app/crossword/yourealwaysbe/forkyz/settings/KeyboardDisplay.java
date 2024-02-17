
package app.crossword.yourealwaysbe.forkyz.settings;

/**
 * Display parameters for keyboard
 *
 * At moment, layout and whether compact
 */
public class KeyboardDisplay {
    private KeyboardLayout layout;
    private boolean compact;

    public KeyboardDisplay(KeyboardLayout layout, boolean compact) {
        this.layout = layout;
        this.compact = compact;
    }

    public KeyboardLayout getLayout() { return layout; }
    public boolean getCompact() { return compact; }
}
