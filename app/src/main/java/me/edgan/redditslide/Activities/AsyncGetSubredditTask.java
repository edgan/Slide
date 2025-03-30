package me.edgan.redditslide.Activities;

import android.os.AsyncTask;
import android.util.Log;

import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.util.LogUtil;
import net.dean.jraw.models.Subreddit;

/**
 * AsyncTask to fetch Subreddit details asynchronously.
 */
public class AsyncGetSubredditTask extends AsyncTask<String, Void, Subreddit> {

    private MainActivity mainActivity;

    public AsyncGetSubredditTask(MainActivity activity) {
        this.mainActivity = activity;
    }

    @Override
    public void onPostExecute(Subreddit subreddit) {
        // Ensure mainActivity is still valid before calling its method
        if (mainActivity != null && !mainActivity.isDestroyed() && subreddit != null) {
            mainActivity.doSubOnlyStuff(subreddit);
        } else if (mainActivity != null && !mainActivity.isDestroyed()) {
             // Handle cases where subreddit is null (e.g., network error, subreddit not found)
            Log.w(LogUtil.getTag(), "Failed to fetch subreddit details or subreddit is null.");
             // Optionally, inform the user or update UI accordingly in MainActivity
             // mainActivity.handleSubredditFetchError();
        }
    }

    @Override
    protected Subreddit doInBackground(String... params) {
        if (params == null || params.length == 0 || params[0] == null || params[0].isEmpty()) {
            Log.e(LogUtil.getTag(), "Subreddit name parameter is missing or empty.");
            return null;
        }
        String subredditName = params[0];
        try {
            // Ensure Authentication.reddit is initialized
            if (Authentication.reddit == null) {
                Log.e(LogUtil.getTag(), "Authentication.reddit is not initialized.");
                return null;
            }
            return Authentication.reddit.getSubreddit(subredditName);
        } catch (Exception e) {
            Log.e(LogUtil.getTag(), "Error fetching subreddit: " + subredditName, e);
            return null;
        }
    }
}