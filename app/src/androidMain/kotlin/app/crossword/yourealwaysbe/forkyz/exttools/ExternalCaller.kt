
package app.crossword.yourealwaysbe.forkyz.exttools

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent

private val ARG_APP_NAME = "appName"
private val ARG_APP_URL = "appUrl"

/**
 * Helper method for calling out to an external app
 *
 * Throws error if app missing
 */
object ExternalCaller {
    /**
     * Launch external app
     *
     * @param forkyzActivity the activity calling from
     * @param externalPackage the package of the app being called
     * @param externalActivity the activity of the app being called
     * @param dataField the name of the string field extra used to pass
     * data
     * @param data the data to pass
     * @param appName name of app for not found exception
     * @param appURL URL of app for not found exception
     */
    @Throws(AppNotFoundException::class)
    fun launchApp(
        forkyzActivity : Activity,
        externalPackage : String,
        externalActivity : String,
        dataField : String?,
        data : String?,
        appName : String,
        appURL : String,
    ) {
        try {
            val i = Intent()
            i.setComponent(ComponentName(externalPackage, externalActivity))
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            i.putExtra(dataField, data)
            forkyzActivity.startActivity(i)
        } catch (e : ActivityNotFoundException) {
            throw AppNotFoundException(appName, appURL)
        }
    }

    /**
     * Launch external app via a single intent
     *
     * Shows a dialog with weblink if app not installed
     *
     * @param forkyzActivity the activity calling from
     * @param intentName the name of the intent to start the app
     * @param dataField the name of the string field extra used to pass
     * data
     * @param data the data to pass
     * @param appName name of app for not found exception
     * @param appURL URL of app for not found exception
     */
    @Throws(AppNotFoundException::class)
    fun launchIntent(
        forkyzActivity : Activity,
        intentName : String?,
        dataField : String?,
        data : String?,
        appName : String,
        appURL : String,
    ) {
        try {
            val i = Intent(intentName)
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            i.putExtra(dataField, data)
            forkyzActivity.startActivity(i)
        } catch (e : ActivityNotFoundException) {
            throw AppNotFoundException(appName, appURL)
        }
    }
}

