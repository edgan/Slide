package me.edgan.redditslide.SubmissionViews;

import static me.edgan.redditslide.Notifications.ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;
import com.google.android.material.snackbar.Snackbar;

import me.edgan.redditslide.ActionStates;
import me.edgan.redditslide.Activities.Album;
import me.edgan.redditslide.Activities.AlbumPager;
import me.edgan.redditslide.Activities.FullscreenVideo;
import me.edgan.redditslide.Activities.MainActivity;
import me.edgan.redditslide.Activities.MediaView;
import me.edgan.redditslide.Activities.MultiredditOverview;
import me.edgan.redditslide.Activities.PostReadLater;
import me.edgan.redditslide.Activities.Profile;
import me.edgan.redditslide.Activities.Search;
import me.edgan.redditslide.Activities.SubredditView;
import me.edgan.redditslide.Activities.Tumblr;
import me.edgan.redditslide.Activities.TumblrPager;
import me.edgan.redditslide.Adapters.CommentAdapter;
import me.edgan.redditslide.Adapters.NewsViewHolder;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.CommentCacheAsync;
import me.edgan.redditslide.ContentType;
import me.edgan.redditslide.DataShare;
import me.edgan.redditslide.ForceTouch.PeekViewActivity;
import me.edgan.redditslide.HasSeen;
import me.edgan.redditslide.Hidden;
import me.edgan.redditslide.LastComments;
import me.edgan.redditslide.OfflineSubreddit;
import me.edgan.redditslide.OpenRedditLink;
import me.edgan.redditslide.PostMatch;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.SubmissionCache;
import me.edgan.redditslide.Views.CreateCardView;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.ClipboardUtil;
import me.edgan.redditslide.util.CompatUtil;
import me.edgan.redditslide.util.DisplayUtil;
import me.edgan.redditslide.util.GifUtils;
import me.edgan.redditslide.util.LayoutUtils;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.NetworkUtil;
import me.edgan.redditslide.util.OnSingleClickListener;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Ruleset;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditRule;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Created by ccrama on 9/19/2015. */
public class PopulateNewsViewHolder {

    public PopulateNewsViewHolder() {}

    private static void addClickFunctions(
            final View base,
            final ContentType.Type type,
            final Activity contextActivity,
            final Submission submission,
            final NewsViewHolder holder,
            final boolean full) {
        base.setOnClickListener(
                new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        if (NetworkUtil.isConnected(contextActivity)
                                || (!NetworkUtil.isConnected(contextActivity)
                                        && ContentType.fullImage(type))) {
                            if (SettingValues.storeHistory && !full) {
                                if (!submission.isNsfw() || SettingValues.storeNSFWHistory) {
                                    HasSeen.addSeen(submission.getFullName());
                                    if (contextActivity instanceof MainActivity
                                            || contextActivity instanceof MultiredditOverview
                                            || contextActivity instanceof SubredditView
                                            || contextActivity instanceof Search
                                            || contextActivity instanceof Profile) {
                                        holder.title.setAlpha(0.54f);
                                    }
                                }
                            }
                            if (!(contextActivity instanceof PeekViewActivity)
                                    || !((PeekViewActivity) contextActivity).isPeeking()
                                    || (base instanceof HeaderImageLinkView
                                            && ((HeaderImageLinkView) base).popped)) {
                                if (!PostMatch.openExternal(submission.getUrl())
                                        || type == ContentType.Type.VIDEO) {
                                    switch (type) {
                                        case STREAMABLE:
                                            if (SettingValues.video) {
                                                Intent myIntent =
                                                        new Intent(
                                                                contextActivity, MediaView.class);
                                                myIntent.putExtra(
                                                        MediaView.SUBREDDIT,
                                                        submission.getSubredditName());
                                                myIntent.putExtra(
                                                        MediaView.EXTRA_URL, submission.getUrl());
                                                myIntent.putExtra(
                                                        EXTRA_SUBMISSION_TITLE,
                                                        submission.getTitle());
                                                contextActivity.startActivity(myIntent);
                                            } else {
                                                LinkUtil.openExternally(submission.getUrl());
                                            }
                                            break;
                                        case IMGUR:
                                        case DEVIANTART:
                                        case XKCD:
                                        case IMAGE:
                                            openImage(
                                                    type,
                                                    contextActivity,
                                                    submission,
                                                    holder.leadImage,
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
                                                    Intent i =
                                                            new Intent(
                                                                    contextActivity,
                                                                    FullscreenVideo.class);
                                                    i.putExtra(FullscreenVideo.EXTRA_HTML, data);
                                                    contextActivity.startActivity(i);
                                                }
                                            } else {
                                                LinkUtil.openExternally(submission.getUrl());
                                            }
                                            break;
                                        case REDDIT:
                                            openRedditContent(submission.getUrl(), contextActivity);
                                            break;
                                        case LINK:
                                            LinkUtil.openUrl(
                                                    submission.getUrl(),
                                                    Palette.getColor(submission.getSubredditName()),
                                                    contextActivity,
                                                    holder.getBindingAdapterPosition(),
                                                    submission);
                                            break;
                                        case SELF:
                                            if (holder != null) {
                                                OnSingleClickListener.override = true;
                                                holder.itemView.performClick();
                                            }
                                            break;
                                        case ALBUM:
                                            if (SettingValues.album) {
                                                Intent i;
                                                if (SettingValues.albumSwipe) {
                                                    i =
                                                            new Intent(
                                                                    contextActivity,
                                                                    AlbumPager.class);
                                                    i.putExtra(
                                                            AlbumPager.SUBREDDIT,
                                                            submission.getSubredditName());
                                                } else {
                                                    i = new Intent(contextActivity, Album.class);
                                                    i.putExtra(
                                                            Album.SUBREDDIT,
                                                            submission.getSubredditName());
                                                }
                                                i.putExtra(
                                                        EXTRA_SUBMISSION_TITLE,
                                                        submission.getTitle());
                                                i.putExtra(Album.EXTRA_URL, submission.getUrl());

                                                PopulateBase.addAdaptorPosition(
                                                        i,
                                                        submission,
                                                        holder.getBindingAdapterPosition());
                                                contextActivity.startActivity(i);
                                                contextActivity.overridePendingTransition(
                                                        R.anim.slideright, R.anim.fade_out);
                                            } else {
                                                LinkUtil.openExternally(submission.getUrl());
                                            }
                                            break;
                                        case TUMBLR:
                                            if (SettingValues.album) {
                                                Intent i;
                                                if (SettingValues.albumSwipe) {
                                                    i =
                                                            new Intent(
                                                                    contextActivity,
                                                                    TumblrPager.class);
                                                    i.putExtra(
                                                            TumblrPager.SUBREDDIT,
                                                            submission.getSubredditName());
                                                } else {
                                                    i = new Intent(contextActivity, Tumblr.class);
                                                    i.putExtra(
                                                            Tumblr.SUBREDDIT,
                                                            submission.getSubredditName());
                                                }
                                                i.putExtra(Album.EXTRA_URL, submission.getUrl());

                                                PopulateBase.addAdaptorPosition(
                                                        i,
                                                        submission,
                                                        holder.getBindingAdapterPosition());
                                                contextActivity.startActivity(i);
                                                contextActivity.overridePendingTransition(
                                                        R.anim.slideright, R.anim.fade_out);
                                            } else {
                                                LinkUtil.openExternally(submission.getUrl());
                                            }
                                            break;
                                        case GIF:
                                            openGif(
                                                    contextActivity,
                                                    submission,
                                                    holder.getBindingAdapterPosition());
                                            break;
                                        case NONE:
                                            if (holder != null) {
                                                holder.itemView.performClick();
                                            }
                                            break;
                                        case VIDEO:
                                            if (!LinkUtil.tryOpenWithVideoPlugin(
                                                    submission.getUrl())) {
                                                LinkUtil.openUrl(
                                                        submission.getUrl(),
                                                        Palette.getStatusBarColor(),
                                                        contextActivity);
                                            }
                                            break;
                                    }
                                } else {
                                    LinkUtil.openExternally(submission.getUrl());
                                }
                            }
                        } else {
                            if (!(contextActivity instanceof PeekViewActivity)
                                    || !((PeekViewActivity) contextActivity).isPeeking()) {

                                Snackbar s =
                                        Snackbar.make(
                                                holder.itemView,
                                                R.string.go_online_view_content,
                                                Snackbar.LENGTH_SHORT);
                                LayoutUtils.showSnackbar(s);
                            }
                        }
                    }
                });
    }

    public static void openRedditContent(String url, Context c) {
        OpenRedditLink.openUrl(c, url, true);
    }

    public static void openImage(
            ContentType.Type type,
            Activity contextActivity,
            Submission submission,
            HeaderImageLinkView baseView,
            int adapterPosition) {
        if (SettingValues.image) {
            Intent myIntent = new Intent(contextActivity, MediaView.class);
            myIntent.putExtra(MediaView.SUBREDDIT, submission.getSubredditName());
            myIntent.putExtra(EXTRA_SUBMISSION_TITLE, submission.getTitle());
            String previewUrl;
            String url = submission.getUrl();

            if (baseView != null
                    && baseView.lq
                    && SettingValues.loadImageLq
                    && type != ContentType.Type.XKCD) {
                myIntent.putExtra(MediaView.EXTRA_LQ, true);
                myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, baseView.loadedUrl);
            } else if (submission.getDataNode().has("preview")
                    && submission
                            .getDataNode()
                            .get("preview")
                            .get("images")
                            .get(0)
                            .get("source")
                            .has("height")
                    && type
                            != ContentType.Type
                                    .XKCD) { // Load the preview image which has probably already
                // been cached in memory instead of the direct link
                previewUrl =
                        StringEscapeUtils.escapeHtml4(
                                submission
                                        .getDataNode()
                                        .get("preview")
                                        .get("images")
                                        .get(0)
                                        .get("source")
                                        .get("url")
                                        .asText());
                if (baseView == null || (!SettingValues.loadImageLq && baseView.lq)) {
                    myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl);
                } else {
                    myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, baseView.loadedUrl);
                }
            }
            myIntent.putExtra(MediaView.EXTRA_URL, url);
            PopulateBase.addAdaptorPosition(myIntent, submission, adapterPosition);
            myIntent.putExtra(MediaView.EXTRA_SHARE_URL, submission.getUrl());

            contextActivity.startActivity(myIntent);

        } else {
            LinkUtil.openExternally(submission.getUrl());
        }
    }

    public static void openGif(
            Activity contextActivity, Submission submission, int adapterPosition) {
        if (SettingValues.gif) {
            DataShare.sharedSubmission = submission;

            Intent myIntent = new Intent(contextActivity, MediaView.class);
            myIntent.putExtra(MediaView.SUBREDDIT, submission.getSubredditName());
            myIntent.putExtra(EXTRA_SUBMISSION_TITLE, submission.getTitle());

            GifUtils.AsyncLoadGif.VideoType t =
                    GifUtils.AsyncLoadGif.getVideoType(submission.getUrl());

            if (t == GifUtils.AsyncLoadGif.VideoType.DIRECT
                    && submission.getDataNode().has("preview")
                    && submission.getDataNode().get("preview").get("images").get(0).has("variants")
                    && submission
                            .getDataNode()
                            .get("preview")
                            .get("images")
                            .get(0)
                            .get("variants")
                            .has("mp4")) {
                myIntent.putExtra(
                        MediaView.EXTRA_URL,
                        StringEscapeUtils.unescapeJson(
                                        submission
                                                .getDataNode()
                                                .get("preview")
                                                .get("images")
                                                .get(0)
                                                .get("variants")
                                                .get("mp4")
                                                .get("source")
                                                .get("url")
                                                .asText())
                                .replace("&amp;", "&"));
            } else if (t == GifUtils.AsyncLoadGif.VideoType.DIRECT
                    && submission.getDataNode().has("media")
                    && submission.getDataNode().get("media").has("reddit_video")
                    && submission
                            .getDataNode()
                            .get("media")
                            .get("reddit_video")
                            .has("fallback_url")) {
                myIntent.putExtra(
                        MediaView.EXTRA_URL,
                        StringEscapeUtils.unescapeJson(
                                        submission
                                                .getDataNode()
                                                .get("media")
                                                .get("reddit_video")
                                                .get("fallback_url")
                                                .asText())
                                .replace("&amp;", "&"));

            } else {
                myIntent.putExtra(MediaView.EXTRA_URL, submission.getUrl());
            }
            if (submission.getDataNode().has("preview")
                    && submission
                            .getDataNode()
                            .get("preview")
                            .get("images")
                            .get(0)
                            .get("source")
                            .has("height")) { // Load the preview image which has probably
                // already been cached in memory instead of the
                // direct link
                String previewUrl =
                        StringEscapeUtils.escapeHtml4(
                                submission
                                        .getDataNode()
                                        .get("preview")
                                        .get("images")
                                        .get(0)
                                        .get("source")
                                        .get("url")
                                        .asText());
                myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl);
            }
            PopulateBase.addAdaptorPosition(myIntent, submission, adapterPosition);
            contextActivity.startActivity(myIntent);
        } else {
            LinkUtil.openExternally(submission.getUrl());
        }
    }

    public String reason;

    boolean[] chosen = new boolean[] {false, false, false};
    boolean[] oldChosen = new boolean[] {false, false, false};

    public <T extends Contribution> void showBottomSheet(
            final Activity mContext,
            final Submission submission,
            final NewsViewHolder holder,
            final List<T> posts,
            final String baseSub,
            final RecyclerView recyclerview,
            final boolean full) {

        int[] attrs = new int[] {R.attr.tintColor};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable profile =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_account_circle, null);
        final Drawable sub =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_bookmark_border, null);
        Drawable saved =
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_star, null);
        Drawable hide =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_visibility_off, null);
        final Drawable report =
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_report, null);
        Drawable copy =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_content_copy, null);
        final Drawable readLater =
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_download, null);
        Drawable open =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_open_in_browser, null);
        Drawable link =
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_link, null);
        Drawable reddit =
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_forum, null);
        Drawable filter =
                ResourcesCompat.getDrawable(
                        mContext.getResources(), R.drawable.ic_filter_list, null);

        final List<Drawable> drawableSet =
                Arrays.asList(
                        profile, sub, saved, hide, report, copy, open, link, reddit, readLater,
                        filter);
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

        ta.recycle();

        final BottomSheet.Builder b =
                new BottomSheet.Builder(mContext).title(CompatUtil.fromHtml(submission.getTitle()));

        final boolean isReadLater = mContext instanceof PostReadLater;
        final boolean isAddedToReadLaterList = ReadLater.isToBeReadLater(submission);
        if (Authentication.didOnline) {
            b.sheet(1, profile, "/u/" + submission.getAuthor())
                    .sheet(2, sub, "/r/" + submission.getSubredditName());
            String save = mContext.getString(R.string.btn_save);
            if (ActionStates.isSaved(submission)) {
                save = mContext.getString(R.string.comment_unsave);
            }
            if (Authentication.isLoggedIn) {
                b.sheet(3, saved, save);
            }
        }

        if (isAddedToReadLaterList) {
            CharSequence markAsReadCs = mContext.getString(R.string.mark_as_read);
            b.sheet(28, readLater, markAsReadCs);
        } else {
            CharSequence readLaterCs = mContext.getString(R.string.read_later);
            b.sheet(28, readLater, readLaterCs);
        }

        if (Authentication.didOnline) {
            if (Authentication.isLoggedIn) {
                b.sheet(12, report, mContext.getString(R.string.btn_report));
            }
        }

        if (submission.getSelftext() != null && !submission.getSelftext().isEmpty() && full) {
            b.sheet(25, copy, mContext.getString(R.string.submission_copy_text));
        }

        boolean hidden = submission.isHidden();
        if (!full && Authentication.didOnline) {
            if (!hidden) {
                b.sheet(5, hide, mContext.getString(R.string.submission_hide));
            } else {
                b.sheet(5, hide, mContext.getString(R.string.submission_unhide));
            }
        }
        b.sheet(7, open, mContext.getString(R.string.open_externally));

        b.sheet(4, link, mContext.getString(R.string.submission_share_permalink))
                .sheet(8, reddit, mContext.getString(R.string.submission_share_reddit_url));
        if ((mContext instanceof MainActivity) || (mContext instanceof SubredditView)) {
            b.sheet(10, filter, mContext.getString(R.string.filter_content));
        }

        b.listener(
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 1:
                                {
                                    Intent i = new Intent(mContext, Profile.class);
                                    i.putExtra(Profile.EXTRA_PROFILE, submission.getAuthor());
                                    mContext.startActivity(i);
                                }
                                break;
                            case 2:
                                {
                                    Intent i = new Intent(mContext, SubredditView.class);
                                    i.putExtra(
                                            SubredditView.EXTRA_SUBREDDIT,
                                            submission.getSubredditName());
                                    mContext.startActivityForResult(i, 14);
                                }
                                break;
                            case 10:
                                String[] choices;
                                final String flair =
                                        submission.getSubmissionFlair().getText() != null
                                                ? submission.getSubmissionFlair().getText()
                                                : "";
                                if (flair.isEmpty()) {
                                    choices =
                                            new String[] {
                                                mContext.getString(
                                                        R.string.filter_posts_sub,
                                                        submission.getSubredditName()),
                                                mContext.getString(
                                                        R.string.filter_posts_user,
                                                        submission.getAuthor()),
                                                mContext.getString(
                                                        R.string.filter_posts_urls,
                                                        submission.getDomain()),
                                                mContext.getString(
                                                        R.string.filter_open_externally,
                                                        submission.getDomain())
                                            };

                                    chosen =
                                            new boolean[] {
                                                SettingValues.subredditFilters.contains(
                                                        submission
                                                                .getSubredditName()
                                                                .toLowerCase(Locale.ENGLISH)),
                                                SettingValues.userFilters.contains(
                                                        submission
                                                                .getAuthor()
                                                                .toLowerCase(Locale.ENGLISH)),
                                                SettingValues.domainFilters.contains(
                                                        submission
                                                                .getDomain()
                                                                .toLowerCase(Locale.ENGLISH)),
                                                SettingValues.alwaysExternal.contains(
                                                        submission
                                                                .getDomain()
                                                                .toLowerCase(Locale.ENGLISH))
                                            };
                                    oldChosen = chosen.clone();
                                } else {
                                    choices =
                                            new String[] {
                                                mContext.getString(
                                                        R.string.filter_posts_sub,
                                                        submission.getSubredditName()),
                                                mContext.getString(
                                                        R.string.filter_posts_user,
                                                        submission.getAuthor()),
                                                mContext.getString(
                                                        R.string.filter_posts_urls,
                                                        submission.getDomain()),
                                                mContext.getString(
                                                        R.string.filter_open_externally,
                                                        submission.getDomain()),
                                                mContext.getString(
                                                        R.string.filter_posts_flair, flair, baseSub)
                                            };
                                }
                                chosen =
                                        new boolean[] {
                                            SettingValues.subredditFilters.contains(
                                                    submission
                                                            .getSubredditName()
                                                            .toLowerCase(Locale.ENGLISH)),
                                            SettingValues.userFilters.contains(
                                                    submission
                                                            .getAuthor()
                                                            .toLowerCase(Locale.ENGLISH)),
                                            SettingValues.domainFilters.contains(
                                                    submission
                                                            .getDomain()
                                                            .toLowerCase(Locale.ENGLISH)),
                                            SettingValues.alwaysExternal.contains(
                                                    submission
                                                            .getDomain()
                                                            .toLowerCase(Locale.ENGLISH)),
                                            SettingValues.flairFilters.contains(
                                                    baseSub
                                                            + ":"
                                                            + flair.toLowerCase(Locale.ENGLISH)
                                                                    .trim())
                                        };
                                oldChosen = chosen.clone();

                                new AlertDialog.Builder(mContext)
                                        .setTitle(R.string.filter_title)
                                        .setMultiChoiceItems(
                                                choices,
                                                chosen,
                                                (dialog1, which1, isChecked) ->
                                                        chosen[which1] = isChecked)
                                        .setPositiveButton(
                                                R.string.filter_btn,
                                                (dialog12, which12) -> {
                                                    boolean filtered = false;
                                                    SharedPreferences.Editor e =
                                                            SettingValues.prefs.edit();
                                                    if (chosen[0] && chosen[0] != oldChosen[0]) {
                                                        SettingValues.subredditFilters.add(
                                                                submission
                                                                        .getSubredditName()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = true;
                                                        e.putStringSet(
                                                                SettingValues
                                                                        .PREF_SUBREDDIT_FILTERS,
                                                                SettingValues.subredditFilters);
                                                    } else if (!chosen[0]
                                                            && chosen[0] != oldChosen[0]) {
                                                        SettingValues.subredditFilters.remove(
                                                                submission
                                                                        .getSubredditName()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = false;
                                                        e.putStringSet(
                                                                SettingValues
                                                                        .PREF_SUBREDDIT_FILTERS,
                                                                SettingValues.subredditFilters);
                                                        e.apply();
                                                    }
                                                    if (chosen[1] && chosen[1] != oldChosen[1]) {
                                                        SettingValues.userFilters.add(
                                                                submission
                                                                        .getAuthor()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = true;
                                                        e.putStringSet(
                                                                SettingValues.PREF_USER_FILTERS,
                                                                SettingValues.userFilters);
                                                    } else if (!chosen[1]
                                                            && chosen[1] != oldChosen[1]) {
                                                        SettingValues.userFilters.remove(
                                                                submission
                                                                        .getAuthor()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = false;
                                                        e.putStringSet(
                                                                SettingValues.PREF_USER_FILTERS,
                                                                SettingValues.userFilters);
                                                        e.apply();
                                                    }
                                                    if (chosen[2] && chosen[2] != oldChosen[2]) {
                                                        SettingValues.domainFilters.add(
                                                                submission
                                                                        .getDomain()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = true;
                                                        e.putStringSet(
                                                                SettingValues.PREF_DOMAIN_FILTERS,
                                                                SettingValues.domainFilters);
                                                    } else if (!chosen[2]
                                                            && chosen[2] != oldChosen[2]) {
                                                        SettingValues.domainFilters.remove(
                                                                submission
                                                                        .getDomain()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        filtered = false;
                                                        e.putStringSet(
                                                                SettingValues.PREF_DOMAIN_FILTERS,
                                                                SettingValues.domainFilters);
                                                        e.apply();
                                                    }
                                                    if (chosen[3] && chosen[3] != oldChosen[3]) {
                                                        SettingValues.alwaysExternal.add(
                                                                submission
                                                                        .getDomain()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        e.putStringSet(
                                                                SettingValues.PREF_ALWAYS_EXTERNAL,
                                                                SettingValues.alwaysExternal);
                                                        e.apply();
                                                    } else if (!chosen[3]
                                                            && chosen[3] != oldChosen[3]) {
                                                        SettingValues.alwaysExternal.remove(
                                                                submission
                                                                        .getDomain()
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim());
                                                        e.putStringSet(
                                                                SettingValues.PREF_ALWAYS_EXTERNAL,
                                                                SettingValues.alwaysExternal);
                                                        e.apply();
                                                    }
                                                    if (chosen.length > 4) {
                                                        String s =
                                                                (baseSub + ":" + flair)
                                                                        .toLowerCase(Locale.ENGLISH)
                                                                        .trim();
                                                        if (chosen[4]
                                                                && chosen[4] != oldChosen[4]) {
                                                            SettingValues.flairFilters.add(s);
                                                            e.putStringSet(
                                                                    SettingValues
                                                                            .PREF_FLAIR_FILTERS,
                                                                    SettingValues.flairFilters);
                                                            e.apply();
                                                            filtered = true;
                                                        } else if (!chosen[4]
                                                                && chosen[4] != oldChosen[4]) {
                                                            SettingValues.flairFilters.remove(s);
                                                            e.putStringSet(
                                                                    SettingValues
                                                                            .PREF_FLAIR_FILTERS,
                                                                    SettingValues.flairFilters);
                                                            e.apply();
                                                        }
                                                    }
                                                    if (filtered) {
                                                        e.apply();
                                                        ArrayList<Contribution> toRemove =
                                                                new ArrayList<>();
                                                        for (Contribution s : posts) {
                                                            if (s instanceof Submission
                                                                    && PostMatch.doesMatch(
                                                                            (Submission) s)) {
                                                                toRemove.add(s);
                                                            }
                                                        }

                                                        OfflineSubreddit s =
                                                                OfflineSubreddit.getSubreddit(
                                                                        baseSub, false, mContext);

                                                        for (Contribution remove : toRemove) {
                                                            final int pos = posts.indexOf(remove);
                                                            posts.remove(pos);
                                                            if (baseSub != null) {
                                                                s.hideMulti(pos);
                                                            }
                                                        }
                                                        s.writeToMemoryNoStorage();
                                                        recyclerview
                                                                .getAdapter()
                                                                .notifyDataSetChanged();
                                                    }
                                                })
                                        .setNegativeButton(R.string.btn_cancel, null)
                                        .show();
                                break;
                            case 5:
                                hideSubmission(submission, posts, baseSub, recyclerview, mContext);
                                break;
                            case 7:
                                LinkUtil.openExternally(submission.getUrl());
                                if (submission.isNsfw() && !SettingValues.storeNSFWHistory) {
                                    // Do nothing if the post is NSFW and storeNSFWHistory is not
                                    // enabled
                                } else if (SettingValues.storeHistory) {
                                    HasSeen.addSeen(submission.getFullName());
                                }
                                break;
                            case 28:
                                if (!isAddedToReadLaterList) {
                                    ReadLater.setReadLater(submission, true);
                                    Snackbar s =
                                            Snackbar.make(
                                                    holder.itemView,
                                                    "Added to read later!",
                                                    Snackbar.LENGTH_SHORT);
                                    View view = s.getView();
                                    TextView tv =
                                            view.findViewById(
                                                    com.google.android.material.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.setAction(
                                            R.string.btn_undo,
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ReadLater.setReadLater(submission, false);
                                                    Snackbar s2 =
                                                            Snackbar.make(
                                                                    holder.itemView,
                                                                    "Removed from read later",
                                                                    Snackbar.LENGTH_SHORT);
                                                    LayoutUtils.showSnackbar(s2);
                                                }
                                            });
                                    if (NetworkUtil.isConnected(mContext)) {
                                        new CommentCacheAsync(
                                                        Collections.singletonList(submission),
                                                        mContext,
                                                        CommentCacheAsync.SAVED_SUBMISSIONS,
                                                        new boolean[] {true, true})
                                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                    }
                                    s.show();
                                } else {
                                    ReadLater.setReadLater(submission, false);
                                    if (isReadLater || !Authentication.didOnline) {
                                        final int pos = posts.indexOf(submission);
                                        posts.remove(submission);

                                        recyclerview
                                                .getAdapter()
                                                .notifyItemRemoved(
                                                        holder.getBindingAdapterPosition());

                                        Snackbar s2 =
                                                Snackbar.make(
                                                        holder.itemView,
                                                        "Removed from read later",
                                                        Snackbar.LENGTH_SHORT);
                                        View view2 = s2.getView();
                                        TextView tv2 =
                                                view2.findViewById(
                                                        com.google.android.material.R.id
                                                                .snackbar_text);
                                        tv2.setTextColor(Color.WHITE);
                                        s2.setAction(
                                                R.string.btn_undo,
                                                new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        posts.add(pos, (T) submission);
                                                        recyclerview
                                                                .getAdapter()
                                                                .notifyDataSetChanged();
                                                    }
                                                });
                                    } else {
                                        Snackbar s2 =
                                                Snackbar.make(
                                                        holder.itemView,
                                                        "Removed from read later",
                                                        Snackbar.LENGTH_SHORT);
                                        View view2 = s2.getView();
                                        TextView tv2 =
                                                view2.findViewById(
                                                        com.google.android.material.R.id
                                                                .snackbar_text);
                                        s2.show();
                                    }
                                    OfflineSubreddit.newSubreddit(
                                                    CommentCacheAsync.SAVED_SUBMISSIONS)
                                            .deleteFromMemory(submission.getFullName());
                                }
                                break;
                            case 4:
                                Reddit.defaultShareText(
                                        CompatUtil.fromHtml(submission.getTitle()).toString(),
                                        StringEscapeUtils.escapeHtml4(submission.getUrl()),
                                        mContext);
                                break;
                            case 12:
                                final MaterialDialog reportDialog =
                                        new MaterialDialog.Builder(mContext)
                                                .customView(R.layout.report_dialog, true)
                                                .title(R.string.report_post)
                                                .positiveText(R.string.btn_report)
                                                .negativeText(R.string.btn_cancel)
                                                .onPositive(
                                                        new MaterialDialog.SingleButtonCallback() {
                                                            @Override
                                                            public void onClick(
                                                                    MaterialDialog dialog,
                                                                    DialogAction which) {
                                                                RadioGroup reasonGroup =
                                                                        dialog.getCustomView()
                                                                                .findViewById(
                                                                                        R.id
                                                                                                .report_reasons);
                                                                String reportReason;
                                                                if (reasonGroup
                                                                                .getCheckedRadioButtonId()
                                                                        == R.id.report_other) {
                                                                    reportReason =
                                                                            ((EditText)
                                                                                            dialog.getCustomView()
                                                                                                    .findViewById(
                                                                                                            R
                                                                                                                    .id
                                                                                                                    .input_report_reason))
                                                                                    .getText()
                                                                                    .toString();
                                                                } else {
                                                                    reportReason =
                                                                            ((RadioButton)
                                                                                            reasonGroup
                                                                                                    .findViewById(
                                                                                                            reasonGroup
                                                                                                                    .getCheckedRadioButtonId()))
                                                                                    .getText()
                                                                                    .toString();
                                                                }
                                                                new PopulateBase.AsyncReportTask(
                                                                                submission,
                                                                                holder.itemView)
                                                                        .executeOnExecutor(
                                                                                AsyncTask
                                                                                        .THREAD_POOL_EXECUTOR,
                                                                                reportReason);
                                                            }
                                                        })
                                                .build();

                                final RadioGroup reasonGroup =
                                        reportDialog
                                                .getCustomView()
                                                .findViewById(R.id.report_reasons);

                                reasonGroup.setOnCheckedChangeListener(
                                        new RadioGroup.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(
                                                    RadioGroup group, int checkedId) {
                                                if (checkedId == R.id.report_other)
                                                    reportDialog
                                                            .getCustomView()
                                                            .findViewById(R.id.input_report_reason)
                                                            .setVisibility(View.VISIBLE);
                                                else
                                                    reportDialog
                                                            .getCustomView()
                                                            .findViewById(R.id.input_report_reason)
                                                            .setVisibility(View.GONE);
                                            }
                                        });

                                // Load sub's report reasons and show the appropriate ones
                                new AsyncTask<Void, Void, Ruleset>() {
                                    @Override
                                    protected Ruleset doInBackground(Void... voids) {
                                        return Authentication.reddit.getRules(
                                                submission.getSubredditName());
                                    }

                                    @Override
                                    protected void onPostExecute(Ruleset rules) {
                                        reportDialog
                                                .getCustomView()
                                                .findViewById(R.id.report_loading)
                                                .setVisibility(View.GONE);
                                        if (rules.getSubredditRules().size() > 0) {
                                            TextView subHeader = new TextView(mContext);
                                            subHeader.setText(
                                                    mContext.getString(
                                                            R.string.report_sub_rules,
                                                            submission.getSubredditName()));
                                            reasonGroup.addView(
                                                    subHeader, reasonGroup.getChildCount() - 2);
                                        }
                                        for (SubredditRule rule : rules.getSubredditRules()) {
                                            if (rule.getKind() == SubredditRule.RuleKind.LINK
                                                    || rule.getKind()
                                                            == SubredditRule.RuleKind.ALL) {
                                                RadioButton btn = new RadioButton(mContext);
                                                btn.setText(rule.getViolationReason());
                                                reasonGroup.addView(
                                                        btn, reasonGroup.getChildCount() - 2);
                                                btn.getLayoutParams().width =
                                                        WindowManager.LayoutParams.MATCH_PARENT;
                                            }
                                        }
                                        if (rules.getSiteRules().size() > 0) {
                                            TextView siteHeader = new TextView(mContext);
                                            siteHeader.setText(R.string.report_site_rules);
                                            reasonGroup.addView(
                                                    siteHeader, reasonGroup.getChildCount() - 2);
                                        }
                                        for (String rule : rules.getSiteRules()) {
                                            RadioButton btn = new RadioButton(mContext);
                                            btn.setText(rule);
                                            reasonGroup.addView(
                                                    btn, reasonGroup.getChildCount() - 2);
                                            btn.getLayoutParams().width =
                                                    WindowManager.LayoutParams.MATCH_PARENT;
                                        }
                                    }
                                }.execute();

                                reportDialog.show();
                                break;
                            case 8:
                                Reddit.defaultShareText(
                                        CompatUtil.fromHtml(submission.getTitle()).toString(),
                                        "https://reddit.com" + submission.getPermalink(),
                                        mContext);
                                break;
                            case 6:
                                {
                                    ClipboardUtil.copyToClipboard(
                                            mContext, "Link", submission.getUrl());
                                    Toast.makeText(
                                                    mContext,
                                                    R.string.submission_link_copied,
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                                break;
                            case 25:
                                final TextView showText = new TextView(mContext);
                                showText.setText(
                                        StringEscapeUtils.unescapeHtml4(
                                                submission.getTitle()
                                                        + "\n\n"
                                                        + submission.getSelftext()));
                                showText.setTextIsSelectable(true);
                                int sixteen = DisplayUtil.dpToPxVertical(24);
                                showText.setPadding(sixteen, 0, sixteen, 0);
                                new AlertDialog.Builder(mContext)
                                        .setView(showText)
                                        .setTitle("Select text to copy")
                                        .setCancelable(true)
                                        .setPositiveButton(
                                                "COPY SELECTED",
                                                (dialog13, which13) -> {
                                                    String selected =
                                                            showText.getText()
                                                                    .toString()
                                                                    .substring(
                                                                            showText
                                                                                    .getSelectionStart(),
                                                                            showText
                                                                                    .getSelectionEnd());
                                                    if (!selected.isEmpty()) {
                                                        ClipboardUtil.copyToClipboard(
                                                                mContext, "Selftext", selected);
                                                    } else {
                                                        ClipboardUtil.copyToClipboard(
                                                                mContext,
                                                                "Selftext",
                                                                CompatUtil.fromHtml(
                                                                        submission.getTitle()
                                                                                + "\n\n"
                                                                                + submission
                                                                                        .getSelftext()));
                                                    }
                                                    Toast.makeText(
                                                                    mContext,
                                                                    R.string
                                                                            .submission_comment_copied,
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                })
                                        .setNegativeButton(R.string.btn_cancel, null)
                                        .setNeutralButton(
                                                "COPY ALL",
                                                (dialog14, which14) -> {
                                                    ClipboardUtil.copyToClipboard(
                                                            mContext,
                                                            "Selftext",
                                                            StringEscapeUtils.unescapeHtml4(
                                                                    submission.getTitle()
                                                                            + "\n\n"
                                                                            + submission
                                                                                    .getSelftext()));

                                                    Toast.makeText(
                                                                    mContext,
                                                                    R.string.submission_text_copied,
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                })
                                        .show();
                                break;
                        }
                    }
                });
        b.show();
    }

    public <T extends Contribution> void hideSubmission(
            final Submission submission,
            final List<T> posts,
            final String baseSub,
            final RecyclerView recyclerview,
            Context c) {
        final int pos = posts.indexOf(submission);
        if (pos != -1) {
            if (submission.isHidden()) {
                posts.remove(pos);
                Hidden.undoHidden(submission);
                recyclerview.getAdapter().notifyItemRemoved(pos + 1);
                Snackbar snack =
                        Snackbar.make(
                                recyclerview,
                                R.string.submission_info_unhidden,
                                Snackbar.LENGTH_LONG);
                LayoutUtils.showSnackbar(snack);
            } else {
                final T t = posts.get(pos);
                posts.remove(pos);
                Hidden.setHidden(t);
                final OfflineSubreddit s;
                boolean success = false;
                if (baseSub != null) {
                    s = OfflineSubreddit.getSubreddit(baseSub, false, c);
                    try {
                        s.hide(pos);
                        success = true;
                    } catch (Exception e) {
                    }
                } else {
                    success = false;
                    s = null;
                }

                recyclerview.getAdapter().notifyItemRemoved(pos + 1);

                final boolean finalSuccess = success;
                Snackbar snack =
                        Snackbar.make(
                                        recyclerview,
                                        R.string.submission_info_hidden,
                                        Snackbar.LENGTH_LONG)
                                .setAction(
                                        R.string.btn_undo,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if (baseSub != null && s != null && finalSuccess) {
                                                    s.unhideLast();
                                                }
                                                posts.add(pos, t);
                                                recyclerview
                                                        .getAdapter()
                                                        .notifyItemInserted(pos + 1);
                                                Hidden.undoHidden(t);
                                            }
                                        });
                LayoutUtils.showSnackbar(snack);
            }
        }
    }

    public void doText(
            NewsViewHolder holder, Submission submission, Context mContext, String baseSub) {
        SpannableStringBuilder t = SubmissionCache.getTitleLine(submission, mContext);
        SpannableStringBuilder l = SubmissionCache.getInfoLine(submission, mContext, baseSub);

        int[] textSizeAttr = new int[] {R.attr.font_cardtitle, R.attr.font_cardinfo};
        TypedArray a = mContext.obtainStyledAttributes(textSizeAttr);
        int textSizeT = a.getDimensionPixelSize(0, 18);
        int textSizeI = a.getDimensionPixelSize(1, 14);

        a.recycle();

        t.setSpan(new AbsoluteSizeSpan(textSizeT), 0, t.length(), 0);
        l.setSpan(new AbsoluteSizeSpan(textSizeI), 0, l.length(), 0);

        SpannableStringBuilder s = new SpannableStringBuilder();
        if (SettingValues.titleTop) {
            s.append(t);
            s.append("\n");
            s.append(l);
        } else {
            s.append(l);
            s.append("\n");
            s.append(t);
        }

        holder.title.setText(s);
    }

    public <T extends Contribution> void populateNewsViewHolder(
            final NewsViewHolder holder,
            final Submission submission,
            final Activity mContext,
            boolean fullscreen,
            final boolean full,
            final List<T> posts,
            final RecyclerView recyclerview,
            final boolean same,
            final boolean offline,
            final String baseSub,
            @Nullable final CommentAdapter adapter) {

        holder.menu.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showBottomSheet(
                                mContext, submission, holder, posts, baseSub, recyclerview, full);
                    }
                });

        // Use this to offset the submission score
        int submissionScore = submission.getScore();

        final int commentCount = submission.getCommentCount();
        final int more = LastComments.commentsSince(submission);
        String scoreRatio =
                (SettingValues.upvotePercentage && full && submission.getUpvoteRatio() != null)
                        ? "(" + (int) (submission.getUpvoteRatio() * 100) + "%)"
                        : "";

        if (!scoreRatio.isEmpty()) {
            TextView percent = holder.itemView.findViewById(R.id.percent);
            percent.setVisibility(View.VISIBLE);
            percent.setText(scoreRatio);

            final double numb = (submission.getUpvoteRatio());
            if (numb <= .5) {
                if (numb <= .1) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_500));
                } else if (numb <= .3) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_400));
                } else {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_300));
                }
            } else {
                if (numb >= .9) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_500));
                } else if (numb >= .7) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_400));
                } else {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_300));
                }
            }
        }

        // Save the score so we can use it in the OnClickListeners for the vote buttons

        ImageView thumbImage2 = holder.thumbnail;

        if (holder.leadImage.thumbImage2 == null) {
            holder.leadImage.setThumbnail(thumbImage2);
        }

        final ContentType.Type type = ContentType.getContentType(submission);

        addClickFunctions(holder.itemView, type, mContext, submission, holder, full);

        holder.comment.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OpenRedditLink.openUrl(mContext, submission.getPermalink(), true);
                    }
                });

        if (thumbImage2 != null) {
            addClickFunctions(thumbImage2, type, mContext, submission, holder, full);
        }

        holder.leadImage.setSubmissionNews(submission, full, baseSub, type);

        holder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        if (offline) {
                            Snackbar s =
                                    Snackbar.make(
                                            holder.itemView,
                                            mContext.getString(R.string.offline_msg),
                                            Snackbar.LENGTH_SHORT);
                            LayoutUtils.showSnackbar(s);
                        } else {
                            if (SettingValues.actionbarTap && !full) {
                                CreateCardView.toggleActionbar(holder.itemView);
                            } else {
                                holder.itemView.findViewById(R.id.menu).callOnClick();
                            }
                        }
                        return true;
                    }
                });

        doText(holder, submission, mContext, baseSub);

        if (HasSeen.getSeen(submission) && !full) {
            holder.title.setAlpha(0.54f);
        } else {
            holder.title.setAlpha(1f);
        }
    }
}
