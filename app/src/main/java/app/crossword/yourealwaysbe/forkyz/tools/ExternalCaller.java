
package app.crossword.yourealwaysbe.forkyz.tools;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Helper method for calling out to an external app
 *
 * With error dialog if app missing
 */
public class ExternalCaller {

    private static final String ARG_APP_NAME = "appName";
    private static final String ARG_APP_URL = "appUrl";

    /**
     * Launch external app
     *
     * Shows a dialog with weblink if app not installed
     *
     * @param forkyzActivity the activity calling from
     * @param externalPackage the package of the app being called
     * @param externalActivity the activity of the app being called
     * @param dataField the name of the string field extra used to pass
     * data
     * @param data the data to pass
     * @param appName the user-friendly name of the app
     * @param appUrl the url to get the app from
     */
    public static void launchApp(
        AppCompatActivity forkyzActivity,
        String externalPackage,
        String externalActivity,
        String dataField,
        String data,
        String appName,
        String appUrl
    ) {
        try {
            Intent i = new Intent();
            i.setComponent(
                new ComponentName(externalPackage, externalActivity)
            );
            i.putExtra(dataField, data);
            forkyzActivity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            launchNotFoundDialog(forkyzActivity, appName, appUrl);
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
     * @param appName the user-friendly name of the app
     * @param appUrl the url to get the app from
     */
    public static void launchIntent(
        AppCompatActivity forkyzActivity,
        String intentName,
        String dataField,
        String data,
        String appName,
        String appUrl
    ) {
        try {
            Intent i = new Intent(intentName);
            i.putExtra(dataField, data);
            forkyzActivity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            launchNotFoundDialog(forkyzActivity, appName, appUrl);
        }
    }

    private static void launchNotFoundDialog(
        AppCompatActivity forkyzActivity, String appName, String appUrl
    ) {
        DialogFragment dialog = new NoAppDialog();

        Bundle args = new Bundle();
        args.putString(ARG_APP_NAME, appName);
        args.putString(ARG_APP_URL, appUrl);
        dialog.setArguments(args);

        dialog.show(
            forkyzActivity.getSupportFragmentManager(),
            "NoAppDialog"
        );
    }

    public static class NoAppDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            Bundle args = getArguments();
            String appName = args.getString(ARG_APP_NAME);
            String appUrl = args.getString(ARG_APP_URL);

            String title = activity.getString(
                R.string.external_app_not_installed, appName
            );
            String msg = activity.getString(
                R.string.external_app_install_msg, appName
            );

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(title)
                .setMessage(msg)
                .setNegativeButton(
                    R.string.website,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(appUrl));
                            activity.startActivity(intent);
                        }
                    }
                ).setPositiveButton(android.R.string.ok, null);

            return builder.create();
       }
    }


}
