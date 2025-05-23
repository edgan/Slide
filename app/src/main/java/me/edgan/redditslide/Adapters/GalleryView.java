package me.edgan.redditslide.Adapters;

import static me.edgan.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.cocosw.bottomsheet.BottomSheet;
import com.fasterxml.jackson.databind.JsonNode;

import me.edgan.redditslide.Activities.Album;
import me.edgan.redditslide.Activities.AlbumPager;
import me.edgan.redditslide.Activities.CommentsScreen;
import me.edgan.redditslide.Activities.FullscreenVideo;
import me.edgan.redditslide.Activities.Gallery;
import me.edgan.redditslide.Activities.GalleryImage;
import me.edgan.redditslide.Activities.MediaView;
import me.edgan.redditslide.Activities.RedditGallery;
import me.edgan.redditslide.Activities.RedditGalleryPager;
import me.edgan.redditslide.Activities.Tumblr;
import me.edgan.redditslide.Activities.TumblrPager;
import me.edgan.redditslide.ContentType;
import me.edgan.redditslide.PostMatch;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.util.SubmissionThumbnailHelper;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.CompatUtil;
import me.edgan.redditslide.util.JsonUtil;
import me.edgan.redditslide.util.LinkUtil;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GalleryView extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Gallery main;
    public boolean paddingBottom;
    public ArrayList<Submission> posts;
    public String subreddit;

    public GalleryView(final Gallery context, ArrayList<Submission> displayer, String subreddit) {
        main = context;
        this.posts = displayer;
        this.subreddit = subreddit;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.gallery_image, parent, false);
        return new AlbumViewHolder(v);
    }

    public double getHeightFromAspectRatio(int imageHeight, int imageWidth, int viewWidth) {
        double ratio = (double) imageHeight / (double) imageWidth;
        return (viewWidth * ratio);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder2, final int i) {
        if (holder2 instanceof AlbumViewHolder) {

            final AlbumViewHolder holder = (AlbumViewHolder) holder2;

            final Submission submission = posts.get(i);

            if (submission.getThumbnails() != null
                    && submission.getThumbnails().getSource() != null) {
                ((Reddit) main.getApplicationContext())
                        .getImageLoader()
                        .displayImage(
                                submission.getThumbnails().getSource().getUrl(),
                                holder.image,
                                ImageGridAdapter.options);
            } else {
                ((Reddit) main.getApplicationContext())
                        .getImageLoader()
                        .displayImage(submission.getUrl(), holder.image, ImageGridAdapter.options);
            }
            double h = 0;
            int height = 0;
            if (submission.getThumbnails() != null) {
                Thumbnails.Image source = submission.getThumbnails().getSource();
                if (source != null) {
                    h =
                            getHeightFromAspectRatio(
                                    source.getHeight(), source.getWidth(), holder.image.getWidth());
                    height = source.getHeight();
                }
            }

            holder.type.setVisibility(View.VISIBLE);
            switch (ContentType.getContentType(submission)) {
                case REDDIT_GALLERY:
                case ALBUM:
                    holder.type.setImageResource(R.drawable.ic_photo_library);
                    break;
                case EXTERNAL:
                case LINK:
                case REDDIT:
                    holder.type.setImageResource(R.drawable.ic_public);
                    break;
                case SELF:
                    holder.type.setImageResource(R.drawable.ic_text_fields);
                    break;
                case EMBEDDED:
                case GIF:
                case STREAMABLE:
                case VIDEO:
                    holder.type.setImageResource(R.drawable.ic_play_arrow);
                    break;
                default:
                    holder.type.setVisibility(View.GONE);
                    break;
            }

            if (h != 0) {
                if (h > 3200) {
                    holder.image.setLayoutParams(
                            new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT, 3200));
                } else {
                    holder.image.setLayoutParams(
                            new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT, (int) h));
                }
            } else {
                if (height > 3200) {
                    holder.image.setLayoutParams(
                            new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT, 3200));
                } else {
                    holder.image.setLayoutParams(
                            new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT));
                }
            }

            holder.comments.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            Intent i2 = new Intent(main, CommentsScreen.class);
                            i2.putExtra(
                                    CommentsScreen.EXTRA_PAGE,
                                    main.subredditPosts.getPosts().indexOf(submission));
                            i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, subreddit);
                            i2.putExtra("fullname", submission.getFullName());
                            main.startActivity(i2);
                        }
                    });

            holder.image.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {

                            if (main != null) {
                                BottomSheet.Builder b =
                                        new BottomSheet.Builder(main)
                                                .title(submission.getUrl())
                                                .grid();
                                int[] attrs = new int[] {R.attr.tintColor};
                                TypedArray ta = main.obtainStyledAttributes(attrs);

                                int color = ta.getColor(0, Color.WHITE);
                                Drawable open =
                                        main.getResources().getDrawable(R.drawable.ic_open_in_new);
                                Drawable share =
                                        main.getResources().getDrawable(R.drawable.ic_share);
                                Drawable copy =
                                        main.getResources().getDrawable(R.drawable.ic_content_copy);
                                final List<Drawable> drawableSet = Arrays.asList(open, share, copy);
                                BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

                                ta.recycle();

                                b.sheet(
                                        R.id.open_link,
                                        open,
                                        main.getResources().getString(R.string.open_externally));
                                b.sheet(
                                        R.id.share_link,
                                        share,
                                        main.getResources().getString(R.string.share_link));
                                b.sheet(
                                        R.id.copy_link,
                                        copy,
                                        main.getResources()
                                                .getString(R.string.submission_link_copy));

                                b.listener(
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialog, int which) {
                                                        switch (which) {
                                                            case R.id.open_link:
                                                                LinkUtil.openExternally(
                                                                        submission.getUrl());
                                                                break;
                                                            case R.id.share_link:
                                                                Reddit.defaultShareText(
                                                                        "",
                                                                        submission.getUrl(),
                                                                        main);
                                                                break;
                                                            case R.id.copy_link:
                                                                LinkUtil.copyUrl(
                                                                        submission.getUrl(), main);
                                                                break;
                                                        }
                                                    }
                                                })
                                        .show();
                                return true;
                            }
                            return true;
                        }
                    });

            holder.image.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ContentType.Type type = ContentType.getContentType(submission);
                            if (!PostMatch.openExternal(submission.getUrl())
                                    || type == ContentType.Type.VIDEO) {
                                switch (type) {
                                    case STREAMABLE:
                                        if (SettingValues.video) {
                                            Intent myIntent = new Intent(main, MediaView.class);
                                            myIntent.putExtra(MediaView.SUBREDDIT, subreddit);
                                            myIntent.putExtra(
                                                    MediaView.EXTRA_URL, submission.getUrl());
                                            myIntent.putExtra(
                                                    EXTRA_SUBMISSION_TITLE, submission.getTitle());
                                            main.startActivity(myIntent);
                                        } else {
                                            LinkUtil.openExternally(submission.getUrl());
                                        }
                                        break;
                                    case IMGUR:
                                    case DEVIANTART:
                                    case XKCD:
                                    case IMAGE:
                                        SubmissionThumbnailHelper.openImage(
                                                type,
                                                main,
                                                submission,
                                                null,
                                                holder.getBindingAdapterPosition());
                                        break;
                                    case EMBEDDED:
                                        if (SettingValues.video) {
                                            String data =
                                                    CompatUtil.fromHtml(
                                                                    submission
                                                                            .getDataNode()
                                                                            .get("media_embed")
                                                                            .get("content")
                                                                            .asText())
                                                            .toString();
                                            {
                                                Intent i = new Intent(main, FullscreenVideo.class);
                                                i.putExtra(FullscreenVideo.EXTRA_HTML, data);
                                                main.startActivity(i);
                                            }
                                        } else {
                                            LinkUtil.openExternally(submission.getUrl());
                                        }
                                        break;
                                    case REDDIT:
                                        SubmissionThumbnailHelper.openRedditContent(
                                                submission.getUrl(), main);
                                        break;
                                    case LINK:
                                        LinkUtil.openUrl(
                                                submission.getUrl(),
                                                Palette.getColor(submission.getSubredditName()),
                                                main);
                                        break;
                                    case ALBUM:
                                        if (SettingValues.album) {
                                            Intent i;
                                            if (SettingValues.albumSwipe) {
                                                i = new Intent(main, AlbumPager.class);
                                                i.putExtra(AlbumPager.SUBREDDIT, subreddit);
                                                i.putExtra(
                                                        EXTRA_SUBMISSION_TITLE,
                                                        submission.getTitle());
                                                i.putExtra(Album.EXTRA_URL, submission.getUrl());
                                            } else {
                                                i = new Intent(main, Album.class);
                                                i.putExtra(Album.SUBREDDIT, subreddit);
                                                i.putExtra(Album.EXTRA_URL, submission.getUrl());
                                                i.putExtra(
                                                        EXTRA_SUBMISSION_TITLE,
                                                        submission.getTitle());
                                            }
                                            main.startActivity(i);
                                        } else {
                                            LinkUtil.openExternally(submission.getUrl());
                                        }
                                        break;
                                    case REDDIT_GALLERY:
                                        if (SettingValues.album) {
                                            Intent i;
                                            if (SettingValues.albumSwipe) {
                                                i = new Intent(main, RedditGalleryPager.class);
                                                i.putExtra(
                                                        AlbumPager.SUBREDDIT,
                                                        submission.getSubredditName());
                                            } else {
                                                i = new Intent(main, RedditGallery.class);
                                                i.putExtra(
                                                        Album.SUBREDDIT,
                                                        submission.getSubredditName());
                                            }
                                            i.putExtra(
                                                    EXTRA_SUBMISSION_TITLE, submission.getTitle());

                                            i.putExtra(
                                                    RedditGallery.SUBREDDIT,
                                                    submission.getSubredditName());

                                            ArrayList<GalleryImage> urls = new ArrayList<>();

                                            JsonNode dataNode = submission.getDataNode();
                                            if (dataNode.has("gallery_data")) {
                                                JsonUtil.getGalleryData(dataNode, urls);
                                            }

                                            Bundle urlsBundle = new Bundle();
                                            urlsBundle.putSerializable(
                                                    RedditGallery.GALLERY_URLS, urls);
                                            i.putExtras(urlsBundle);

                                            main.startActivity(i);
                                        } else {
                                            LinkUtil.openExternally(submission.getUrl());
                                        }
                                        break;

                                    case TUMBLR:
                                        if (SettingValues.image) {
                                            Intent i;
                                            if (SettingValues.albumSwipe) {
                                                i = new Intent(main, TumblrPager.class);
                                                i.putExtra(TumblrPager.SUBREDDIT, subreddit);
                                            } else {
                                                i = new Intent(main, Tumblr.class);
                                                i.putExtra(Tumblr.SUBREDDIT, subreddit);
                                            }
                                            i.putExtra(Album.EXTRA_URL, submission.getUrl());
                                            main.startActivity(i);
                                        } else {
                                            LinkUtil.openExternally(submission.getUrl());
                                        }
                                        break;
                                    case GIF:
                                        SubmissionThumbnailHelper.openGif(
                                                main,
                                                submission,
                                                holder.getBindingAdapterPosition());
                                        break;
                                    case NONE:
                                    case SELF:
                                        holder.comments.callOnClick();
                                        break;
                                    case VIDEO:
                                        if (!LinkUtil.tryOpenWithVideoPlugin(submission.getUrl())) {
                                            LinkUtil.openUrl(
                                                    submission.getUrl(),
                                                    Palette.getStatusBarColor(),
                                                    main);
                                        }
                                        break;
                                }
                            } else {
                                LinkUtil.openExternally(submission.getUrl());
                            }
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final ImageView type;
        final View comments;

        public AlbumViewHolder(View itemView) {
            super(itemView);
            comments = itemView.findViewById(R.id.comments);
            image = itemView.findViewById(R.id.image);
            type = itemView.findViewById(R.id.type);
        }
    }
}
