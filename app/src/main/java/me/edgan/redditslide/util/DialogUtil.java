package me.edgan.redditslide.util;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import me.edgan.redditslide.R;

/**
 * Created by TacoTheDank on 07/14/2021.
 * Updated to use Storage Access Framework
 */
public class DialogUtil {
    private static final String TAG = "DialogUtil";

    public static void showErrorDialog(final AppCompatActivity activity) {
        Log.d(TAG, "showErrorDialog called from: " + Log.getStackTraceString(new Exception()));

        Uri currentUri = StorageUtil.getStorageUri(activity);
        Log.d(TAG, "Current storage URI: " + currentUri);

        if (currentUri != null) {
            DocumentFile file = DocumentFile.fromTreeUri(activity, currentUri);
            Log.d(TAG, "Current DocumentFile exists: " + (file != null));
            if (file != null) {
                Log.d(TAG, "DocumentFile canWrite: " + file.canWrite());
                Log.d(TAG, "DocumentFile name: " + file.getName());
            }
        }

        showBaseChooserDialog(activity,
                R.string.err_something_wrong, R.string.err_couldnt_save_choose_new);
    }

    public static void showFirstDialog(final AppCompatActivity activity) {
        showBaseChooserDialog(activity,
                R.string.set_save_location, R.string.set_save_location_msg);
    }

    private static void showBaseChooserDialog(
            final AppCompatActivity activity,
            final @StringRes int titleId,
            final @StringRes int messageId
    ) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                    try {
                        StorageUtil.showDirectoryChooser(activity);
                    } catch (Exception e) {
                        LogUtil.e(e, TAG + "Error launching directory chooser");
                    }
                })
                .create();

        dialog.show();
    }
}
