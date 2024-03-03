package app.crossword.yourealwaysbe.forkyz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.inject.Inject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.core.text.HtmlCompat;
import androidx.core.view.WindowCompat;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.databinding.HtmlViewBinding;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.recycler.ShowHideOnScroll;

@AndroidEntryPoint
public class HTMLActivity extends ForkyzActivity {
    private static final Charset CHARSET = Charset.forName("UTF-8");

    @Inject
    protected AndroidVersionUtils utils;


    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    private HtmlViewBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = HtmlViewBinding.inflate(getLayoutInflater());

        setSupportActionBar(binding.toolbar);

        holographic();
        finishOnHomeButton();
        setContentView(binding.getRoot());
        setupBottomInsets(binding.content);
        setStatusBarElevation(binding.appBarLayout);

        String assetName = this.getIntent().getData().toString();
        startLoadAssetName(assetName);
        binding.backButton.setOnClickListener((v) -> { finish(); });

        setScrollListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getTitle() == null){
            finish();
            return true;
        }
        return false;
    }

    // suppress because ShowHideOnScroll does not consume/handle clicks
    @SuppressWarnings("ClickableViewAccessibility")
    private void setScrollListener() {
        binding.scrollView.setOnTouchListener(
            new ShowHideOnScroll(binding.backButton)
        );
    }

    private void startLoadAssetName(String assetName) {
        executorService.execute(() -> {
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        getAssets().open(assetName)
                    )
                )
            ) {
                String htmlData
                    = reader.lines().collect(Collectors.joining("\n"));
                handler.post(() -> {
                    binding.text.setText(HtmlCompat.fromHtml(htmlData, 0));
                });
            } catch (IOException e) {
                handler.post(() -> {
                    finish();
                });
            }
        });
    }
}
