package me.edgan.redditslide.util;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import me.edgan.redditslide.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import me.edgan.redditslide.Visuals.Palette;

/** Created by TacoTheDank on 07/14/2021. Updated to use Storage Access Framework */
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

        showBaseChooserDialog(
                activity, R.string.err_something_wrong, R.string.err_couldnt_save_choose_new);
    }

    public static void showFirstDialog(final AppCompatActivity activity) {
        showBaseChooserDialog(activity, R.string.set_save_location, R.string.set_save_location_msg);
    }

    private static void showBaseChooserDialog(
            final AppCompatActivity activity,
            final @StringRes int titleId,
            final @StringRes int messageId) {
        AlertDialog dialog =
                new AlertDialog.Builder(activity)
                        .setTitle(titleId)
                        .setMessage(messageId)
                        .setPositiveButton(
                                android.R.string.ok,
                                (dialogInterface, which) -> {
                                    try {
                                        StorageUtil.showDirectoryChooser(activity);
                                    } catch (Exception e) {
                                        LogUtil.e(e, TAG + "Error launching directory chooser");
                                    }
                                })
                        .create();

        dialog.show();
    }

    /**
     * Applies a custom accent-colored border to an AlertDialog
     * @param context Context for accessing resources
     * @param dialog The AlertDialog to apply the border to
     */
    public static void applyCustomBorderToAlertDialog(Context context, AlertDialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            // Create a GradientDrawable with the accent color
            GradientDrawable drawable = new GradientDrawable();

            // Get the appropriate background color from the current theme
            TypedArray ta = context.obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
            int backgroundColor = ta.getColor(0, Color.BLACK);
            ta.recycle();

            // Set the drawable properties
            drawable.setColor(backgroundColor);
            drawable.setStroke(DisplayUtil.dpToPxVertical(2), Palette.getDarkerColor(Palette.getDefaultAccent()));
            drawable.setCornerRadius(DisplayUtil.dpToPxVertical(2));

            // Apply the drawable to the dialog window
            dialog.getWindow().setBackgroundDrawable(drawable);
        }
    }
}
