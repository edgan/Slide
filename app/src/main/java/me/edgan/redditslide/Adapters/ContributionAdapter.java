package me.edgan.redditslide.Adapters;

/** Created by ccrama on 3/22/2015. */
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.cocosw.bottomsheet.BottomSheet;
import com.devspark.robototextview.RobotoTypefaces;
import com.google.android.material.snackbar.Snackbar;

import me.edgan.redditslide.ActionStates;
import me.edgan.redditslide.Activities.Profile;
import me.edgan.redditslide.Activities.SubredditView;
import me.edgan.redditslide.Activities.Website;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.HasSeen;
import me.edgan.redditslide.Hidden;
import me.edgan.redditslide.OpenRedditLink;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.SubmissionViews.PopulateSubmissionViewHolder;
import me.edgan.redditslide.Views.CatchStaggeredGridLayoutManager;
import me.edgan.redditslide.Views.CreateCardView;
import me.edgan.redditslide.Visuals.FontPreferences;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.CompatUtil;
import me.edgan.redditslide.util.LayoutUtils;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.MiscUtil;
import me.edgan.redditslide.util.SubmissionParser;
import me.edgan.redditslide.util.TimeUtils;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.List;
import java.util.Locale;

public class ContributionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements BaseAdapter {

    private final int SPACER = 6;
    private static final int COMMENT = 1;
    public final Activity mContext;
    private final RecyclerView listView;
    private final Boolean isHiddenPost;
    public GeneralPosts dataSet;

    public ContributionAdapter(Activity mContext, GeneralPosts dataSet, RecyclerView listView) {
        this.mContext = mContext;
        this.listView = listView;
        this.dataSet = dataSet;

        this.isHiddenPost = false;
    }

    public ContributionAdapter(
            Activity mContext, GeneralPosts dataSet, RecyclerView listView, Boolean isHiddenPost) {
        this.mContext = mContext;
        this.listView = listView;
        this.dataSet = dataSet;

        this.isHiddenPost = isHiddenPost;
    }

    private final int LOADING_SPINNER = 5;
    private final int NO_MORE = 3;

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && !dataSet.posts.isEmpty()) {
            return SPACER;
        } else if (!dataSet.posts.isEmpty()) {
            position -= 1;
        }
        if (position == dataSet.posts.size() && !dataSet.posts.isEmpty() && !dataSet.nomore) {
            return LOADING_SPINNER;
        } else if (position == dataSet.posts.size() && dataSet.nomore) {
            return NO_MORE;
        }
        if (dataSet.posts.get(position) instanceof Comment) return COMMENT;

        return 2;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        if (i == SPACER) {
            View v =
                    LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.spacer, viewGroup, false);
            return new SpacerViewHolder(v);

        } else if (i == COMMENT) {
            View v =
                    LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.profile_comment, viewGroup, false);
            return new ProfileCommentViewHolder(v);
        } else if (i == LOADING_SPINNER) {
            View v =
                    LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.loadingmore, viewGroup, false);
            return new SubmissionFooterViewHolder(v);
        } else if (i == NO_MORE) {
            View v =
                    LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.nomoreposts, viewGroup, false);
            return new SubmissionFooterViewHolder(v);
        } else {
            View v = CreateCardView.CreateView(viewGroup);
            return new SubmissionViewHolder(v);
        }
    }

    public static class SubmissionFooterViewHolder extends RecyclerView.ViewHolder {
        public SubmissionFooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder firstHolder, final int pos) {
        int i = pos != 0 ? pos - 1 : pos;

        if (firstHolder instanceof SubmissionViewHolder) {
            final SubmissionViewHolder holder = (SubmissionViewHolder) firstHolder;
            final Submission submission = (Submission) dataSet.posts.get(i);
            CreateCardView.resetColorCard(holder.itemView);
            if (submission.getSubredditName() != null)
                CreateCardView.colorCard(
                        submission.getSubredditName().toLowerCase(Locale.ENGLISH),
                        holder.itemView,
                        "no_subreddit",
                        false);
            holder.itemView.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            LayoutInflater inflater = mContext.getLayoutInflater();
                            final View dialoglayout = inflater.inflate(R.layout.postmenu, null);
                            final TextView title = dialoglayout.findViewById(R.id.title);
                            title.setText(CompatUtil.fromHtml(submission.getTitle()));

                            ((TextView) dialoglayout.findViewById(R.id.userpopup))
                                    .setText("/u/" + submission.getAuthor());
                            ((TextView) dialoglayout.findViewById(R.id.subpopup))
                                    .setText("/r/" + submission.getSubredditName());
                            dialoglayout
                                    .findViewById(R.id.sidebar)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent i = new Intent(mContext, Profile.class);
                                                    i.putExtra(
                                                            Profile.EXTRA_PROFILE,
                                                            submission.getAuthor());
                                                    mContext.startActivity(i);
                                                }
                                            });

                            dialoglayout
                                    .findViewById(R.id.wiki)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent i =
                                                            new Intent(
                                                                    mContext, SubredditView.class);
                                                    i.putExtra(
                                                            SubredditView.EXTRA_SUBREDDIT,
                                                            submission.getSubredditName());
                                                    mContext.startActivity(i);
                                                }
                                            });

                            dialoglayout
                                    .findViewById(R.id.save)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    if (submission.isSaved()) {
                                                        ((TextView)
                                                                        dialoglayout.findViewById(
                                                                                R.id.savedtext))
                                                                .setText(R.string.submission_save);
                                                    } else {
                                                        ((TextView)
                                                                        dialoglayout.findViewById(
                                                                                R.id.savedtext))
                                                                .setText(
                                                                        R.string
                                                                                .submission_post_saved);
                                                    }
                                                    new AsyncSave(mContext, firstHolder.itemView)
                                                            .execute(submission);
                                                }
                                            });
                            dialoglayout.findViewById(R.id.copy).setVisibility(View.GONE);
                            if (submission.isSaved()) {
                                ((TextView) dialoglayout.findViewById(R.id.savedtext))
                                        .setText(R.string.submission_post_saved);
                            }
                            dialoglayout
                                    .findViewById(R.id.gild)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    String urlString =
                                                            "https://reddit.com"
                                                                    + submission.getPermalink();
                                                    Intent i = new Intent(mContext, Website.class);
                                                    i.putExtra(LinkUtil.EXTRA_URL, urlString);
                                                    mContext.startActivity(i);
                                                }
                                            });
                            dialoglayout
                                    .findViewById(R.id.share)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    if (submission.isSelfPost()) {
                                                        if (SettingValues.shareLongLink) {
                                                            Reddit.defaultShareText(
                                                                    "",
                                                                    "https://reddit.com"
                                                                            + submission
                                                                                    .getPermalink(),
                                                                    mContext);
                                                        } else {
                                                            Reddit.defaultShareText(
                                                                    "",
                                                                    "https://reddit.com/comments/"
                                                                            + submission.getId(),
                                                                    mContext);
                                                        }
                                                    } else {
                                                        new BottomSheet.Builder(mContext)
                                                                .title(
                                                                        R.string
                                                                                .submission_share_title)
                                                                .grid()
                                                                .sheet(R.menu.share_menu)
                                                                .listener(
                                                                        new DialogInterface
                                                                                .OnClickListener() {
                                                                            @Override
                                                                            public void onClick(
                                                                                    DialogInterface
                                                                                            dialog,
                                                                                    int which) {
                                                                                switch (which) {
                                                                                    case R.id
                                                                                            .reddit_url:
                                                                                        if (SettingValues
                                                                                                .shareLongLink) {
                                                                                            Reddit
                                                                                                    .defaultShareText(
                                                                                                            submission
                                                                                                                    .getTitle(),
                                                                                                            "https://reddit.com"
                                                                                                                    + submission
                                                                                                                            .getPermalink(),
                                                                                                            mContext);
                                                                                        } else {
                                                                                            Reddit
                                                                                                    .defaultShareText(
                                                                                                            submission
                                                                                                                    .getTitle(),
                                                                                                            "https://reddit.com/comments/"
                                                                                                                    + submission
                                                                                                                            .getId(),
                                                                                                            mContext);
                                                                                        }
                                                                                        break;
                                                                                    case R.id
                                                                                            .link_url:
                                                                                        Reddit
                                                                                                .defaultShareText(
                                                                                                        submission
                                                                                                                .getTitle(),
                                                                                                        submission
                                                                                                                .getUrl(),
                                                                                                        mContext);
                                                                                        break;
                                                                                }
                                                                            }
                                                                        })
                                                                .show();
                                                    }
                                                }
                                            });
                            if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                                dialoglayout.findViewById(R.id.save).setVisibility(View.GONE);
                                dialoglayout.findViewById(R.id.gild).setVisibility(View.GONE);
                            }
                            title.setBackgroundColor(
                                    Palette.getColor(submission.getSubredditName()));

                            final AlertDialog.Builder builder =
                                    new AlertDialog.Builder(mContext).setView(dialoglayout);
                            final Dialog d = builder.show();
                            dialoglayout
                                    .findViewById(R.id.hide)
                                    .setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    final int pos =
                                                            dataSet.posts.indexOf(submission);
                                                    final Contribution old = dataSet.posts.get(pos);
                                                    dataSet.posts.remove(submission);
                                                    notifyItemRemoved(pos + 1);
                                                    d.dismiss();

                                                    Hidden.setHidden(old);

                                                    Snackbar s =
                                                            Snackbar.make(
                                                                            listView,
                                                                            R.string
                                                                                    .submission_info_hidden,
                                                                            Snackbar.LENGTH_LONG)
                                                                    .setAction(
                                                                            R.string.btn_undo,
                                                                            new View
                                                                                    .OnClickListener() {
                                                                                @Override
                                                                                public void onClick(
                                                                                        View v) {
                                                                                    dataSet.posts
                                                                                            .add(
                                                                                                    pos,
                                                                                                    old);
                                                                                    notifyItemInserted(
                                                                                            pos
                                                                                                    + 1);
                                                                                    Hidden
                                                                                            .undoHidden(
                                                                                                    old);
                                                                                }
                                                                            });
                                                    LayoutUtils.showSnackbar(s);
                                                }
                                            });
                            return true;
                        }
                    });
            new PopulateSubmissionViewHolder()
                    .populateSubmissionViewHolder(
                            holder,
                            submission,
                            mContext,
                            false,
                            false,
                            dataSet.posts,
                            listView,
                            false,
                            false,
                            null,
                            null);

            final ImageView hideButton = holder.itemView.findViewById(R.id.hide);
            if (hideButton != null && isHiddenPost) {
                hideButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final int pos = dataSet.posts.indexOf(submission);
                                final Contribution old = dataSet.posts.get(pos);
                                dataSet.posts.remove(submission);
                                notifyItemRemoved(pos + 1);

                                Hidden.undoHidden(old);
                            }
                        });
            }
            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = "www.reddit.com" + submission.getPermalink();
                            url = url.replace("?ref=search_posts", "");
                            OpenRedditLink.openUrl(mContext, url, true);
                            if (SettingValues.storeHistory) {
                                if (SettingValues.storeNSFWHistory && submission.isNsfw()
                                        || !submission.isNsfw())
                                    HasSeen.addSeen(submission.getFullName());
                            }

                            notifyItemChanged(pos);
                        }
                    });

        } else if (firstHolder instanceof ProfileCommentViewHolder) {
            // IS COMMENT
            ProfileCommentViewHolder holder = (ProfileCommentViewHolder) firstHolder;
            final Comment comment = (Comment) dataSet.posts.get(i);

            String scoreText;
            if (comment.isScoreHidden()) {
                scoreText =
                        "[" + mContext.getString(R.string.misc_score_hidden).toUpperCase() + "]";
            } else {
                scoreText = String.format(Locale.getDefault(), "%d", comment.getScore());
            }

            SpannableStringBuilder score = new SpannableStringBuilder(scoreText);

            if (score == null || score.toString().isEmpty()) {
                score = new SpannableStringBuilder("0");
            }
            if (!scoreText.contains("[")) {
                score.append(
                        String.format(
                                Locale.getDefault(),
                                " %s",
                                mContext.getResources()
                                        .getQuantityString(R.plurals.points, comment.getScore())));
            }
            holder.score.setText(score);

            if (Authentication.isLoggedIn) {
                if (ActionStates.getVoteDirection(comment) == VoteDirection.UPVOTE) {
                    holder.score.setTextColor(
                            mContext.getResources().getColor(R.color.md_orange_500));
                } else if (ActionStates.getVoteDirection(comment) == VoteDirection.DOWNVOTE) {
                    holder.score.setTextColor(
                            mContext.getResources().getColor(R.color.md_blue_500));
                } else {
                    holder.score.setTextColor(holder.time.getCurrentTextColor());
                }
            }
            String spacer = mContext.getString(R.string.submission_properties_seperator);
            SpannableStringBuilder titleString = new SpannableStringBuilder();

            String timeAgo = TimeUtils.getTimeAgo(comment.getCreated().getTime(), mContext);
            String time =
                    ((timeAgo == null || timeAgo.isEmpty())
                            ? "just now"
                            : timeAgo); // some users were crashing here
            time =
                    time
                            + (((comment.getEditDate() != null)
                                    ? " (edit "
                                            + TimeUtils.getTimeAgo(
                                                    comment.getEditDate().getTime(), mContext)
                                            + ")"
                                    : ""));
            titleString.append(time);
            titleString.append(spacer);

            if (comment.getSubredditName() != null) {
                String subname = comment.getSubredditName();
                SpannableStringBuilder subreddit = new SpannableStringBuilder("/r/" + subname);
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
            }

            holder.time.setText(titleString);
            setViews(
                    comment.getDataNode().get("body_html").asText(),
                    comment.getSubredditName(),
                    holder);

            int type = new FontPreferences(mContext).getFontTypeComment().getTypeface();
            Typeface typeface;
            if (type >= 0) {
                typeface = RobotoTypefaces.obtainTypeface(mContext, type);
            } else {
                typeface = Typeface.DEFAULT;
            }
            holder.content.setTypeface(typeface);

            ((TextView) holder.gild).setText("");
            if (!SettingValues.hideCommentAwards
                    && (comment.getTimesSilvered() > 0
                            || comment.getTimesGilded() > 0
                            || comment.getTimesPlatinized() > 0)) {
                TypedArray a =
                        mContext.obtainStyledAttributes(
                                new FontPreferences(mContext).getPostFontStyle().getResId(),
                                R.styleable.FontStyle);
                int fontsize =
                        (int)
                                (a.getDimensionPixelSize(R.styleable.FontStyle_font_cardtitle, -1)
                                        * .75);
                a.recycle();
                holder.gild.setVisibility(View.VISIBLE);
                // Add silver, gold, platinum icons and counts in that order
                MiscUtil.addAwards(
                        mContext, fontsize, holder, comment.getTimesSilvered(), R.drawable.silver);
                MiscUtil.addAwards(
                        mContext, fontsize, holder, comment.getTimesGilded(), R.drawable.gold);
                MiscUtil.addAwards(
                        mContext,
                        fontsize,
                        holder,
                        comment.getTimesPlatinized(),
                        R.drawable.platinum);
            } else if (holder.gild.getVisibility() == View.VISIBLE)
                holder.gild.setVisibility(View.GONE);

            if (comment.getSubmissionTitle() != null)
                holder.title.setText(CompatUtil.fromHtml(comment.getSubmissionTitle()));
            else holder.title.setText(CompatUtil.fromHtml(comment.getAuthor()));

            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OpenRedditLink.openUrl(
                                    mContext,
                                    comment.getSubmissionId(),
                                    comment.getSubredditName(),
                                    comment.getId());
                        }
                    });
            holder.content.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OpenRedditLink.openUrl(
                                    mContext,
                                    comment.getSubmissionId(),
                                    comment.getSubredditName(),
                                    comment.getId());
                        }
                    });

        } else if (firstHolder instanceof SpacerViewHolder) {
            firstHolder.itemView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            firstHolder.itemView.getWidth(),
                            mContext.findViewById(R.id.header).getHeight()));
            if (listView.getLayoutManager() instanceof CatchStaggeredGridLayoutManager) {
                CatchStaggeredGridLayoutManager.LayoutParams layoutParams =
                        new CatchStaggeredGridLayoutManager.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                mContext.findViewById(R.id.header).getHeight());
                layoutParams.setFullSpan(true);
                firstHolder.itemView.setLayoutParams(layoutParams);
            }
        }
    }

    public static class SpacerViewHolder extends RecyclerView.ViewHolder {
        public SpacerViewHolder(View itemView) {
            super(itemView);
        }
    }

    private void setViews(String rawHTML, String subredditName, ProfileCommentViewHolder holder) {
        if (rawHTML.isEmpty()) {
            return;
        }

        List<String> blocks = SubmissionParser.getBlocks(rawHTML);

        int startIndex = 0;
        // the <div class="md"> case is when the body contains a table or code block first
        if (!blocks.get(0).equals("<div class=\"md\">")) {
            holder.content.setVisibility(View.VISIBLE);
            holder.content.setTextHtml(blocks.get(0), subredditName);
            startIndex = 1;
        } else {
            holder.content.setText("");
            holder.content.setVisibility(View.GONE);
        }

        if (blocks.size() > 1) {
            if (startIndex == 0) {
                holder.overflow.setViews(blocks, subredditName);
            } else {
                holder.overflow.setViews(blocks.subList(startIndex, blocks.size()), subredditName);
            }
        } else {
            holder.overflow.removeAllViews();
        }
    }

    @Override
    public int getItemCount() {
        if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            return 0;
        } else {
            return dataSet.posts.size() + 2;
        }
    }

    @Override
    public void setError(Boolean b) {
        listView.setAdapter(new ErrorAdapter());
    }

    @Override
    public void undoSetError() {
        listView.setAdapter(this);
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
