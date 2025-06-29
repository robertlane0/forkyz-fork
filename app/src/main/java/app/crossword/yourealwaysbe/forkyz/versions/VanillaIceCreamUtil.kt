
package app.crossword.yourealwaysbe.forkyz.versions

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build

@TargetApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class VanillaIceCreamUtil() : UpsideDownCakeUtil() {
    override fun legacySetStatusBarColor(activity : Activity?, color : Int) {
        // do nothing
    }
}
