
package app.crossword.yourealwaysbe.forkyz.versions

import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
open class UpsideDownCakeUtil  : TiramisuUtil() {
    public override fun foregroundServiceTypeShortService() : Int {
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
    }

    public override fun foregroundServiceTypeDataSync() : Int {
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }
}

