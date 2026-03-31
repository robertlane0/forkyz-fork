
package app.crossword.yourealwaysbe.forkyz.versions

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class VanillaIceCreamUtil() : UpsideDownCakeUtil() {
    override fun legacySetStatusBarColor(activity : Activity, color : Int) {
        // do nothing
    }
}

