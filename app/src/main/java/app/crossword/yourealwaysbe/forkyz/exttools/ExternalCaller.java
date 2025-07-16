
package app.crossword.yourealwaysbe.forkyz.exttools;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Helper method for calling out to an external app
 *
 * Throws error if app missing
 */
public class ExternalCaller {
    private static final String ARG_APP_NAME = "appName";
    private static final String ARG_APP_URL = "appUrl";

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
    public static void launchApp(
        Activity forkyzActivity,
        String externalPackage,
        String externalActivity,
        String dataField,
        String data,
        String appName,
        String appURL
    ) throws AppNotFoundException {
        try {
            Intent i = new Intent();
            i.setComponent(
                new ComponentName(externalPackage, externalActivity)
            );
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );
            i.putExtra(dataField, data);
            forkyzActivity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            throw new AppNotFoundException(appName, appURL);
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
    public static void launchIntent(
        Activity forkyzActivity,
        String intentName,
        String dataField,
        String data,
        String appName,
        String appURL
    ) throws AppNotFoundException {
        try {
            Intent i = new Intent(intentName);
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );
            i.putExtra(dataField, data);
            forkyzActivity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            throw new AppNotFoundException(appName, appURL);
        }
    }
}
