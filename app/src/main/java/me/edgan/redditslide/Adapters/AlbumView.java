package me.edgan.redditslide.Adapters;

import static me.edgan.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devspark.robototextview.RobotoTypefaces;

import me.edgan.redditslide.Activities.Album;
import me.edgan.redditslide.Activities.MediaView;
import me.edgan.redditslide.ImgurAlbum.Image;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.SpoilerRobotoTextView;
import me.edgan.redditslide.Visuals.FontPreferences;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.SubmissionParser;

import java.util.List;

public class AlbumView extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Image> users;

    private final Activity main;

    public boolean paddingBottom;
    public int height;
    public String subreddit;
    private final String submissionTitle;

    public AlbumView(
            final Activity context,
            final List<Image> users,
            int height,
            String subreddit,
            String SubmissionTitle) {

        this.height = height;
        main = context;
        this.users = users;
        this.subreddit = subreddit;
        this.submissionTitle = SubmissionTitle;

        paddingBottom = main.findViewById(R.id.toolbar) == null;
        if (context.findViewById(R.id.grid) != null)
            context.findViewById(R.id.grid)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    LayoutInflater l = context.getLayoutInflater();
                                    View body = l.inflate(R.layout.album_grid_dialog, null, false);
                                    GridView gridview = body.findViewById(R.id.images);
                                    gridview.setAdapter(new ImageGridAdapter(context, users));

                                    final AlertDialog.Builder builder =
                                            new AlertDialog.Builder(context).setView(body);
                                    final Dialog d = builder.create();
                                    gridview.setOnItemClickListener(
                                            new AdapterView.OnItemClickListener() {
                                                public void onItemClick(
                                                        AdapterView<?> parent,
                                                        View v,
                                                        int position,
                                                        long id) {
                                                    if (context instanceof Album) {
                                                        ((LinearLayoutManager)
                                                                        ((Album) context)
                                                                                .album.album
                                                                                        .recyclerView
                                                                                        .getLayoutManager())
                                                                .scrollToPositionWithOffset(
                                                                        position + 1,
                                                                        context.findViewById(
                                                                                        R.id
                                                                                                .toolbar)
                                                                                .getHeight());

                                                    } else {
                                                        ((LinearLayoutManager)
                                                                        ((RecyclerView)
                                                                                        context
                                                                                                .findViewById(
                                                                                                        R
                                                                                                                .id
                                                                                                                .images))
                                                                                .getLayoutManager())
                                                                .scrollToPositionWithOffset(
                                                                        position + 1,
                                                                        context.findViewById(
                                                                                        R.id
                                                                                                .toolbar)
                                                                                .getHeight());
                                                    }
                                                    d.dismiss();
                                                }
                                            });
                                    d.show();
                                }
                            });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View v =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.album_image, parent, false);
            return new AlbumViewHolder(v);
        } else {
            View v =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.spacer, parent, false);
            return new SpacerViewHolder(v);
        }
    }

    public double getHeightFromAspectRatio(int imageHeight, int imageWidth, int viewWidth) {
        double ratio = (double) imageHeight / (double) imageWidth;
        return (viewWidth * ratio);
    }

    @Override
    public int getItemViewType(int position) {
        int SPACER = 6;
        if (!paddingBottom && position == 0) {
            return SPACER;
        } else if (paddingBottom && position == getItemCount() - 1) {
            return SPACER;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder2, int i) {
        if (holder2 instanceof AlbumViewHolder) {
            final int position = paddingBottom ? i : i - 1;

            AlbumViewHolder holder = (AlbumViewHolder) holder2;

            final Image user = users.get(position);
            ((Reddit) main.getApplicationContext())
                    .getImageLoader()
                    .displayImage(user.getImageUrl(), holder.image, ImageGridAdapter.options);
            holder.body.setVisibility(View.VISIBLE);
            holder.text.setVisibility(View.VISIBLE);
            View imageView = holder.image;
            if (imageView.getWidth() == 0) {
                holder.image.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT));
            } else {
                holder.image.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                (int)
                                        getHeightFromAspectRatio(
                                                user.getHeight(),
                                                user.getWidth(),
                                                imageView.getWidth())));
            }
            {
                int type =
                        new FontPreferences(holder.body.getContext())
                                .getFontTypeComment()
                                .getTypeface();
                Typeface typeface;
                if (type >= 0) {
                    typeface = RobotoTypefaces.obtainTypeface(holder.body.getContext(), type);
                } else {
                    typeface = Typeface.DEFAULT;
                }
                holder.body.setTypeface(typeface);
            }
            {
                int type =
                        new FontPreferences(holder.body.getContext())
                                .getFontTypeTitle()
                                .getTypeface();
                Typeface typeface;
                if (type >= 0) {
                    typeface = RobotoTypefaces.obtainTypeface(holder.body.getContext(), type);
                } else {
                    typeface = Typeface.DEFAULT;
                }
                holder.text.setTypeface(typeface);
            }
            {
                if (user.getTitle() != null) {
                    List<String> text = SubmissionParser.getBlocks(user.getTitle());
                    LinkUtil.setTextWithLinks(text.get(0), holder.text);
                    if (holder.text.getText().toString().isEmpty()) {
                        holder.text.setVisibility(View.GONE);
                    }

                } else {
                    holder.text.setVisibility(View.GONE);
                }
            }
            {
                if (user.getDescription() != null) {
                    List<String> text = SubmissionParser.getBlocks(user.getDescription());
                    LinkUtil.setTextWithLinks(text.get(0), holder.body);
                    if (holder.body.getText().toString().isEmpty()) {
                        holder.body.setVisibility(View.GONE);
                    }
                } else {
                    holder.body.setVisibility(View.GONE);
                }
            }

            View.OnClickListener onGifImageClickListener =
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (SettingValues.image && !user.isAnimated()
                                    || SettingValues.gif && user.isAnimated()) {
                                Intent myIntent = new Intent(main, MediaView.class);
                                myIntent.putExtra(MediaView.EXTRA_URL, user.getImageUrl());
                                myIntent.putExtra(MediaView.SUBREDDIT, subreddit);
                                if (submissionTitle != null) {
                                    myIntent.putExtra(EXTRA_SUBMISSION_TITLE, submissionTitle);
                                }
                                main.startActivity(myIntent);
                            } else {
                                LinkUtil.openExternally(user.getImageUrl());
                            }
                        }
                    };

            if (user.isAnimated()) {
                holder.body.setVisibility(View.VISIBLE);
                holder.body.setSingleLine(false);
                holder.body.setTextHtml(
                        holder.text.getText()
                                + main.getString(R.string.submission_tap_gif)
                                        .toUpperCase()); // got rid of the \n thing, because it
                // didnt parse and it was already a new
                // line so...
                holder.body.setOnClickListener(onGifImageClickListener);
            }

            holder.itemView.setOnClickListener(onGifImageClickListener);
        } else if (holder2 instanceof SpacerViewHolder) {
            holder2.itemView
                    .findViewById(R.id.height)
                    .setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    holder2.itemView.getWidth(),
                                    paddingBottom
                                            ? height
                                            : main.findViewById(R.id.toolbar).getHeight()));
        }
    }

    @Override
    public int getItemCount() {
        return users == null ? 0 : users.size() + 1;
    }

    public static class SpacerViewHolder extends RecyclerView.ViewHolder {
        public SpacerViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        final SpoilerRobotoTextView text;
        final SpoilerRobotoTextView body;
        final ImageView image;

        public AlbumViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.imagetitle);
            body = itemView.findViewById(R.id.imageCaption);
            image = itemView.findViewById(R.id.image);
        }
    }
}
