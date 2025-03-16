package me.edgan.redditslide.Activities;

import java.util.List;

/**
 * Interface to provide common functionality for gallery parent activities.
 * Implemented by both RedditGallery and RedditGalleryPager to share code.
 */
public interface GalleryParent {
    List<GalleryImage> getGalleryImages();
    String getGallerySubreddit();
    String getGallerySubmissionTitle();
    void showGalleryBottomSheet(String url, boolean isGif, int position);
    void saveGalleryMedia(boolean isGif, String url, int position);
}