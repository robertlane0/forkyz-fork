
package app.crossword.yourealwaysbe.forkyz.versions

import java.io.IOException
import java.io.Serializable

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher

open class MarshmallowUtil  : AndroidVersionUtils {
    override fun createNotificationChannels(context : Context) {
    }

    override fun immutablePendingIntentFlag() : Int {
        return PendingIntent.FLAG_IMMUTABLE
    }

    override fun getStaticLayout(
        text : CharSequence,
        style : TextPaint,
        width : Int,
        align : Layout.Alignment,
    ) : StaticLayout {
        return StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            style,
            width,
        ).setAlignment(align)
            .build()
    }

    @Suppress("deprecation")
    override fun setFullScreen(window : Window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
    }

    // Fallback to bold, no semibold before P
    override val semiBoldTypeface : Typeface
        get() = Typeface.create("sans-serif", Typeface.BOLD)

    @Throws(IOException::class)
    override fun isInternalStorageFull(
        context : Context,
        minimumBytesFree : Long,
    ) : Boolean {
        val files = context.getFilesDir()
        return files.getFreeSpace() < minimumBytesFree
    }

    override fun hasNetworkConnection(context : Context) : Boolean {
        val cm = context.getSystemService<ConnectivityManager>(
            ConnectivityManager::class.java,
        )
        return cm.getActiveNetwork() != null
    }

    override fun hasPostNotificationsPermission(context : Context) : Boolean {
        return true
    }

    override fun requestPostNotifications(
        launcher : ActivityResultLauncher<String>,
    ) {
        // do nothing
    }

    override fun shouldShowRequestNotificationPermissionRationale(
        activity : Activity,
    ) : Boolean {
        return false
    }

    override fun invalidateInput(imm : InputMethodManager, view : View) {
        imm.restartInput(view)
    }

    @Suppress("deprecation")
    override fun getApplicationUID(context : Context) : Int {
        try {
            val info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
            return info.applicationInfo!!.uid
        } catch (e : PackageManager.NameNotFoundException) {
            return -1
        }
    }

    override val filterClueToAlphabeticRegex : String
        get() = "&[^;]*;|<[^>]*>|[^\\p{Alphabetic}]"

    override fun foregroundServiceTypeShortService() : Int {
        return 0
    }

    override fun foregroundServiceTypeDataSync() : Int {
        return 0
    }

    @Suppress("deprecation")
    override fun legacySetStatusBarColor(activity : Activity, color : Int) {
        activity.getWindow().setStatusBarColor(color)
    }
}
