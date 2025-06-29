package app.crossword.yourealwaysbe.forkyz.versions;

import androidx.annotation.RequiresApi;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

@RequiresApi(api = Build.VERSION_CODES.R)
public class RUtil extends QUtil {
    @Override
    public void setFullScreen(Window window) {
        WindowInsetsController insetsController
            = window.getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars());
        }
    }
}
