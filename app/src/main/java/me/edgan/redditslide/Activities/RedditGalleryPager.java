package me.edgan.redditslide.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.cocosw.bottomsheet.BottomSheet;

import me.edgan.redditslide.Adapters.ImageGridAdapter;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.Notifications.ImageDownloadNotificationService;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Views.ExoVideoView;
import me.edgan.redditslide.Views.ToolbarColorizeHelper;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.DialogUtil;
import me.edgan.redditslide.util.GifUtils;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.LogUtil;
import me.edgan.redditslide.util.NetworkUtil;
import me.edgan.redditslide.util.ShareUtil;
import me.edgan.redditslide.util.StorageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays gallery content in a horizontal paging view. This class extends BaseSaveActivity to use
 * the Storage Access Framework for saving images, replacing the old file-based approach.
 */
public class RedditGalleryPager extends BaseSaveActivity {

    private static int adapterPosition;
    public static final String SUBREDDIT = "subreddit";
    ViewPager p;
    public List<GalleryImage> images;
    private BottomSheet.Builder bottomSheetBuilder;
    private String lastContentUrl; // Track URL for retry after permission
    private int lastIndex = -1; // Track index for retry after permission

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.vertical:
                SettingValues.albumSwipe = false;
                SettingValues.prefs
                        .edit()
                        .putBoolean(SettingValues.PREF_ALBUM_SWIPE, false)
                        .apply();

                Intent i = new Intent(RedditGalleryPager.this, RedditGallery.class);
                if (getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                    i.putExtra(
                            MediaView.SUBMISSION_URL,
                            getIntent().getStringExtra(MediaView.SUBMISSION_URL));
                }
                if (getIntent().hasExtra(SUBREDDIT)) {
                    i.putExtra(SUBREDDIT, getIntent().getStringExtra(SUBREDDIT));
                }
                if (submissionTitle != null) {
                    i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                }
                i.putExtras(getIntent());

                Bundle urlsBundle = new Bundle();
                urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, new ArrayList<>(images));
                i.putExtras(urlsBundle);

                startActivity(i);
                finish();
                return true;

            case R.id.grid:
                showGridView();
                return true;

            case R.id.external:
                LinkUtil.openExternally(getIntent().getExtras().getString("url", ""));
                return true;

            case R.id.comments:
                int adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
                finish();
                SubmissionsView.datachanged(adapterPosition);
                return true;

            case R.id.download:
                if (images != null) {
                    int index = 0;
                    for (final GalleryImage elem : images) {
                        if (elem.isAnimated()) {
                            // Handle videos/GIFs using GifUtils
                            GifUtils.cacheSaveGif(
                                Uri.parse(elem.url),
                                this,
                                subreddit != null ? subreddit : "",
                                submissionTitle != null ? submissionTitle : "",
                                true);
                        } else {
                            // Handle static images using existing image download
                            doImageSave(false, elem.url, index);
                        }
                        index++;
                    }
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3) {
            Reddit.appRestart.edit().putBoolean("tutorialSwipe", true).apply();
        }
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
        setContentView(R.layout.album_pager);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getIntent().hasExtra(SUBREDDIT)) {
            this.subreddit = getIntent().getStringExtra(SUBREDDIT);
        }

        if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
            this.submissionTitle = getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE);
        }

        setupToolbar();

        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);

        if (!Reddit.appRestart.contains("tutorialSwipe")) {
            startActivityForResult(new Intent(this, SwipeTutorial.class), 3);
        }

        findViewById(R.id.progress).setVisibility(View.GONE);
        images =
                (ArrayList<GalleryImage>)
                        getIntent().getSerializableExtra(RedditGallery.GALLERY_URLS);

        p = (ViewPager) findViewById(R.id.images_horizontal);
        p.setOffscreenPageLimit(2);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(1 + "/" + images.size());
        }

        GalleryViewPagerAdapter adapter = new GalleryViewPagerAdapter(getSupportFragmentManager());
        p.setAdapter(adapter);

        p.post(
                new Runnable() {
                    @Override
                    public void run() {
                        // Force load first two positions
                        adapter.instantiateItem(p, 0);
                        adapter.instantiateItem(p, 1);
                    }
                });

        findViewById(R.id.grid)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                LayoutInflater l = getLayoutInflater();
                                View body = l.inflate(R.layout.album_grid_dialog, null, false);
                                GridView gridview = body.findViewById(R.id.images);
                                gridview.setAdapter(
                                        new ImageGridAdapter(
                                                RedditGalleryPager.this, true, images));

                                final AlertDialog.Builder builder =
                                        new AlertDialog.Builder(RedditGalleryPager.this)
                                                .setView(body);
                                final Dialog d = builder.create();
                                gridview.setOnItemClickListener(
                                        new AdapterView.OnItemClickListener() {
                                            public void onItemClick(
                                                    AdapterView<?> parent,
                                                    View v,
                                                    int position,
                                                    long id) {
                                                p.setCurrentItem(position + 1);
                                                d.dismiss();
                                            }
                                        });
                                d.show();
                            }
                        });

        p.setCurrentItem(0);

        setupGridAndPagerListeners(adapter);
    }

    private void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.type_gallery);
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setPopupTheme(
                new ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE));
    }

    private void setupGridAndPagerListeners(final GalleryViewPagerAdapter adapter) {
        findViewById(R.id.grid).setOnClickListener(v -> showGridView());

        p.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle((position + 1) + "/" + images.size());
                        }
                    }
                });
        adapter.notifyDataSetChanged();
    }

    private void showGridView() {
        LayoutInflater l = getLayoutInflater();
        View body = l.inflate(R.layout.album_grid_dialog, null, false);
        GridView gridview = body.findViewById(R.id.images);
        gridview.setAdapter(new ImageGridAdapter(RedditGalleryPager.this, true, images));

        final AlertDialog.Builder builder =
                new AlertDialog.Builder(RedditGalleryPager.this).setView(body);
        final Dialog d = builder.create();

        gridview.setOnItemClickListener(
                (parent, v, position, id) -> {
                    p.setCurrentItem(position);
                    d.dismiss();
                });

        d.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_pager, menu);
        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).setVisible(false);
        }
        return true;
    }

    public void showBottomSheetImage(
            final String contentUrl, final boolean isGif, final int index) {
        lastContentUrl = contentUrl; // Store for potential retry after permission grant

        int[] attrs = new int[] {R.attr.tintColor};
        TypedArray ta = obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable external = getResources().getDrawable(R.drawable.ic_open_in_browser);
        Drawable share = getResources().getDrawable(R.drawable.ic_share);
        Drawable image = getResources().getDrawable(R.drawable.ic_image);
        Drawable save = getResources().getDrawable(R.drawable.ic_download);

        final List<Drawable> drawableSet = Arrays.asList(external, share, image, save);
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

        ta.recycle();
        bottomSheetBuilder = new BottomSheet.Builder(this).title(contentUrl);

        bottomSheetBuilder.sheet(2, external, getString(R.string.open_externally));
        bottomSheetBuilder.sheet(5, share, getString(R.string.submission_link_share));
        if (!isGif) {
            bottomSheetBuilder.sheet(3, image, getString(R.string.share_image));
        }
        bottomSheetBuilder.sheet(4, save, getString(R.string.submission_save_image));

        bottomSheetBuilder.listener(
                (dialog, which) -> {
                    switch (which) {
                        case 2:
                            LinkUtil.openExternally(contentUrl);
                            break;
                        case 3:
                            ShareUtil.shareImage(contentUrl, RedditGalleryPager.this);
                            break;
                        case 5:
                            Reddit.defaultShareText("", contentUrl, RedditGalleryPager.this);
                            break;
                        case 4:
                            doImageSave(isGif, contentUrl, index);
                            break;
                    }
                });

        bottomSheetBuilder.show();
    }

    @Override
    protected void onStoragePermissionGranted() {
        if (lastContentUrl != null) {
            doImageSave(false, lastContentUrl, p.getCurrentItem());
            lastContentUrl = null;
        }
    }

    private class GalleryViewPagerAdapter extends FragmentStatePagerAdapter {
        GalleryViewPagerAdapter(FragmentManager m) {
            super(m, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            GalleryImage current = images.get(i);

            Fragment f;
            if (current.isAnimated()) {
                f = new Gif();
            } else {
                f = new ImageFullNoSubmission();
            }

            Bundle args = new Bundle();
            args.putInt("page", i);
            f.setArguments(args);
            return f;
        }

        @Override
        public int getCount() {
            if (images == null) {
                return 0;
            }
            return images.size();
        }
    }

    public static class Gif extends Fragment {
        private int i = 0;
        private View gif;
        ViewGroup rootView;
        ProgressBar loader;

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (this.isVisible()) {
                if (!isVisibleToUser) {
                    ((ExoVideoView) gif).pause();
                    gif.setVisibility(View.GONE);
                }
                if (isVisibleToUser) {
                    ((ExoVideoView) gif).play();
                    gif.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView =
                    (ViewGroup)
                            inflater.inflate(R.layout.submission_gifcard_album, container, false);
            loader = rootView.findViewById(R.id.gifprogress);
            gif = rootView.findViewById(R.id.gif);

            gif.setVisibility(View.VISIBLE);
            final ExoVideoView v = (ExoVideoView) gif;
            v.clearFocus();

            GalleryImage current = ((RedditGalleryPager) getActivity()).images.get(i);
            final String url = current.getImageUrl();

            LogUtil.i(url);

            new GifUtils.AsyncLoadGif(
                            getActivity(),
                            rootView.findViewById(R.id.gif),
                            loader,
                            null,
                            null,
                            false,
                            true,
                            rootView.findViewById(R.id.size),
                            ((RedditGalleryPager) getActivity()).subreddit,
                            getActivity().getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE))
                    .execute(url);

            rootView.findViewById(R.id.more)
                    .setOnClickListener(
                            v1 ->
                                    ((RedditGalleryPager) getActivity())
                                            .showBottomSheetImage(url, true, i));

            rootView.findViewById(R.id.save)
                    .setOnClickListener(
                            v1 -> {
                                if (url != null && getActivity() != null) {
                                    ((RedditGalleryPager) getActivity()).doImageSave(true, url, i);
                                } else if (url == null) {
                                    LogUtil.i("URL is null");
                                } else if (getActivity() == null) {
                                    LogUtil.i("getActivity is null");
                                }
                            });

            if (!SettingValues.imageDownloadButton) {
                rootView.findViewById(R.id.save).setVisibility(View.INVISIBLE);
            }
            return rootView;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle bundle = this.getArguments();
            i = bundle.getInt("page", 0);
        }
    }

    public void doImageSave(boolean isGif, String contentUrl, int index) {
        Uri storageUri = StorageUtil.getStorageUri(this);
        if (storageUri == null) {
            lastContentUrl = contentUrl;
            lastIndex = index;
            StorageUtil.showDirectoryChooser(this);
        } else {
            if (isGif) {
                // Handle video/gif save
                GifUtils.cacheSaveGif(
                        Uri.parse(contentUrl),
                        this,
                        subreddit != null ? subreddit : "",
                        submissionTitle != null ? submissionTitle : "",
                        true);
            } else {
                // Handle image save
                Intent i = new Intent(this, ImageDownloadNotificationService.class);
                i.putExtra("actuallyLoaded", contentUrl);
                i.putExtra("downloadUri", storageUri.toString());

                if (subreddit != null && !subreddit.isEmpty()) {
                    i.putExtra("subreddit", subreddit);
                }
                if (submissionTitle != null) {
                    i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                }
                i.putExtra("index", index);

                startService(i);
            }
        }
    }

    public static class ImageFullNoSubmission extends Fragment {

        private int i = 0;

        public ImageFullNoSubmission() {}

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final ViewGroup rootView =
                    (ViewGroup) inflater.inflate(R.layout.album_image_pager, container, false);

            final GalleryImage current = ((RedditGalleryPager) getActivity()).images.get(i);
            final String url = current.url;

            if (SettingValues.loadImageLq
                    && (SettingValues.lowResAlways
                            || (!NetworkUtil.isConnectedWifi(getActivity())
                                    && SettingValues.lowResMobile))) {
                String lqurl =
                        url.substring(0, url.lastIndexOf("."))
                                + (SettingValues.lqLow ? "m" : (SettingValues.lqMid ? "l" : "h"))
                                + url.substring(url.lastIndexOf("."));
                AlbumPager.loadImage(
                        rootView,
                        this,
                        lqurl,
                        ((RedditGalleryPager) getActivity()).images.size() == 1);
            } else {
                AlbumPager.loadImage(
                        rootView,
                        this,
                        url,
                        ((RedditGalleryPager) getActivity()).images.size() == 1);
            }

            rootView.findViewById(R.id.more)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ((RedditGalleryPager) getActivity())
                                            .showBottomSheetImage(url, false, i);
                                }
                            });
            rootView.findViewById(R.id.save)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v2) {
                                    ((RedditGalleryPager) getActivity()).doImageSave(false, url, i);
                                }
                            });
            if (!SettingValues.imageDownloadButton) {
                rootView.findViewById(R.id.save).setVisibility(View.INVISIBLE);
            }

            rootView.findViewById(R.id.panel).setVisibility(View.GONE);
            (rootView.findViewById(R.id.margin)).setPadding(0, 0, 0, 0);

            rootView.findViewById(R.id.hq).setVisibility(View.GONE);

            if (getActivity().getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                rootView.findViewById(R.id.comments)
                        .setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        getActivity().finish();
                                        SubmissionsView.datachanged(adapterPosition);
                                    }
                                });
            } else {
                rootView.findViewById(R.id.comments).setVisibility(View.GONE);
            }
            return rootView;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle bundle = this.getArguments();
            i = bundle.getInt("page", 0);
        }
    }

    private void showFirstDialog() {
        runOnUiThread(() -> DialogUtil.showFirstDialog(RedditGalleryPager.this));
    }

    private void showErrorDialog() {
        runOnUiThread(() -> DialogUtil.showErrorDialog(RedditGalleryPager.this));
    }
}