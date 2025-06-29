
package app.crossword.yourealwaysbe.forkyz.versions

import androidx.annotation.RequiresApi
import android.app.Activity
import android.os.Build

@RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class VanillaIceCreamUtil() : UpsideDownCakeUtil() {
    override fun legacySetStatusBarColor(activity : Activity?, color : Int) {
        // do nothing
    }
}
