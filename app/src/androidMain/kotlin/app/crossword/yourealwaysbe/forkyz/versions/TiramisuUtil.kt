
package app.crossword.yourealwaysbe.forkyz.versions

import java.io.Serializable

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
open class TiramisuUtil  : RUtil() {
    public override fun hasPostNotificationsPermission(
        context : Context,
    ) : Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    public override fun requestPostNotifications(
        launcher : ActivityResultLauncher<String>,
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    public override fun shouldShowRequestNotificationPermissionRationale(
        activity : Activity,
    ) : Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.POST_NOTIFICATIONS
        )
    }

    public override fun invalidateInput(imm : InputMethodManager, view : View) {
        imm.invalidateInput(view)
    }

    public override fun getApplicationVersionName(context : Context) : String? {
        try {
            val info = context.getPackageManager()
                .getPackageInfo(
                    context.getPackageName(),
                    PackageInfoFlags.of(0),
                )
            return info.versionName
        } catch (e : PackageManager.NameNotFoundException) {
            return null
        }
    }

    public override fun getApplicationUID(context : Context) : Int {
        try {
            val info = context.getPackageManager()
                .getPackageInfo(
                    context.getPackageName(),
                    PackageInfoFlags.of(0),
                )
            return info.applicationInfo!!.uid
        } catch (e : PackageManager.NameNotFoundException) {
            return -1
        }
    }
}

