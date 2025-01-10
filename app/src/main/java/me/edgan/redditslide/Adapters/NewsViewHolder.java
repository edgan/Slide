package me.edgan.redditslide.Adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import me.edgan.redditslide.R;
import me.edgan.redditslide.SpoilerRobotoTextView;
import me.edgan.redditslide.SubmissionViews.HeaderImageLinkView;

/**
 * Created by ccrama on 11/19/2017.
 */
public class NewsViewHolder extends RecyclerView.ViewHolder {
    public final SpoilerRobotoTextView title;
    public final View                  menu;
    public final View                  comment;
    public final HeaderImageLinkView   leadImage;
    public final RelativeLayout        innerRelative;
    public final ImageView             thumbnail;

    public NewsViewHolder(View v) {
        super(v);
        title = v.findViewById(R.id.title);
        comment = v.findViewById(R.id.comments);
        menu = v.findViewById(R.id.more);
        leadImage = v.findViewById(R.id.headerimage);
        innerRelative = v.findViewById(R.id.innerrelative);
        thumbnail = v.findViewById(R.id.thumbimage2);
    }
}
