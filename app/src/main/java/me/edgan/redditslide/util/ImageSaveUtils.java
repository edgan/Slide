package me.edgan.redditslide.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import me.edgan.redditslide.Notifications.ImageDownloadNotificationService;
import me.edgan.redditslide.util.GifUtils;
import me.edgan.redditslide.util.StorageUtil;

public class ImageSaveUtils {
    private static final String TAG = "ImageSaveUtils";
    public static final String EXTRA_SUBMISSION_TITLE = "title";

    /**
     * Saves an image or gif to the device storage
     *
     * @param activity The activity context
     * @param isGif Whether the content is a gif/video
     * @param contentUrl The URL of the content to save
     * @param index The index of the image in a gallery
     * @param subreddit The subreddit the content is from
     * @param submissionTitle The title of the submission
     * @param showFirstDialogCallback Callback to show the storage access dialog
     */
    public static void doImageSave(
            Activity activity,
            boolean isGif,
            String contentUrl,
            int index,
            String subreddit,
            String submissionTitle,
            Runnable showFirstDialogCallback) {

        Uri storageUri = StorageUtil.getStorageUri(activity);
        if (storageUri == null || !StorageUtil.hasStorageAccess(activity)) {
            Log.e(TAG, "No valid storage URI found.");
            if (showFirstDialogCallback != null) {
                showFirstDialogCallback.run();
            }
            return;
        } else {
            if (isGif) {
                // Handle video/gif save
                GifUtils.cacheSaveGif(
                        Uri.parse(contentUrl),
                        activity,
                        subreddit != null ? subreddit : "",
                        submissionTitle != null ? submissionTitle : "",
                        true);
            } else {
                // Handle image save
                Intent i = new Intent(activity, ImageDownloadNotificationService.class);
                i.putExtra("actuallyLoaded", contentUrl);
                i.putExtra("downloadUri", storageUri.toString());

                if (subreddit != null && !subreddit.isEmpty()) {
                    i.putExtra("subreddit", subreddit);
                }
                if (submissionTitle != null) {
                    i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                }
                i.putExtra("index", index);

                activity.startService(i);
            }
        }
    }
}