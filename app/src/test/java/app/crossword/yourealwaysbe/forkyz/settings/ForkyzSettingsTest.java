
package app.crossword.yourealwaysbe.forkyz.settings;

import android.content.SharedPreferences;
import android.os.Handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForkyzSettingsTest {

    @Mock
    Handler handler = mock(Handler.class);

    @Mock
    SharedPreferences prefs = mock(SharedPreferences.class);

    @Before
    public void setupMocks() {
        when(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(prefs.getInt(anyString(), anyInt())).thenReturn(0);

        // may need to actually run in future!
        when(handler.post(any(Runnable.class))).thenReturn(true);
    }

    @Test
    public void testDefaults() {
        ForkyzSettings settings = new ForkyzSettings(prefs, handler);
        for (Method method : ForkyzSettings.class.getDeclaredMethods()) {
            boolean isGet = method.getName().startsWith("get");
            boolean isPublic = (method.getModifiers() & Modifier.PUBLIC) > 0;
            boolean isNotStatic = (method.getModifiers() & Modifier.STATIC) == 0;

            if (isGet && isPublic && isNotStatic) {
                try {
                    if (method.getParameterCount() == 0) {
                        method.invoke(settings);
                    } else {
                        Consumer<Object> cb = value -> {};
                        method.invoke(settings, cb);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    fail("Failed to call " + method.getName());
                }
            }
        }
    }
}

