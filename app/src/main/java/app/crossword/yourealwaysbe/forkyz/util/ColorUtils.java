
package app.crossword.yourealwaysbe.forkyz.util;

public class ColorUtils {
    private final static int ADD_ALPHA = 0xff000000;
    private final static int DEL_ALPHA = 0x00ffffff;

    /**
     * Add ff alpha channel
     */
    public static int addAlpha(int color) {
        return color | ADD_ALPHA;
    }

    /**
     * Remove ff alpha channel
     */
    public static int delAlpha(int color) {
        return color & DEL_ALPHA;
    }
}
