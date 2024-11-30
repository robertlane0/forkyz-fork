
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Objects;

/**
 * A class to encapsulate some common enum setting code
 */
public interface EnumSetting {
    /**
     * String representing the enum for settings
     */
    String getSettingsValue();

    /**
     * Convert settings string to value
     *
     * @param value the value to convert
     * @param klass the class to convert to
     * @param defaultValue the default value
     * @return the enum value or default value if not found
     */
    static <T extends Enum<T> & EnumSetting>
    T getFromSettingsValueNonNull(
        String value, Class<T> klass, T defaultValue
    ) {
        T gotten = getFromSettingsValueNull(value, klass);
        return gotten == null ? defaultValue : gotten;
    }

    /**
     * Convert settings string to value
     *
     * @param value the value to convert
     * @param klass the class to convert to
     * @return the enum value or null
     */    static <T extends Enum<T> & EnumSetting>
    T getFromSettingsValueNull(String value, Class<T> klass) {
        for (T m : klass.getEnumConstants()) {
            if (Objects.equals(value, m.getSettingsValue()))
                return m;
        }
        return null;
    }
}
