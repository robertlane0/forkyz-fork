
package app.crossword.yourealwaysbe.forkyz.versions;

import androidx.annotation.RequiresApi;
import android.content.Context;
import android.app.PendingIntent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MarshmallowUtil extends LollipopUtil {
    @Override
    public int immutablePendingIntentFlag() {
        return PendingIntent.FLAG_IMMUTABLE;
    }

    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    ) {
        return StaticLayout.Builder.obtain(
            text, 0, text.length(), style, width
        ).setAlignment(align)
        .build();
    }

    @Override
    public boolean hasNetworkConnection(Context activity) {
        ConnectivityManager cm
            = activity.getSystemService(ConnectivityManager.class);
        return cm.getActiveNetwork() != null;
    }
}
