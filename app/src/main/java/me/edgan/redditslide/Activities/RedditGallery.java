package me.edgan.redditslide.Activities;

import static me.edgan.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;

import android.content.Intent;
import android.graphics.Color;
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

import me.edgan.redditslide.Adapters.RedditGalleryView;
import me.edgan.redditslide.Fragments.BlankFragment;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Views.PreCachingLayoutManager;
import me.edgan.redditslide.Views.ToolbarColorizeHelper;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.NavigationModeDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying Reddit gallery content in a vertical scrolling view. Supports downloading
 * images using the Storage Access Framework.
 */
public class RedditGallery extends BaseSaveActivity {

    public static final String SUBREDDIT = "subreddit";
    public static final String GALLERY_URLS = "galleryurls";
    private List<GalleryImage> images;
    private int adapterPosition;
    public String url;
    public String subreddit;
    private String submissionTitle;
    public RedditGalleryPagerAdapter album;

    private View rootView;
    private int navigationMode;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.slider:
                SettingValues.albumSwipe = true;
                SettingValues.prefs.edit().putBoolean(SettingValues.PREF_ALBUM_SWIPE, true).apply();

                Intent i = new Intent(RedditGallery.this, RedditGalleryPager.class);
                i.putExtra(MediaView.ADAPTER_POSITION, adapterPosition);

                if (getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                    i.putExtra(
                            MediaView.SUBMISSION_URL,
                            getIntent().getStringExtra(MediaView.SUBMISSION_URL));
                }
                if (subreddit != null && !subreddit.isEmpty()) {
                    i.putExtra(RedditGalleryPager.SUBREDDIT, subreddit);
                }
                if (submissionTitle != null) {
                    i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                }

                Bundle urlsBundle = new Bundle();
                urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, new ArrayList<>(images));
                i.putExtras(urlsBundle);

                startActivity(i);
                finish();
                return true;

            case R.id.grid:
                mToolbar.findViewById(R.id.grid).callOnClick();
                return true;

            case R.id.comments:
                SubmissionsView.datachanged(adapterPosition);
                finish();
                return true;

            case R.id.external:
                LinkUtil.openExternally(url);
                return true;

            case R.id.download:
                // Download all images in the gallery
                int index = 0;
                for (final GalleryImage elem : images) {
                    doImageSave(false, elem.url, index);
                    index++;
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        overrideSwipeFromAnywhere();
        super.onCreate(savedInstanceState);

        getTheme()
                .applyStyle(
                        new ColorPreferences(this)
                                .getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
                        true);
        setContentView(R.layout.album);

        rootView = findViewById(android.R.id.content);

        navigationMode = NavigationModeDetector.getNavigationMode(this, rootView);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getIntent().hasExtra(SUBREDDIT)) {
            this.subreddit = getIntent().getExtras().getString(SUBREDDIT);
        }
        if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
            this.submissionTitle = getIntent().getExtras().getString(EXTRA_SUBMISSION_TITLE);
        }

        final ViewPager pager = (ViewPager) findViewById(R.id.images);

        album = new RedditGalleryPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(album);
        pager.setCurrentItem(1);
        if (navigationMode == NavigationModeDetector.NAVIGATION_MODE_THREE_BUTTON) {
            pager.addOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageScrolled(
                                int position, float positionOffset, int positionOffsetPixels) {
                            if (position == 0 && positionOffsetPixels == 0) {
                                finish();
                            }
                            if (position == 0
                                    && ((RedditGalleryPagerAdapter) pager.getAdapter()).blankPage
                                            != null) {
                                if (((RedditGalleryPagerAdapter) pager.getAdapter()).blankPage
                                        != null) {
                                    ((RedditGalleryPagerAdapter) pager.getAdapter())
                                            .blankPage.doOffset(positionOffset);
                                }
                                ((RedditGalleryPagerAdapter) pager.getAdapter())
                                        .blankPage.realBack.setBackgroundColor(
                                                Palette.adjustAlpha(positionOffset * 0.7f));
                            }
                        }
                    });
        }

        configureViewPager(pager);
    }

    private void configureViewPager(final ViewPager pager) {
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {});
    }

    @Override
    protected void onStoragePermissionGranted() {
        // After getting SAF permission, retry the last attempted download if any
        if (!images.isEmpty()) {
            int index = 0;
            for (final GalleryImage elem : images) {
                doImageSave(false, elem.url, index);
                index++;
            }
        }
    }

    public class RedditGalleryPagerAdapter extends FragmentStatePagerAdapter {
        public AlbumFrag album;
        BlankFragment blankPage;

        RedditGalleryPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            if (navigationMode == NavigationModeDetector.NAVIGATION_MODE_THREE_BUTTON) {
                if (i == 0) {
                    blankPage = new BlankFragment();
                    return blankPage;
                } else {
                    album = new AlbumFrag();
                    return album;
                }
            } else {
                album = new AlbumFrag();

                return album;
            }
        }

        @Override
        public int getCount() {
            if (navigationMode == NavigationModeDetector.NAVIGATION_MODE_THREE_BUTTON) {
                return 2;
            } else {
                return 1;
            }
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

            final RedditGallery galleryActivity = (RedditGallery) getActivity();
            if (galleryActivity != null) {
                galleryActivity.images =
                        (ArrayList<GalleryImage>)
                                getActivity()
                                        .getIntent()
                                        .getSerializableExtra(RedditGallery.GALLERY_URLS);

                galleryActivity.mToolbar = rootView.findViewById(R.id.toolbar);
                galleryActivity.mToolbar.setTitle(R.string.type_gallery);

                ToolbarColorizeHelper.colorizeToolbar(
                        galleryActivity.mToolbar, Color.WHITE, getActivity());
                galleryActivity.setSupportActionBar(galleryActivity.mToolbar);
                galleryActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                galleryActivity.mToolbar.setPopupTheme(
                        new ColorPreferences(getActivity())
                                .getDarkThemeSubreddit(ColorPreferences.FONT_STYLE));

                rootView.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                rootView.findViewById(R.id.progress).setVisibility(View.GONE);
                                RedditGalleryView adapter =
                                        new RedditGalleryView(
                                                galleryActivity,
                                                galleryActivity.images,
                                                rootView.findViewById(R.id.toolbar).getHeight(),
                                                galleryActivity.subreddit,
                                                galleryActivity.submissionTitle);
                                recyclerView.setAdapter(adapter);
                            }
                        });
            }

            return rootView;
        }
    }
}
