package me.edgan.redditslide.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import me.edgan.redditslide.Adapters.TumblrView;
import me.edgan.redditslide.Fragments.BlankFragment;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.Notifications.ImageDownloadNotificationService;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Tumblr.Photo;
import me.edgan.redditslide.Tumblr.TumblrUtils;
import me.edgan.redditslide.Views.PreCachingLayoutManager;
import me.edgan.redditslide.Views.ToolbarColorizeHelper;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.StorageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ccrama on 9/7/2016.
 *
 * <p>This class is responsible for accessing the Tumblr api to get the image-related json data from
 * a URL. It extends FullScreenActivity and supports swipe from anywhere.
 */
public class Tumblr extends BaseSaveActivity {
    public static final String EXTRA_URL = "url";
    private List<Photo> images;
    public static final String SUBREDDIT = "subreddit";
    private int adapterPosition;
    public String subreddit;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }
        if (id == R.id.slider) {
            SettingValues.albumSwipe = true;
            SettingValues.prefs.edit().putBoolean(SettingValues.PREF_ALBUM_SWIPE, true).apply();
            Intent i = new Intent(Tumblr.this, TumblrPager.class);
            int adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
            i.putExtra(MediaView.ADAPTER_POSITION, adapterPosition);
            if (getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                        MediaView.SUBMISSION_URL,
                        getIntent().getStringExtra(MediaView.SUBMISSION_URL));
            }
            if (getIntent().hasExtra(SUBREDDIT)) {
                i.putExtra(SUBREDDIT, getIntent().getStringExtra(SUBREDDIT));
            }

            i.putExtra("url", url);
            startActivity(i);
            finish();
        }
        if (id == R.id.grid) {
            mToolbar.findViewById(R.id.grid).callOnClick();
        }
        if (id == R.id.comments) {
            SubmissionsView.datachanged(adapterPosition);
            finish();
        }
        if (id == R.id.external) {
            LinkUtil.openExternally(url);
        }
        if (id == R.id.download) {
            for (final Photo elem : images) {
                doImageSave(false, elem.getOriginalSize().getUrl());
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public String url;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.album_vertical, menu);
        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).setVisible(false);
        }
        return true;
    }

    public TumblrPagerAdapter album;

    public void onCreate(Bundle savedInstanceState) {
        overrideSwipeFromAnywhere();
        super.onCreate(savedInstanceState);
        getTheme()
                .applyStyle(
                        new ColorPreferences(this)
                                .getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
                        true);
        setContentView(R.layout.album);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final ViewPager pager = (ViewPager) findViewById(R.id.images);

        album = new TumblrPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(album);
        pager.setCurrentItem(1);
        if (getIntent().hasExtra(SUBREDDIT)) {
            subreddit = getIntent().getStringExtra(SUBREDDIT);
        }

        if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
            this.submissionTitle = getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE);
        }

        if (SettingValues.oldSwipeMode) {
            pager.addOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageScrolled(
                                int position, float positionOffset, int positionOffsetPixels) {
                            if (position == 0 && positionOffsetPixels == 0) {
                                finish();
                            }
                            if (position == 0
                                    && ((TumblrPagerAdapter) pager.getAdapter()).blankPage
                                            != null) {
                                if (((TumblrPagerAdapter) pager.getAdapter()).blankPage != null) {
                                    ((TumblrPagerAdapter) pager.getAdapter())
                                            .blankPage.doOffset(positionOffset);
                                }
                                ((TumblrPagerAdapter) pager.getAdapter())
                                        .blankPage.realBack.setBackgroundColor(
                                                Palette.adjustAlpha(positionOffset * 0.7f));
                            }
                        }
                    });
        } else {
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {});
        }
    }

    public static class TumblrPagerAdapter extends FragmentStatePagerAdapter {
        public AlbumFrag album;
        public BlankFragment blankPage;

        public TumblrPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            if (SettingValues.oldSwipeMode) {
                if (i == 0) {
                    blankPage = new BlankFragment();
                    return blankPage;
                }
            }

            album = new AlbumFrag();

            return album;
        }

        @Override
        public int getCount() {
            int count = 1;

            if (SettingValues.oldSwipeMode) {
                count = 2;
            }

            return count;
        }
    }

    public static class AlbumFrag extends Fragment {
        View rootView;
        public RecyclerView recyclerView;

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_verticalalbum, container, false);

            final PreCachingLayoutManager mLayoutManager =
                    new PreCachingLayoutManager(getActivity());
            recyclerView = rootView.findViewById(R.id.images);
            recyclerView.setLayoutManager(mLayoutManager);
            ((Tumblr) getActivity()).url =
                    getActivity().getIntent().getExtras().getString(EXTRA_URL, "");

            new LoadIntoRecycler(((Tumblr) getActivity()).url, getActivity())
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            ((Tumblr) getActivity()).mToolbar = rootView.findViewById(R.id.toolbar);
            ((Tumblr) getActivity()).mToolbar.setTitle(R.string.type_album);
            ToolbarColorizeHelper.colorizeToolbar(
                    ((Tumblr) getActivity()).mToolbar, Color.WHITE, (getActivity()));
            ((Tumblr) getActivity()).setSupportActionBar(((Tumblr) getActivity()).mToolbar);
            ((Tumblr) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            ((Tumblr) getActivity())
                    .mToolbar.setPopupTheme(
                            new ColorPreferences(getActivity())
                                    .getDarkThemeSubreddit(ColorPreferences.FONT_STYLE));
            return rootView;
        }

        public class LoadIntoRecycler extends TumblrUtils.GetTumblrPostWithCallback {

            String url;

            public LoadIntoRecycler(@NonNull String url, @NonNull Activity baseActivity) {
                super(url, baseActivity);
                this.url = url;
            }

            @Override
            public void onError() {
                Intent i = new Intent(getActivity(), Website.class);
                i.putExtra(LinkUtil.EXTRA_URL, url);
                startActivity(i);
                getActivity().finish();
            }

            @Override
            public void doWithData(final List<Photo> jsonElements) {
                super.doWithData(jsonElements);
                if (getActivity() != null) {
                    getActivity().findViewById(R.id.progress).setVisibility(View.GONE);
                    ((Tumblr) getActivity()).images = new ArrayList<>(jsonElements);
                    TumblrView adapter =
                            new TumblrView(
                                    baseActivity,
                                    ((Tumblr) getActivity()).images,
                                    getActivity().findViewById(R.id.toolbar).getHeight(),
                                    ((Tumblr) getActivity()).subreddit);
                    recyclerView.setAdapter(adapter);
                }
            }
        }
    }

    public void doImageSave(boolean isGif, String contentUrl) {
        if (!isGif) {
            Uri storageUri = StorageUtil.getStorageUri(this);
            if (storageUri == null) {
                StorageUtil.showDirectoryChooser(this);
            } else {
                Intent i = new Intent(this, ImageDownloadNotificationService.class);
                i.putExtra("actuallyLoaded", contentUrl);
                i.putExtra("downloadUri", storageUri.toString());

                if (subreddit != null && !subreddit.isEmpty()) {
                    i.putExtra("subreddit", subreddit);
                }
                startService(i);
            }
        } else {
            MediaView.doOnClick.run();
        }
    }
}
