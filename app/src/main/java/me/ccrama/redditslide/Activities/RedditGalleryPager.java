package me.edgan.redditslide.Activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.cocosw.bottomsheet.BottomSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.edgan.redditslide.Adapters.ImageGridAdapter;
import me.edgan.redditslide.Fragments.BlankFragment;
import me.edgan.redditslide.Fragments.FolderChooserDialogCreate;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.Notifications.ImageDownloadNotificationService;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Views.ToolbarColorizeHelper;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.DialogUtil;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.NetworkUtil;
import me.edgan.redditslide.util.ShareUtil;
import me.edgan.redditslide.Views.ExoVideoView;
import me.edgan.redditslide.util.GifUtils;
import me.edgan.redditslide.util.LogUtil;

import static me.edgan.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;

/**
 * Created by ccrama on 11/7/2020. <p/> This is an extension of RedditAlbum.java which utilizes a
 * ViewPager for Reddit Gallery content instead of a RecyclerView (horizontal vs vertical).
 */
public class RedditGalleryPager extends FullScreenActivity
        implements FolderChooserDialogCreate.FolderCallback {

    private static int adapterPosition;
    public static final String SUBREDDIT = "subreddit";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }
        if (id == R.id.vertical) {
            SettingValues.albumSwipe = false;
            SettingValues.prefs.edit().putBoolean(SettingValues.PREF_ALBUM_SWIPE, false).apply();
            Intent i = new Intent(RedditGalleryPager.this, RedditGallery.class);
            if (getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(MediaView.SUBMISSION_URL,
                        getIntent().getStringExtra(MediaView.SUBMISSION_URL));
            }
            if(getIntent().hasExtra(SUBREDDIT)){
                i.putExtra(SUBREDDIT, getIntent().getStringExtra(SUBREDDIT));
            }
            if (submissionTitle != null) i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
            i.putExtras(getIntent());
            Bundle urlsBundle = new Bundle();
            urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, new ArrayList<GalleryImage>(images));
            i.putExtras(urlsBundle);

            startActivity(i);
            finish();
        }
        if (id == R.id.grid) {
            mToolbar.findViewById(R.id.grid).callOnClick();
        }
        if (id == R.id.external) {
            LinkUtil.openExternally(getIntent().getExtras().getString("url", ""));
        }

        if (id == R.id.comments) {
            int adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
            finish();
            SubmissionsView.datachanged(adapterPosition);
            //getIntent().getStringExtra(MediaView.SUBMISSION_SUBREDDIT));
            //SubmissionAdapter.setOpen(this, getIntent().getStringExtra(MediaView.SUBMISSION_URL));
        }

        if (id == R.id.download && images != null) {
            int index = 0;
            for (final GalleryImage elem : images) {
                doImageSave(false, elem.url, index);
                index++;
            }
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

    public String subreddit;
    private String submissionTitle;

    public void onCreate(Bundle savedInstanceState) {
        overrideSwipeFromAnywhere();
        super.onCreate(savedInstanceState);
        getTheme().applyStyle(
                new ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
                true);
        setContentView(R.layout.album_pager);

        //Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(getIntent().hasExtra(SUBREDDIT)){
            this.subreddit = getIntent().getStringExtra(SUBREDDIT);
        }
        if (getIntent().hasExtra(EXTRA_SUBMISSION_TITLE)) {
            this.submissionTitle = getIntent().getExtras().getString(EXTRA_SUBMISSION_TITLE);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.type_album);
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setPopupTheme(
                new ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE));

        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);

        String url = getIntent().getExtras().getString("url", "");

        if (!Reddit.appRestart.contains("tutorialSwipe")) {
            startActivityForResult(new Intent(this, SwipeTutorial.class), 3);
        }

        findViewById(R.id.progress).setVisibility(View.GONE);
        images = (ArrayList<GalleryImage>)
                getIntent().getSerializableExtra(RedditGallery.GALLERY_URLS);

        p = (ViewPager) findViewById(R.id.images_horizontal);
        p.setOffscreenPageLimit(2);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(1 + "/" + images.size());
        }

        GalleryViewPagerAdapter adapter = new GalleryViewPagerAdapter(getSupportFragmentManager());
        p.setAdapter(adapter);
        p.setCurrentItem(1);
        p.post(new Runnable() {
            @Override
            public void run() {
                // Force load first two positions
                adapter.instantiateItem(p, 0);
                adapter.instantiateItem(p, 1);
            }
        });
        findViewById(R.id.grid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater l = getLayoutInflater();
                View body = l.inflate(R.layout.album_grid_dialog, null, false);
                GridView gridview = body.findViewById(R.id.images);
                gridview.setAdapter(new ImageGridAdapter(RedditGalleryPager.this, true, images));

                final AlertDialog.Builder builder = new AlertDialog.Builder(RedditGalleryPager.this)
                        .setView(body);
                final Dialog d = builder.create();
                gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position,
                            long id) {
                        p.setCurrentItem(position + 1);
                        d.dismiss();
                    }
                });
                d.show();
            }
        });
        p.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {
                if (position != 0) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle((position) + "/" + images.size());
                    }
                }
                if (position == 0 && positionOffset < 0.2) {
                    finish();
                }
            }
        });
        adapter.notifyDataSetChanged();

    }

    ViewPager p;

    private List<GalleryImage> images;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.album_pager, menu);
        adapterPosition = getIntent().getIntExtra(MediaView.ADAPTER_POSITION, -1);
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).setVisible(false);
        }
        return true;
    }

    private class GalleryViewPagerAdapter extends FragmentStatePagerAdapter {

        GalleryViewPagerAdapter(FragmentManager m) {
            super(m, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                return new BlankFragment();
            }
            i--;

            GalleryImage current = images.get(i);

            if (current.isAnimated()) {
                Fragment f = new Gif();
                Bundle args = new Bundle();
                args.putInt("page", i);
                f.setArguments(args);
                return f;
            } else {
                Fragment f = new ImageFullNoSubmission();
                Bundle args = new Bundle();
                args.putInt("page", i);
                f.setArguments(args);
                return f;
            }
        }

        @Override
        public int getCount() {
            if (images == null) {
                return 0;
            }
            return images.size() + 1;
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            rootView = (ViewGroup) inflater.inflate(R.layout.submission_gifcard_album, container, false);
            loader = rootView.findViewById(R.id.gifprogress);
            gif = rootView.findViewById(R.id.gif);

            gif.setVisibility(View.VISIBLE);
            final ExoVideoView v = (ExoVideoView) gif;
            v.clearFocus();

            GalleryImage current = ((RedditGalleryPager) getActivity()).images.get(i);
            final String url = current.getImageUrl(); // Now uses metadata-aware URL getter

            LogUtil.i(url);

            new GifUtils.AsyncLoadGif(getActivity(),
                rootView.findViewById(R.id.gif),
                loader,
                null,
                null,
                false,
                true,
                rootView.findViewById(R.id.size),
                ((RedditGalleryPager)getActivity()).subreddit,
                getActivity().getIntent().getStringExtra(EXTRA_SUBMISSION_TITLE)
            ).execute(url);

            rootView.findViewById(R.id.more).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((RedditGalleryPager) getActivity()).showBottomSheetImage(url, true, i);
                }
            });
            rootView.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MediaView.doOnClick.run();
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

    public void showBottomSheetImage(final String contentUrl, final boolean isGif,
            final int index) {

        int[] attrs = new int[]{R.attr.tintColor};
        TypedArray ta = obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable external = getResources().getDrawable(R.drawable.ic_open_in_browser);
        Drawable share = getResources().getDrawable(R.drawable.ic_share);
        Drawable image = getResources().getDrawable(R.drawable.ic_image);
        Drawable save = getResources().getDrawable(R.drawable.ic_download);

        final List<Drawable> drawableSet = Arrays.asList(external, share, image, save);
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

        ta.recycle();
        BottomSheet.Builder b = new BottomSheet.Builder(this).title(contentUrl);

        b.sheet(2, external, getString(R.string.open_externally));
        b.sheet(5, share, getString(R.string.submission_link_share));
        if (!isGif) b.sheet(3, image, getString(R.string.share_image));
        b.sheet(4, save, getString(R.string.submission_save_image));
        b.listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case (2): {
                        LinkUtil.openExternally(contentUrl);
                    }
                    break;
                    case (3): {
                        ShareUtil.shareImage(contentUrl, RedditGalleryPager.this);
                    }
                    break;
                    case (5): {
                        Reddit.defaultShareText("", contentUrl, RedditGalleryPager.this);
                    }
                    break;
                    case (4): {
                        doImageSave(isGif, contentUrl, index);
                    }
                    break;
                }
            }
        });

        b.show();

    }

    public void doImageSave(boolean isGif, String contentUrl, int index) {
        if (!isGif) {
            if (Reddit.appRestart.getString("imagelocation", "").isEmpty()) {
                showFirstDialog();
            } else if (!new File(Reddit.appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog();
            } else {
                Intent i = new Intent(this, ImageDownloadNotificationService.class);
                i.putExtra("actuallyLoaded", contentUrl);
                if (subreddit != null && !subreddit.isEmpty()) i.putExtra("subreddit", subreddit);
                if (submissionTitle != null) i.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                i.putExtra("index", index);
                startService(i);
            }
        } else {
            MediaView.doOnClick.run();
        }
    }

    public static class ImageFullNoSubmission extends Fragment {

        private int i = 0;

        public ImageFullNoSubmission() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final ViewGroup rootView =
                    (ViewGroup) inflater.inflate(R.layout.album_image_pager, container, false);

            final GalleryImage current = ((RedditGalleryPager) getActivity()).images.get(i);
            final String url = current.url;
            boolean lq = false;
            if (SettingValues.loadImageLq && (SettingValues.lowResAlways || (!NetworkUtil.isConnectedWifi(getActivity())
                    && SettingValues.lowResMobile))) {
                String lqurl = url.substring(0, url.lastIndexOf("."))
                        + (SettingValues.lqLow ? "m" : (SettingValues.lqMid ? "l" : "h"))
                        + url.substring(url.lastIndexOf("."));
                AlbumPager.loadImage(rootView, this, lqurl, ((RedditGalleryPager) getActivity()).images.size() == 1);
                lq = true;
            } else {
                AlbumPager.loadImage(rootView, this, url, ((RedditGalleryPager) getActivity()).images.size() == 1);
            }

            {
                rootView.findViewById(R.id.more).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((RedditGalleryPager) getActivity()).showBottomSheetImage(url, false, i);
                    }
                });
                {
                    rootView.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v2) {
                            ((RedditGalleryPager) getActivity()).doImageSave(false, url, i);
                        }

                    });
                    if (!SettingValues.imageDownloadButton) {
                        rootView.findViewById(R.id.save).setVisibility(View.INVISIBLE);
                    }
                }

                rootView.findViewById(R.id.panel).setVisibility(View.GONE);
                (rootView.findViewById(R.id.margin)).setPadding(0, 0, 0, 0);
            }

            rootView.findViewById(R.id.hq).setVisibility(View.GONE);

            if (getActivity().getIntent().hasExtra(MediaView.SUBMISSION_URL)) {
                rootView.findViewById(R.id.comments).setOnClickListener(new View.OnClickListener() {
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
        runOnUiThread(() ->
                DialogUtil.showFirstDialog(RedditGalleryPager.this));
    }

    private void showErrorDialog() {
        runOnUiThread(() ->
                DialogUtil.showErrorDialog(RedditGalleryPager.this));
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialogCreate dialog,
                                  @NonNull File folder, boolean isSaveToLocation) {
        Reddit.appRestart.edit().putString("imagelocation", folder.getAbsolutePath()).apply();
        Toast.makeText(this,
                getString(R.string.settings_set_image_location, folder.getAbsolutePath())
                        + folder.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialogCreate dialog) {
    }
}
