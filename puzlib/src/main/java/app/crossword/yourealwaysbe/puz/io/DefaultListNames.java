
package app.crossword.yourealwaysbe.puz.io;

import java.util.Locale;
import java.util.ResourceBundle;

public class DefaultListNames {
    public static String ACROSS_LIST;
    public static String DOWN_LIST;
    public static String NODIR_LIST;
    static {
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(
                "app.crossword.yourealwaysbe.puz.strings",
                Locale.getDefault()
            );
        ACROSS_LIST = resourceBundle.getString("acrossListName");
        DOWN_LIST = resourceBundle.getString("downListName");
        NODIR_LIST = resourceBundle.getString("noDirListName");
    }
}
