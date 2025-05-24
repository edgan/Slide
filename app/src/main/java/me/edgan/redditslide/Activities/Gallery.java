package me.edgan.redditslide.Activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import me.edgan.redditslide.Adapters.GalleryView;
import me.edgan.redditslide.Adapters.MultiredditPosts;
import me.edgan.redditslide.Adapters.SubmissionDisplay;
import me.edgan.redditslide.Adapters.SubredditPosts;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.ContentType;
import me.edgan.redditslide.OfflineSubreddit;
import me.edgan.redditslide.PostLoader;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Views.CatchStaggeredGridLayoutManager;
import me.edgan.redditslide.util.LayoutUtils;
import me.edgan.redditslide.util.MiscUtil;

import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.List;

/** Created by ccrama on 9/17/2015. */
public class Gallery extends FullScreenActivity implements SubmissionDisplay {
    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_PAGE = "page";
    public static final String EXTRA_SUBREDDIT = "subreddit";
    public static final String EXTRA_MULTIREDDIT = "multireddit";
    public PostLoader subredditPosts;
    public String subreddit;

    public ArrayList<Submission> baseSubs;

    @Override
    public void onCreate(Bundle savedInstance) {
        overrideSwipeFromAnywhere();
        subreddit = getIntent().getExtras().getString(EXTRA_SUBREDDIT);
        String multireddit = getIntent().getExtras().getString(EXTRA_MULTIREDDIT);
        String profile = getIntent().getExtras().getString(EXTRA_PROFILE, "");
        if (multireddit != null) {
            subredditPosts = new MultiredditPosts(multireddit, profile);
        } else {
            subredditPosts = new SubredditPosts(subreddit, Gallery.this);
        }
        subreddit = multireddit == null ? subreddit : ("multi" + multireddit);

        applyDarkColorTheme(subreddit);
        super.onCreate(savedInstance);
        setContentView(R.layout.gallery);

        MiscUtil.setupOldSwipeModeBackground(this, getWindow().getDecorView());

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        long offline = getIntent().getLongExtra("offline", 0L);

        final OfflineSubreddit submissions =
                OfflineSubreddit.getSubreddit(subreddit, offline, !Authentication.didOnline, this);

        baseSubs = new ArrayList<>();

        for (Submission s : submissions.submissions) {
            if (s.getThumbnails() != null && s.getThumbnails().getSource() != null) {
                baseSubs.add(s);
            } else if (ContentType.getContentType(s) == ContentType.Type.IMAGE) {
                baseSubs.add(s);
            }
            subredditPosts.getPosts().add(s);
        }

        rv = (RecyclerView) findViewById(R.id.content_view);
        recyclerAdapter = new GalleryView(this, baseSubs, subreddit);
        RecyclerView.LayoutManager layoutManager =
                createLayoutManager(
                        LayoutUtils.getNumColumns(
                                getResources().getConfiguration().orientation, Gallery.this));
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(recyclerAdapter);
        rv.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int[] firstVisibleItems =
                                ((CatchStaggeredGridLayoutManager) rv.getLayoutManager())
                                        .findFirstVisibleItemPositions(null);
                        if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                            for (int firstVisibleItem : firstVisibleItems) {
                                pastVisiblesItems = firstVisibleItem;
                            }
                        }

                        if ((visibleItemCount + pastVisiblesItems) + 5 >= totalItemCount) {
                            if (subredditPosts instanceof SubredditPosts) {
                                if (!((SubredditPosts) subredditPosts).loading) {
                                    ((SubredditPosts) subredditPosts).loading = true;
                                    ((SubredditPosts) subredditPosts)
                                            .loadMore(Gallery.this, Gallery.this, false, subreddit);
                                }
                            } else if (subredditPosts instanceof MultiredditPosts) {
                                if (!((MultiredditPosts) subredditPosts).loading) {
                                    ((MultiredditPosts) subredditPosts).loading = true;
                                    (subredditPosts).loadMore(Gallery.this, Gallery.this, false);
                                }
                            }
                        }
                    }
                });
    }

    GalleryView recyclerAdapter;
    public int pastVisiblesItems;
    public int visibleItemCount;
    public int totalItemCount;
    RecyclerView rv;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final int currentOrientation = newConfig.orientation;

        final CatchStaggeredGridLayoutManager mLayoutManager =
                (CatchStaggeredGridLayoutManager) rv.getLayoutManager();

        mLayoutManager.setSpanCount(LayoutUtils.getNumColumns(currentOrientation, Gallery.this));
    }

    @Override
    public void updateSuccess(final List<Submission> submissions, final int startIndex) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        int startSize = baseSubs.size();
                        for (Submission s : submissions) {
                            if (!baseSubs.contains(s)
                                    && s.getThumbnails() != null
                                    && s.getThumbnails().getSource() != null) {
                                baseSubs.add(s);
                            }
                        }
                        recyclerAdapter.notifyItemRangeInserted(
                                startSize, baseSubs.size() - startSize);
                    }
                });
    }

    @Override
    public void updateOffline(List<Submission> submissions, final long cacheTime) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        recyclerAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void updateOfflineError() {}

    @Override
    public void updateError() {}

    @Override
    public void updateViews() {
        recyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAdapterUpdated() {
        recyclerAdapter.notifyDataSetChanged();
    }

    @NonNull
    private RecyclerView.LayoutManager createLayoutManager(final int numColumns) {
        return new CatchStaggeredGridLayoutManager(
                numColumns, CatchStaggeredGridLayoutManager.VERTICAL);
    }
}
