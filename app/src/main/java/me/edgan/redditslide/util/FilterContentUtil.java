package me.edgan.redditslide.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

/**
 * Utility class for handling content filtering dialogs
 */
public class FilterContentUtil {

    /**
     * Shows a dialog to filter content for a specific subreddit
     *
     * @param activity The activity context
     * @param subreddit The subreddit to filter content for
     * @param onSaveCallback Callback to execute after filters are saved
     */
    public static void showFilterDialog(Activity activity, String subreddit, Runnable onSaveCallback) {
        // Create a custom dialog view with our own button layout
        LinearLayout customDialogView = new LinearLayout(activity);
        customDialogView.setOrientation(LinearLayout.VERTICAL);

        // Add the original filter dialog content
        View filterContent = LayoutInflater.from(activity).inflate(R.layout.dialog_two_column_filter, null);
        customDialogView.addView(filterContent);

        // Get references to the list views
        final ListView regularListView = filterContent.findViewById(R.id.regular_content_list);
        final ListView nsfwListView = filterContent.findViewById(R.id.nsfw_content_list);

        // Create a wrapper class to hold the lists reference that can be modified
        class ListsHolder {
            FilterUtil.FilterLists lists;
        }

        final ListsHolder listsHolder = new ListsHolder();
        listsHolder.lists = FilterUtil.setupFilterLists(activity, subreddit);

        FilterUtil.setupListViews(activity, regularListView, nsfwListView, listsHolder.lists);

        final String FILTER_TITLE = activity.getString(R.string.content_to_show,
                subreddit.equals("frontpage") ? "frontpage" : "/r/" + subreddit);

        // Create the dialog with no buttons (we'll add our own)
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(FILTER_TITLE)
                .setView(customDialogView);

        final AlertDialog dialog = builder.create();

        // Create a custom button bar
        LinearLayout buttonBar = new LinearLayout(activity);
        buttonBar.setOrientation(LinearLayout.HORIZONTAL);
        buttonBar.setPadding(16, 8, 16, 8);

        // Create the toggle button (left)
        Button toggleButton = new Button(activity, null, android.R.attr.buttonBarButtonStyle);
        toggleButton.setText(R.string.btn_toggle);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        toggleButton.setLayoutParams(toggleParams);

        // Create the reset button (center)
        Button resetButton = new Button(activity, null, android.R.attr.buttonBarButtonStyle);
        resetButton.setText(R.string.btn_reset);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        resetButton.setLayoutParams(resetParams);

        // Create the save button (right)
        Button saveButton = new Button(activity, null, android.R.attr.buttonBarButtonStyle);
        saveButton.setText(R.string.btn_save);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        saveButton.setLayoutParams(saveParams);

        // Add buttons to the button bar
        buttonBar.addView(toggleButton);
        buttonBar.addView(resetButton);
        buttonBar.addView(saveButton);

        // Add the button bar to the dialog
        customDialogView.addView(buttonBar);

        // Set up button click listeners
        toggleButton.setOnClickListener(view -> {
            FilterToggleUtil.handleFilterToggle(regularListView, nsfwListView, listsHolder.lists,
                    SettingValues.showNSFWContent, 0);
        });

        resetButton.setOnClickListener(view -> {
            listsHolder.lists = FilterUtil.setupFilterLists(activity, subreddit);
            FilterUtil.setupListViews(activity, regularListView, nsfwListView, listsHolder.lists);
        });

        saveButton.setOnClickListener(view -> {
            FilterUtil.saveFilters(regularListView, nsfwListView, listsHolder.lists, subreddit);
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}