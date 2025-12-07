
package app.crossword.yourealwaysbe.forkyz.versions

import java.io.IOException
import java.io.Serializable

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher

interface AndroidVersionUtils {
    object Factory {
        fun provideAndroidVersionUtils() : AndroidVersionUtils {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                return VanillaIceCreamUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                return UpsideDownCakeUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return TiramisuUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                return RUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                return QUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return PieUtil()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return OreoUtil()
            return MarshmallowUtil()
        }
    }

    fun createNotificationChannels(context : Context)

    fun immutablePendingIntentFlag() : Int

    fun getStaticLayout(
        text : CharSequence,
        style : TextPaint,
        width : Int,
        align : Layout.Alignment,
    ) : StaticLayout

    fun getStaticLayout(
        text : CharSequence,
        style : TextPaint,
        width : Int,
    ) : StaticLayout {
        return getStaticLayout(
            text, style, width, Layout.Alignment.ALIGN_NORMAL
        )
    }

    fun setFullScreen(window : Window)

    val semiBoldTypeface : Typeface

    @Throws(IOException::class)
    fun isInternalStorageFull(
        context : Context,
        minimumBytesFree : Long,
    ) : Boolean

    fun hasNetworkConnection(context : Context) : Boolean

    /**
     * Has POST_NOTIFICATIONS permission or is not on an API that needs it
     */
    fun hasPostNotificationsPermission(context : Context) : Boolean

    /**
     * Request POST_NOTIFICATIONS on devices that have it
     *
     * Else, ignore
     */
    fun requestPostNotifications(launcher : ActivityResultLauncher<String>)

    /**
     * If rationale is needed when requesting notification permission
     */
    fun shouldShowRequestNotificationPermissionRationale(
        activity : Activity,
    ) : Boolean

    /**
     * Call invalidateInput or restartInput as available
     */
    fun invalidateInput(imm : InputMethodManager, view : View)

    /**
     * Get the current application version name
     */
    fun getApplicationVersionName(context : Context) : String?

    /**
     * Get the current application uid or -1
     */
    fun getApplicationUID(context : Context) : Int

    /**
     * A regex for filtering all non alphabetic chars and tags from clue text
     *
     * (Before Android 10 "Alphabetic", since "IsAlphabetic")
     */
    val filterClueToAlphabeticRegex : String

    /**
     * Constant ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
     */
    fun foregroundServiceTypeShortService() : Int

    /**
     * Constant ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
     */
    fun foregroundServiceTypeDataSync() : Int

    /**
     * Set status bar color using deprecated API methods if available
     */
    fun legacySetStatusBarColor(activity : Activity, color : Int)
}

