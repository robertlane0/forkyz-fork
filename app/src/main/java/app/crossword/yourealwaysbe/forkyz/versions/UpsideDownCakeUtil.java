
package app.crossword.yourealwaysbe.forkyz.versions;

import androidx.annotation.RequiresApi;
import android.content.pm.ServiceInfo;
import android.os.Build;

@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class UpsideDownCakeUtil extends TiramisuUtil {
    @Override
    public int foregroundServiceTypeShortService() {
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE;
    }

    @Override
    public int foregroundServiceTypeDataSync() {
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
    }
}
