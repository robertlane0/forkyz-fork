package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.IOException;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.util.Log;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.util.PuzzleExportService;
import app.crossword.yourealwaysbe.forkyz.util.PuzzleSaveService;

@RequiresApi(api = Build.VERSION_CODES.O)
public class OreoUtil extends NougatUtil {
    private static final String TAG = "ForkyzOreoUtil";

    @Override
    public void createNotificationChannels(Context context) {
        Log.i(TAG, "Creating notification channels");

        NotificationManager notificationManager
            = context.getSystemService(NotificationManager.class);

        CharSequence name
            = context.getString(R.string.download_notification_channel_name);
        String description
            = context.getString(R.string.download_notification_channel_desc);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(
            DownloadersProvider.PUZZLE_DOWNLOAD_CHANNEL_ID,
            name,
            importance
        );
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);

        name = context.getString(R.string.save_notification_channel_name);
        description
            = context.getString(R.string.save_notification_channel_desc);
        channel = new NotificationChannel(
            PuzzleSaveService.NOTIFICATION_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);

        name = context.getString(R.string.export_notification_channel_name);
        description
            = context.getString(R.string.export_notification_channel_desc);
        channel = new NotificationChannel(
            PuzzleExportService.NOTIFICATION_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public boolean isInternalStorageFull(
        Context context, long minimumBytesFree
    ) throws IOException {
        StorageManager storageManager
            = context.getSystemService(StorageManager.class);
        UUID appSpecificInternalDirUuid
            = storageManager.getUuidForPath(context.getFilesDir());
        long availableBytes =
            storageManager.getAllocatableBytes(
                appSpecificInternalDirUuid
            );
        return availableBytes < minimumBytesFree;
    }
}
