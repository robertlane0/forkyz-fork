package app.crossword.yourealwaysbe.forkyz;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper;
import app.crossword.yourealwaysbe.forkyz.util.ThemeHelper;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

import java.util.logging.Logger;
import javax.inject.Inject;

@AndroidEntryPoint
public class ForkyzActivity extends AppCompatActivity {
    private static final Logger LOG
        = Logger.getLogger(ForkyzActivity.class.getCanonicalName());

    @Inject
    protected ForkyzSettings settings;

    protected AndroidVersionUtils utils = AndroidVersionUtils.Factory
            .getInstance();
    public NightModeHelper nightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        doOrientation();
        (new ThemeHelper(settings)).themeActivity(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        nightMode.restoreNightMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(nightMode == null) {
            nightMode = NightModeHelper.bind(this, settings);
            nightMode.restoreNightMode();
        }

        doOrientation();
    }

    @SuppressWarnings("SourceLockedOrientationActivity")
    private void doOrientation() {
        settings.getAppOrientationLock(orientation -> {
            try {
                switch (orientation) {
                case PORTRAIT:
                    setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    );
                    break;
                case LANDSCAPE:
                    setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    );
                    break;
                default:
                    setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    );
                }
            } catch(RuntimeException e) {
                Toast.makeText(
                    this,
                    R.string.no_orientation_lock,
                    Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    protected Bitmap createBitmap(String fontFile, String character){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dpi = Math.round(160F * metrics.density);
        int size = dpi / 2;
        Bitmap bitmap = Bitmap.createBitmap(size , size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.createFromAsset(getAssets(), fontFile));
        p.setTextSize(size);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(character, size/2, size - size / 9, p );
        return bitmap;
    }

    protected static Spanned smartHtml(String text) {
        return text == null ? null : HtmlCompat.fromHtml(text, 0);
    }
}
