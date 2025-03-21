package me.edgan.redditslide;

import android.content.Context;

import me.edgan.redditslide.Adapters.SubmissionDisplay;

import net.dean.jraw.models.Submission;

import java.util.List;

/**
 * This interface provides methods for loading and retrieving submissions (such as subreddit or
 * multireddit submissions) to be called by views which require a minimal amount of functionality.
 */
public interface PostLoader {
    /**
     * Load more submissions, which will be available in the {@link #getPosts()} method.
     *
     * @param context context to get connectivity information
     * @param display the object that is displaying the view
     * @param reset whether to reset the posts or add onto the existing set
     */
    void loadMore(Context context, SubmissionDisplay display, boolean reset);

    /**
     * Get all currently loaded posts
     *
     * @return
     */
    List<Submission> getPosts();

    /**
     * Returns whether there are more posts to load.
     *
     * @return
     */
    boolean hasMore();
}
