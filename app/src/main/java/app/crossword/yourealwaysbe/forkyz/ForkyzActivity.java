package app.crossword.yourealwaysbe.forkyz;

import java.util.logging.Logger;
import javax.inject.Inject;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper;
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder;
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper;
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.puz.Playboard;

@AndroidEntryPoint
public class ForkyzActivity extends AppCompatActivity {
    private static final Logger LOG
        = Logger.getLogger(ForkyzActivity.class.getCanonicalName());

    @Inject
    protected ForkyzSettings settings;

    @Inject
    protected CurrentPuzzleHolder currentPuzzleHolder;

    @Inject
    protected AndroidVersionUtils utils;

    @Inject
    protected ThemeHelper themeHelper;

    @Inject
    protected OrientationHelper orientationHelper;

    protected NightModeHelper nightMode;
    private Drawable originalStatusBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeHelper.themeActivity(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        orientationHelper.applyOrientation(this);
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

    /**
     * Set the board and base file of the puzzle loaded on it
     */
    protected void setCurrentBoard(Playboard board, PuzHandle puzHandle) {
        currentPuzzleHolder.setBoard(board, puzHandle);
    }

    protected Playboard getCurrentBoard() {
        return currentPuzzleHolder.getBoard();
    }

    protected PuzHandle getCurrentPuzHandle() {
        return currentPuzzleHolder.getPuzHandle();
    }

    protected void clearCurrentBoard() {
        System.out.println("FORKYZ: base activity clear board");
        currentPuzzleHolder.clearBoard();
    }

    protected void finishOnHomeButton() {
        ActionBar bar = getSupportActionBar();
        if(bar == null){
            return;
        }
        bar.setDisplayHomeAsUpEnabled(true);
        View home = findViewById(android.R.id.home);
        if(home != null){
            home.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    finish();
                }
            });
        }
    }

    protected void holographic() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Call this during onCreate for bottommost views of layout
     *
     * Because we setDecorFitsSystemWindows to false, we have to manage
     * our own insets. Calling this method on bottom most view makes
     * sure it is not hidden by the keyboard and navigation buttons.
     *
     * Assumes view also touches sides, so sets side insets
     */
    protected void setupBottomInsets(View view) {
        setupInsets(view, true, true);
    }

    /**
     * Call this during onCreate for views of layout touching sides
     *
     * Because we setDecorFitsSystemWindows to false, we have to manage our own
     * insets. Calling this method on a view makes sure it is not hidden by the
     * navigation buttons in landscape.
     */
    protected void setupSideInsets(View view) {
        setupInsets(view, true, false);
    }

    /**
     * Set the elevation to match the app bar
     *
     * As per Material3 docs
     *
     * Public for theme helper
     */
    public void setStatusBarElevation(AppBarLayout appBarLayout) {
        appBarLayout.setStatusBarForeground(
            MaterialShapeDrawable.createWithElevationOverlay(this)
        );
    }

    /**
     * Set color of status bar
     *
     * Calling reset will reset the status bar to what it was the first
     * time this method was used. I would call setStatusBarElevation
     * first if you want to be able to reset to that.
     *
     * Make public for theme helper
     */
    public void setStatusBarColor(AppBarLayout appBarLayout, int color) {
        if (originalStatusBar == null)
            originalStatusBar = appBarLayout.getStatusBarForeground();
        appBarLayout.setStatusBarForeground(
            MaterialShapeDrawable.createWithElevationOverlay(
                this, 0, ColorStateList.valueOf(color)
            )
        );
    }

    /**
     * Reset status bar to original color when setStatusBarColor first called
     *
     * Does nothing if no original set by call to setStatusBarColor.
     */
    protected void resetStatusBarColor(AppBarLayout appBarLayout) {
        if (originalStatusBar != null)
            appBarLayout.setStatusBarForeground(originalStatusBar);
    }

    /**
     * Set margins on view to avoid system bars/ime on bottom/side
     *
     * Needed with setDecorFitsSystemWindows(.., true) for app bar
     * overlap status bar. Else bottom bars/ime overlap app.
     */
    public void setupInsets(View view, boolean sides, boolean bottom) {
        MarginLayoutParams origMlp
            = (MarginLayoutParams) view.getLayoutParams();
        // keep own copies because above object will change with view
        int origLeftMargin = origMlp.leftMargin;
        int origBottomMargin = origMlp.bottomMargin;
        int origRightMargin = origMlp.rightMargin;

        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime()
                );

                MarginLayoutParams mlp
                    = (MarginLayoutParams) v.getLayoutParams();
                if (sides) {
                    mlp.leftMargin = insets.left + origLeftMargin;
                    mlp.rightMargin = insets.right + origRightMargin;
                }
                if (bottom)
                    mlp.bottomMargin = insets.bottom + origBottomMargin;
                v.setLayoutParams(mlp);

                // do not consume insets -- it prevents other views
                // avoiding system bars/ime on older Android versions
                return windowInsets;
            }
        );
    }

    /**
     * Setup status bar stuff
     *
     * Elevation and colour to match show errors
     */
    protected void setupStatusBar(AppBarLayout appBarLayout) {
        themeHelper.setupStatusBar(this, appBarLayout);
    }
}
