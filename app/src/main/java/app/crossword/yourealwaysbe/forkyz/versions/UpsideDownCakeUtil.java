
package app.crossword.yourealwaysbe.forkyz.versions;

import android.annotation.TargetApi;
import android.content.pm.ServiceInfo;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class UpsideDownCakeUtil extends TiramisuUtil {
    @Override
    public int foregroundServiceTypeShortService() {
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE;
    }
}
