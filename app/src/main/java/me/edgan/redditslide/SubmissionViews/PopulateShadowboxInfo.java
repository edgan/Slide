package me.edgan.redditslide.SubmissionViews;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import me.edgan.redditslide.ActionStates;
import me.edgan.redditslide.Activities.Profile;
import me.edgan.redditslide.Activities.SubredditView;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.HasSeen;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Views.RoundedBackgroundSpan;
import me.edgan.redditslide.Views.TitleTextView;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.Vote;
import me.edgan.redditslide.util.AnimatorUtil;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.ClipboardUtil;
import me.edgan.redditslide.util.CompatUtil;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.TimeUtils;

import net.dean.jraw.ApiException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.DistinguishedStatus;
import net.dean.jraw.models.Ruleset;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditRule;
import net.dean.jraw.models.VoteDirection;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Created by carlo_000 on 2/27/2016. */
public class PopulateShadowboxInfo {
    public static void doActionbar(
            final Submission s, final View rootView, final Activity c, boolean extras) {
        TextView title = rootView.findViewById(R.id.title);
        TextView desc = rootView.findViewById(R.id.desc);
        String distingush = "";
        if (s != null) {
            if (s.getDistinguishedStatus() == DistinguishedStatus.MODERATOR) distingush = "[M]";
            else if (s.getDistinguishedStatus() == DistinguishedStatus.ADMIN) distingush = "[A]";

            title.setText(CompatUtil.fromHtml(s.getTitle()));

            String spacer = c.getString(R.string.submission_properties_seperator);
            SpannableStringBuilder titleString = new SpannableStringBuilder();

            SpannableStringBuilder subreddit =
                    new SpannableStringBuilder(" /r/" + s.getSubredditName() + " ");

            String subname = s.getSubredditName().toLowerCase(Locale.ENGLISH);
            if ((SettingValues.colorSubName
                    && Palette.getColor(subname) != Palette.getDefaultColor())) {
                subreddit.setSpan(
                        new ForegroundColorSpan(Palette.getColor(subname)),
                        0,
                        subreddit.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                subreddit.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        0,
                        subreddit.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            titleString.append(subreddit);
            titleString.append(distingush);
            titleString.append(spacer);

            titleString.append(TimeUtils.getTimeAgo(s.getCreated().getTime(), c));

            desc.setText(titleString);
            ((TextView) rootView.findViewById(R.id.comments))
                    .setText(String.format(Locale.getDefault(), "%d", s.getCommentCount()));
            ((TextView) rootView.findViewById(R.id.score))
                    .setText(String.format(Locale.getDefault(), "%d", s.getScore()));

            if (extras) {
                final ImageView downvotebutton = rootView.findViewById(R.id.downvote);
                final ImageView upvotebutton = rootView.findViewById(R.id.upvote);

                if (s.isArchived() || s.isLocked()) {
                    downvotebutton.setVisibility(View.GONE);
                    upvotebutton.setVisibility(View.GONE);
                } else if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (SettingValues.actionbarVisible
                            && downvotebutton.getVisibility() != View.VISIBLE) {
                        downvotebutton.setVisibility(View.VISIBLE);
                        upvotebutton.setVisibility(View.VISIBLE);
                    }
                    switch (ActionStates.getVoteDirection(s)) {
                        case UPVOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ContextCompat.getColor(c, R.color.md_orange_500));
                                BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.BOLD);
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(),
                                                        "%d",
                                                        (s.getScore()
                                                                + ((s.getAuthor()
                                                                                .equals(
                                                                                        Authentication
                                                                                                .name))
                                                                        ? 0
                                                                        : 1))));
                                BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, Color.WHITE);
                                break;
                            }
                        case DOWNVOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ContextCompat.getColor(c, R.color.md_blue_500));
                                BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.BOLD);
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(),
                                                        "%d",
                                                        (s.getScore()
                                                                + ((s.getAuthor()
                                                                                .equals(
                                                                                        Authentication
                                                                                                .name))
                                                                        ? 0
                                                                        : -1))));
                                BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, Color.WHITE);
                                break;
                            }
                        case NO_VOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ((TextView) rootView.findViewById(R.id.comments))
                                                        .getCurrentTextColor());
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(), "%d", s.getScore()));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.NORMAL);
                                final List<ImageView> imageViewSet =
                                        Arrays.asList(downvotebutton, upvotebutton);
                                BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, Color.WHITE);
                                break;
                            }
                    }
                }
                final ImageView save = (ImageView) rootView.findViewById(R.id.save);
                if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (ActionStates.isSaved(s)) {
                        BlendModeUtil.tintImageViewAsSrcAtop(
                                save, ContextCompat.getColor(c, R.color.md_amber_500));
                    } else {
                        BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE);
                    }
                    save.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            try {
                                                if (ActionStates.isSaved(s)) {
                                                    new AccountManager(Authentication.reddit)
                                                            .unsave(s);
                                                    ActionStates.setSaved(s, false);
                                                } else {
                                                    new AccountManager(Authentication.reddit)
                                                            .save(s);
                                                    ActionStates.setSaved(s, true);
                                                }
                                            } catch (ApiException e) {
                                                e.printStackTrace();
                                            }

                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);

                                            if (ActionStates.isSaved(s)) {
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        save,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_amber_500));
                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        save,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_amber_500));
                                            } else {
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        save, Color.WHITE);
                                            }
                                        }
                                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            });
                }

                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    save.setVisibility(View.GONE);
                }
                try {
                    final TextView points = rootView.findViewById(R.id.score);
                    final TextView comments = rootView.findViewById(R.id.comments);
                    if (Authentication.isLoggedIn && Authentication.didOnline) {
                        {
                            downvotebutton.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);

                                            if (SettingValues.storeHistory) {
                                                if (!s.isNsfw() || SettingValues.storeNSFWHistory) {
                                                    HasSeen.addSeen(s.getFullName());
                                                }
                                            }
                                            if (ActionStates.getVoteDirection(s)
                                                    != VoteDirection
                                                            .DOWNVOTE) { // has not been downvoted
                                                points.setTextColor(
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton, Color.WHITE);

                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        downvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.BOLD);
                                                final int downvoteScore =
                                                        (s.getScore() == 0)
                                                                ? 0
                                                                : s.getScore()
                                                                        - 1; // if a post is at 0
                                                // votes, keep it at 0
                                                // when downvoting
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        downvoteScore));
                                                new Vote(false, points, c).execute(s);
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.DOWNVOTE);
                                            } else {
                                                points.setTextColor(comments.getCurrentTextColor());
                                                new Vote(points, c).execute(s);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.NORMAL);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore()));
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.NO_VOTE);
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton, Color.WHITE);
                                            }
                                        }
                                    });
                        }
                        {
                            upvotebutton.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);

                                            if (SettingValues.storeHistory) {
                                                if (!s.isNsfw() || SettingValues.storeNSFWHistory) {
                                                    HasSeen.addSeen(s.getFullName());
                                                }
                                            }

                                            if (ActionStates.getVoteDirection(s)
                                                    != VoteDirection
                                                            .UPVOTE) { // has not been upvoted
                                                points.setTextColor(
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton, Color.WHITE);

                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        upvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.BOLD);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore() + 1));
                                                new Vote(true, points, c).execute(s);
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.UPVOTE);
                                            } else {
                                                points.setTextColor(comments.getCurrentTextColor());
                                                new Vote(points, c).execute(s);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.NORMAL);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore()));
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.NO_VOTE);
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton, Color.WHITE);
                                            }
                                        }
                                    });
                        }
                    } else {
                        upvotebutton.setVisibility(View.GONE);
                        downvotebutton.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                rootView.findViewById(R.id.menu)
                        .setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        showBottomSheet(c, s, rootView);
                                    }
                                });
            }
        }
    }

    public static void doActionbar(
            final CommentNode node, final View rootView, final Activity c, boolean extras) {
        final Comment s = node.getComment();
        TitleTextView title = rootView.findViewById(R.id.title);
        TextView desc = rootView.findViewById(R.id.desc);
        String distingush = "";
        if (s != null) {
            if (s.getDistinguishedStatus() == DistinguishedStatus.MODERATOR) distingush = "[M]";
            else if (s.getDistinguishedStatus() == DistinguishedStatus.ADMIN) distingush = "[A]";

            SpannableStringBuilder commentTitle = new SpannableStringBuilder();
            SpannableStringBuilder level = new SpannableStringBuilder();
            if (!node.isTopLevel()) {
                level.append("[").append(String.valueOf(node.getDepth())).append("] ");
                level.setSpan(
                        new RelativeSizeSpan(0.7f),
                        0,
                        level.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                commentTitle.append(level);
            }
            commentTitle.append(
                    CompatUtil.fromHtml(s.getDataNode().get("body_html").asText().trim()));
            title.setTextHtml(commentTitle);
            title.setMaxLines(3);

            String spacer = c.getString(R.string.submission_properties_seperator);
            SpannableStringBuilder titleString = new SpannableStringBuilder();

            SpannableStringBuilder author =
                    new SpannableStringBuilder(" /u/" + s.getAuthor() + " ");
            int authorcolor = Palette.getFontColorUser(s.getAuthor());

            if (Authentication.name != null
                    && s.getAuthor()
                            .toLowerCase(Locale.ENGLISH)
                            .equals(Authentication.name.toLowerCase(Locale.ENGLISH))) {
                author.setSpan(
                        new RoundedBackgroundSpan(
                                c, android.R.color.white, R.color.md_deep_orange_300, false),
                        0,
                        author.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (s.getDistinguishedStatus() == DistinguishedStatus.MODERATOR
                    || s.getDistinguishedStatus() == DistinguishedStatus.ADMIN) {
                author.setSpan(
                        new RoundedBackgroundSpan(
                                c, android.R.color.white, R.color.md_green_300, false),
                        0,
                        author.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (authorcolor != 0) {
                author.setSpan(
                        new ForegroundColorSpan(authorcolor),
                        0,
                        author.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            titleString.append(author);
            titleString.append(distingush);
            titleString.append(spacer);

            titleString.append(TimeUtils.getTimeAgo(s.getCreated().getTime(), c));

            desc.setText(titleString);
            ((TextView) rootView.findViewById(R.id.score))
                    .setText(String.format(Locale.getDefault(), "%d", s.getScore()));

            if (extras) {
                final ImageView downvotebutton = rootView.findViewById(R.id.downvote);
                final ImageView upvotebutton = rootView.findViewById(R.id.upvote);

                if (s.isArchived()) {
                    downvotebutton.setVisibility(View.GONE);
                    upvotebutton.setVisibility(View.GONE);
                } else if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (SettingValues.actionbarVisible
                            && downvotebutton.getVisibility() != View.VISIBLE) {
                        downvotebutton.setVisibility(View.VISIBLE);
                        upvotebutton.setVisibility(View.VISIBLE);
                    }
                    switch (ActionStates.getVoteDirection(s)) {
                        case UPVOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ContextCompat.getColor(c, R.color.md_orange_500));
                                BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.BOLD);
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(),
                                                        "%d",
                                                        (s.getScore()
                                                                + ((s.getAuthor()
                                                                                .equals(
                                                                                        Authentication
                                                                                                .name))
                                                                        ? 0
                                                                        : 1))));
                                BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, Color.WHITE);
                                break;
                            }
                        case DOWNVOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ContextCompat.getColor(c, R.color.md_blue_500));
                                BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.BOLD);
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(),
                                                        "%d",
                                                        (s.getScore()
                                                                + ((s.getAuthor()
                                                                                .equals(
                                                                                        Authentication
                                                                                                .name))
                                                                        ? 0
                                                                        : -1))));
                                BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, Color.WHITE);
                                break;
                            }
                        case NO_VOTE:
                            {
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTextColor(
                                                ((TextView) rootView.findViewById(R.id.comments))
                                                        .getCurrentTextColor());
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setText(
                                                String.format(
                                                        Locale.getDefault(), "%d", s.getScore()));
                                ((TextView) rootView.findViewById(R.id.score))
                                        .setTypeface(null, Typeface.NORMAL);
                                final List<ImageView> imageViewSet =
                                        Arrays.asList(downvotebutton, upvotebutton);
                                BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, Color.WHITE);
                                break;
                            }
                    }
                }
                final ImageView save = (ImageView) rootView.findViewById(R.id.save);
                if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (ActionStates.isSaved(s)) {
                        BlendModeUtil.tintImageViewAsSrcAtop(
                                save, ContextCompat.getColor(c, R.color.md_amber_500));
                    } else {
                        BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE);
                    }
                    save.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            try {
                                                if (ActionStates.isSaved(s)) {
                                                    new AccountManager(Authentication.reddit)
                                                            .unsave(s);
                                                    ActionStates.setSaved(s, false);
                                                } else {
                                                    new AccountManager(Authentication.reddit)
                                                            .save(s);
                                                    ActionStates.setSaved(s, true);
                                                }
                                            } catch (ApiException e) {
                                                e.printStackTrace();
                                            }

                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);

                                            if (ActionStates.isSaved(s)) {
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        save,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_amber_500));
                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        save,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_amber_500));
                                            } else {
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        save, Color.WHITE);
                                            }
                                        }
                                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            });
                }

                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    save.setVisibility(View.GONE);
                }
                try {
                    final TextView points = rootView.findViewById(R.id.score);
                    final TextView comments = rootView.findViewById(R.id.comments);
                    if (Authentication.isLoggedIn && Authentication.didOnline) {
                        {
                            downvotebutton.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);

                                            if (ActionStates.getVoteDirection(s)
                                                    != VoteDirection
                                                            .DOWNVOTE) { // has not been downvoted
                                                points.setTextColor(
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton, Color.WHITE);

                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        downvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_blue_500));
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.BOLD);
                                                final int downvoteScore =
                                                        (s.getScore() == 0)
                                                                ? 0
                                                                : s.getScore()
                                                                        - 1; // if a post is at 0
                                                // votes, keep it at 0
                                                // when downvoting
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        downvoteScore));
                                                new Vote(false, points, c).execute(s);
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.DOWNVOTE);
                                            } else {
                                                points.setTextColor(comments.getCurrentTextColor());
                                                new Vote(points, c).execute(s);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.NORMAL);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore()));
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.NO_VOTE);
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton, Color.WHITE);
                                            }
                                        }
                                    });
                        }
                        {
                            upvotebutton.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ((SlidingUpPanelLayout)
                                                            rootView.findViewById(
                                                                    R.id.sliding_layout))
                                                    .setPanelState(
                                                            SlidingUpPanelLayout.PanelState
                                                                    .COLLAPSED);
                                            if (ActionStates.getVoteDirection(s)
                                                    != VoteDirection
                                                            .UPVOTE) { // has not been upvoted
                                                points.setTextColor(
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        downvotebutton, Color.WHITE);

                                                AnimatorUtil.setFlashAnimation(
                                                        rootView,
                                                        upvotebutton,
                                                        ContextCompat.getColor(
                                                                c, R.color.md_orange_500));
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.BOLD);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore() + 1));
                                                new Vote(true, points, c).execute(s);
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.UPVOTE);
                                            } else {
                                                points.setTextColor(comments.getCurrentTextColor());
                                                new Vote(points, c).execute(s);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setTypeface(null, Typeface.NORMAL);
                                                ((TextView) rootView.findViewById(R.id.score))
                                                        .setText(
                                                                String.format(
                                                                        Locale.getDefault(),
                                                                        "%d",
                                                                        s.getScore()));
                                                ActionStates.setVoteDirection(
                                                        s, VoteDirection.NO_VOTE);
                                                BlendModeUtil.tintImageViewAsSrcAtop(
                                                        upvotebutton, Color.WHITE);
                                            }
                                        }
                                    });
                        }
                    } else {
                        upvotebutton.setVisibility(View.GONE);
                        downvotebutton.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    public static void showBottomSheet(
            final Activity mContext, final Submission submission, final View rootView) {

        int[] attrs = new int[] {R.attr.tintColor};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable profile = mContext.getResources().getDrawable(R.drawable.ic_account_circle);
        final Drawable sub = mContext.getResources().getDrawable(R.drawable.ic_bookmark_border);
        final Drawable report = mContext.getResources().getDrawable(R.drawable.ic_report);
        Drawable copy = mContext.getResources().getDrawable(R.drawable.ic_content_copy);
        Drawable open = mContext.getResources().getDrawable(R.drawable.ic_open_in_browser);
        Drawable link = mContext.getResources().getDrawable(R.drawable.ic_link);
        Drawable reddit = mContext.getResources().getDrawable(R.drawable.ic_forum);

        final List<Drawable> drawableSet =
                Arrays.asList(profile, sub, report, copy, open, link, reddit);
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

        ta.recycle();

        BottomSheet.Builder b =
                new BottomSheet.Builder(mContext).title(CompatUtil.fromHtml(submission.getTitle()));

        if (Authentication.didOnline) {
            b.sheet(1, profile, "/u/" + submission.getAuthor())
                    .sheet(2, sub, "/r/" + submission.getSubredditName());
            if (Authentication.isLoggedIn) {
                b.sheet(12, report, mContext.getString(R.string.btn_report));
            }
        }
        b.sheet(7, open, mContext.getString(R.string.open_externally))
                .sheet(4, link, mContext.getString(R.string.submission_share_permalink))
                .sheet(8, reddit, mContext.getString(R.string.submission_share_reddit_url))
                .listener(
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 1:
                                        {
                                            Intent i = new Intent(mContext, Profile.class);
                                            i.putExtra(
                                                    Profile.EXTRA_PROFILE, submission.getAuthor());
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
                                    case 7:
                                        LinkUtil.openExternally(submission.getUrl());
                                        break;
                                    case 4:
                                        Reddit.defaultShareText(
                                                submission.getTitle(),
                                                submission.getUrl(),
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
                                                                new MaterialDialog
                                                                        .SingleButtonCallback() {
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
                                                                                == R.id
                                                                                        .report_other) {
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
                                                                        new PopulateBase
                                                                                        .AsyncReportTask(
                                                                                        submission,
                                                                                        rootView)
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
                                                                    .findViewById(
                                                                            R.id
                                                                                    .input_report_reason)
                                                                    .setVisibility(View.VISIBLE);
                                                        else
                                                            reportDialog
                                                                    .getCustomView()
                                                                    .findViewById(
                                                                            R.id
                                                                                    .input_report_reason)
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
                                                            subHeader,
                                                            reasonGroup.getChildCount() - 2);
                                                }
                                                for (SubredditRule rule :
                                                        rules.getSubredditRules()) {
                                                    if (rule.getKind()
                                                                    == SubredditRule.RuleKind.LINK
                                                            || rule.getKind()
                                                                    == SubredditRule.RuleKind.ALL) {
                                                        RadioButton btn = new RadioButton(mContext);
                                                        btn.setText(rule.getViolationReason());
                                                        reasonGroup.addView(
                                                                btn,
                                                                reasonGroup.getChildCount() - 2);
                                                        btn.getLayoutParams().width =
                                                                WindowManager.LayoutParams
                                                                        .MATCH_PARENT;
                                                    }
                                                }
                                                if (rules.getSiteRules().size() > 0) {
                                                    TextView siteHeader = new TextView(mContext);
                                                    siteHeader.setText(R.string.report_site_rules);
                                                    reasonGroup.addView(
                                                            siteHeader,
                                                            reasonGroup.getChildCount() - 2);
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
                                        if (SettingValues.shareLongLink) {
                                            Reddit.defaultShareText(
                                                    submission.getTitle(),
                                                    "https://reddit.com"
                                                            + submission.getPermalink(),
                                                    mContext);
                                        } else {
                                            Reddit.defaultShareText(
                                                    submission.getTitle(),
                                                    "https://reddit.com/comments/"
                                                            + submission.getId(),
                                                    mContext);
                                        }
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
                                }
                            }
                        });

        b.show();
    }
}
