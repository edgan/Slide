package me.edgan.redditslide.util;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Locale;

import me.edgan.redditslide.PostMatch;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

public class FilterUtil {
    public static class FilterLists {
        public final ArrayList<Boolean> regularList = new ArrayList<>();
        public final ArrayList<String> regularLabels = new ArrayList<>();
        public final ArrayList<Boolean> nsfwList = new ArrayList<>();
        public final ArrayList<String> nsfwLabels = new ArrayList<>();
    }

    public static FilterLists setupFilterLists(Context context, String subreddit) {
        FilterLists lists = new FilterLists();
        String sub = subreddit.toLowerCase(Locale.ENGLISH);

        // Add regular content types
        lists.regularList.add(!PostMatch.isAlbums(sub));
        lists.regularList.add(!PostMatch.isGallery(sub));
        lists.regularList.add(!PostMatch.isGif(sub));
        lists.regularList.add(!PostMatch.isImage(sub));
        lists.regularList.add(!PostMatch.isLinks(sub));
        lists.regularList.add(!PostMatch.isSelftext(sub));
        lists.regularList.add(!PostMatch.isVideo(sub));

        lists.regularLabels.add(context.getString(R.string.type_albums));
        lists.regularLabels.add(context.getString(R.string.type_gallery));
        lists.regularLabels.add(context.getString(R.string.type_gifs));
        lists.regularLabels.add(context.getString(R.string.images));
        lists.regularLabels.add(context.getString(R.string.type_links));
        lists.regularLabels.add(context.getString(R.string.type_selftext));
        lists.regularLabels.add(context.getString(R.string.type_videos));

        // Add NSFW content types if enabled
        if (SettingValues.showNSFWContent) {
            lists.nsfwList.add(!PostMatch.isNsfwGallery(sub));
            lists.nsfwList.add(!PostMatch.isNsfwGif(sub));
            lists.nsfwList.add(!PostMatch.isNsfwImage(sub));
            lists.nsfwList.add(!PostMatch.isNsfwLink(sub));
            lists.nsfwList.add(!PostMatch.isNsfwSelftext(sub));

            lists.nsfwLabels.add(context.getString(R.string.type_nsfw_gallery));
            lists.nsfwLabels.add(context.getString(R.string.type_nsfw_gifs));
            lists.nsfwLabels.add(context.getString(R.string.type_nsfw_images));
            lists.nsfwLabels.add(context.getString(R.string.type_nsfw_links));
            lists.nsfwLabels.add(context.getString(R.string.type_nsfw_selftext));
        }

        return lists;
    }

    public static void setupListViews(Context context, ListView regularListView, ListView nsfwListView,
                                    FilterLists lists) {
        ArrayAdapter<String> regularAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_multiple_choice, lists.regularLabels);
        regularListView.setAdapter(regularAdapter);

        // Set initial regular selections
        for (int i = 0; i < lists.regularList.size(); i++) {
            regularListView.setItemChecked(i, lists.regularList.get(i));
        }

        // Always set NSFW list visibility based on settings
        nsfwListView.setVisibility(SettingValues.showNSFWContent ? View.VISIBLE : View.GONE);

        // Set up NSFW list if enabled
        if (SettingValues.showNSFWContent) {
            ArrayAdapter<String> nsfwAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_list_item_multiple_choice, lists.nsfwLabels);
            nsfwListView.setAdapter(nsfwAdapter);

            // Set initial NSFW selections
            for (int i = 0; i < lists.nsfwList.size(); i++) {
                nsfwListView.setItemChecked(i, lists.nsfwList.get(i));
            }
        } else {
            // Clear adapter when NSFW is disabled
            nsfwListView.setAdapter(null);
        }
    }

    public static boolean[] getCombinedChoices(ListView regularListView, ListView nsfwListView,
                                             FilterLists lists) {
        ArrayList<Boolean> allChosen = new ArrayList<>();

        // Get regular selections
        for (int i = 0; i < lists.regularList.size(); i++) {
            allChosen.add(!regularListView.isItemChecked(i));
        }

        // Get NSFW selections if enabled
        if (SettingValues.showNSFWContent) {
            for (int i = 0; i < lists.nsfwList.size(); i++) {
                allChosen.add(!nsfwListView.isItemChecked(i));
            }
        }

        // Convert to array
        boolean[] chosen = new boolean[allChosen.size()];
        for (int i = 0; i < allChosen.size(); i++) {
            chosen[i] = allChosen.get(i);
        }

        return chosen;
    }
}