package me.edgan.redditslide.SubmissionViews;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.cocosw.bottomsheet.BottomSheet;
import com.fasterxml.jackson.databind.JsonNode;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import me.edgan.redditslide.ContentType;
import me.edgan.redditslide.ForceTouch.PeekView;
import me.edgan.redditslide.ForceTouch.PeekViewActivity;
import me.edgan.redditslide.ForceTouch.builder.Peek;
import me.edgan.redditslide.ForceTouch.builder.PeekViewOptions;
import me.edgan.redditslide.ForceTouch.callback.OnButtonUp;
import me.edgan.redditslide.ForceTouch.callback.OnPop;
import me.edgan.redditslide.ForceTouch.callback.OnRemove;
import me.edgan.redditslide.ForceTouch.callback.SimpleOnPeek;
import me.edgan.redditslide.HasSeen;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Views.PeekMediaView;
import me.edgan.redditslide.Views.TransparentTagTextView;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.CompatUtil;
import me.edgan.redditslide.util.LinkUtil;
import me.edgan.redditslide.util.NetworkUtil;

import net.dean.jraw.models.Submission;

import java.util.Arrays;
import java.util.List;

/** Created by carlo_000 on 2/7/2016. */
public class HeaderImageLinkView extends RelativeLayout {
    public String loadedUrl;
    public boolean lq;
    public ImageView thumbImage2;
    public TextView secondTitle;
    public TextView secondSubTitle;
    public View wrapArea;
    String lastDone = "";
    ContentType.Type type;
    DisplayImageOptions bigOptions =
            new DisplayImageOptions.Builder()
                    .resetViewBeforeLoading(false)
                    .cacheOnDisk(true)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .cacheInMemory(false)
                    .displayer(new FadeInBitmapDisplayer(250))
                    .build();
    boolean clickHandled;
    Handler handler;
    MotionEvent event;
    Runnable longClicked;
    float position;
    private TextView title;
    private TextView info;
    public ImageView backdrop;

    public HeaderImageLinkView(Context context) {
        super(context);
        init();
    }

    public HeaderImageLinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeaderImageLinkView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    boolean thumbUsed;

    public void doImageAndText(
            final Submission submission, boolean full, String baseSub, boolean news) {

        backdrop.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        boolean fullImage = ContentType.fullImage(type);
        thumbUsed = false;

        setVisibility(View.VISIBLE);
        String url = "";
        boolean forceThumb = false;
        thumbImage2.setImageResource(android.R.color.transparent);

        boolean loadLq =
                (((!NetworkUtil.isConnectedWifi(getContext()) && SettingValues.lowResMobile)
                        || SettingValues.lowResAlways));

        if (type == ContentType.Type.SELF && SettingValues.hideSelftextLeadImage
                || SettingValues.noImages && submission.isSelfPost()) {
            setVisibility(View.GONE);
            if (wrapArea != null) wrapArea.setVisibility(View.GONE);
            thumbImage2.setVisibility(View.GONE);
        } else {
            if (submission.getThumbnails() != null) {

                int height = submission.getThumbnails().getSource().getHeight();
                int width = submission.getThumbnails().getSource().getWidth();

                if (full) {
                    if (!fullImage && height < dpToPx(50) && type != ContentType.Type.SELF) {
                        forceThumb = true;
                    } else if (SettingValues.cropImage) {
                        backdrop.setLayoutParams(
                                new RelativeLayout.LayoutParams(
                                        LayoutParams.MATCH_PARENT, dpToPx(200)));
                    } else {
                        double h = getHeightFromAspectRatio(height, width);
                        if (h != 0) {
                            if (h > 3200) {
                                backdrop.setLayoutParams(
                                        new RelativeLayout.LayoutParams(
                                                LayoutParams.MATCH_PARENT, 3200));
                            } else {
                                backdrop.setLayoutParams(
                                        new RelativeLayout.LayoutParams(
                                                LayoutParams.MATCH_PARENT, (int) h));
                            }
                        } else {
                            backdrop.setLayoutParams(
                                    new RelativeLayout.LayoutParams(
                                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                        }
                    }
                } else if (SettingValues.bigPicCropped) {
                    if (!fullImage && height < dpToPx(50)) {
                        forceThumb = true;
                    } else {
                        backdrop.setLayoutParams(
                                new RelativeLayout.LayoutParams(
                                        LayoutParams.MATCH_PARENT, dpToPx(200)));
                    }
                } else if (fullImage || height >= dpToPx(50)) {
                    double h = getHeightFromAspectRatio(height, width);
                    if (h != 0) {
                        if (h > 3200) {
                            backdrop.setLayoutParams(
                                    new RelativeLayout.LayoutParams(
                                            LayoutParams.MATCH_PARENT, 3200));
                        } else {
                            backdrop.setLayoutParams(
                                    new RelativeLayout.LayoutParams(
                                            LayoutParams.MATCH_PARENT, (int) h));
                        }
                    } else {
                        backdrop.setLayoutParams(
                                new RelativeLayout.LayoutParams(
                                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    }
                } else {
                    forceThumb = true;
                }
            } else if (type == ContentType.Type.REDDIT_GALLERY) {
                if (full) {
                    backdrop.setLayoutParams(
                            new RelativeLayout.LayoutParams(
                                    LayoutParams.MATCH_PARENT, dpToPx(200)));
                }
            }

            JsonNode thumbnail = submission.getDataNode().get("thumbnail");
            Submission.ThumbnailType thumbnailType;
            if (!submission.getDataNode().get("thumbnail").isNull()) {
                thumbnailType = submission.getThumbnailType();
            } else {
                thumbnailType = Submission.ThumbnailType.NONE;
            }

            JsonNode node = submission.getDataNode();
            if (!SettingValues.ignoreSubSetting
                    && node != null
                    && node.has("sr_detail")
                    && node.get("sr_detail").has("show_media")
                    && !node.get("sr_detail").get("show_media").asBoolean()) {
                thumbnailType = Submission.ThumbnailType.NONE;
            }

            if (SettingValues.noImages && loadLq) {
                setVisibility(View.GONE);
                if (!full && !submission.isSelfPost()) {
                    thumbImage2.setVisibility(View.VISIBLE);
                } else {
                    if (full && !submission.isSelfPost()) wrapArea.setVisibility(View.VISIBLE);
                }
                thumbImage2.setImageDrawable(
                        ContextCompat.getDrawable(getContext(), R.drawable.web));
                thumbUsed = true;
            } else if (submission.isNsfw() && SettingValues.getIsNSFWEnabled()
                    || (baseSub != null
                            && submission.isNsfw()
                            && SettingValues.hideNSFWCollection
                            && (baseSub.equals("frontpage")
                                    || baseSub.equals("all")
                                    || baseSub.contains("+")
                                    || baseSub.equals("popular")))) {
                setVisibility(View.GONE);
                if (!full || forceThumb) {
                    thumbImage2.setVisibility(View.VISIBLE);
                } else {
                    wrapArea.setVisibility(View.VISIBLE);
                }
                if (submission.isSelfPost() && full) {
                    wrapArea.setVisibility(View.GONE);
                } else {
                    thumbImage2.setImageDrawable(
                            ContextCompat.getDrawable(getContext(), R.drawable.nsfw));
                    thumbUsed = true;
                }
                loadedUrl = submission.getUrl();
            } else if (submission.getDataNode().get("spoiler").asBoolean()) {
                setVisibility(View.GONE);
                if (!full || forceThumb) {
                    thumbImage2.setVisibility(View.VISIBLE);
                } else {
                    wrapArea.setVisibility(View.VISIBLE);
                }
                if (submission.isSelfPost() && full) {
                    wrapArea.setVisibility(View.GONE);
                } else {
                    thumbImage2.setImageDrawable(
                            ContextCompat.getDrawable(getContext(), R.drawable.spoiler));
                    thumbUsed = true;
                }
                loadedUrl = submission.getUrl();
            } else if (type == ContentType.Type.GIF) {
                JsonNode dataNode = submission.getDataNode();

                url = submission.getUrl();
                String redditPreviewUrl = null;

                // Check for preview data
                if (dataNode.has("preview")) {
                    JsonNode previewNode = dataNode.get("preview").get("images");
                    if (previewNode != null && previewNode.size() > 0) {
                        JsonNode sourceNode = previewNode.get(0).get("source");
                        if (sourceNode != null && sourceNode.has("url")) {
                            redditPreviewUrl = sourceNode.get("url").asText();
                        }
                    }
                }

                // Use Reddit preview URL if available
                if (redditPreviewUrl != null) {
                    url = redditPreviewUrl;
                }

                // Load the URL
                if (!full && !SettingValues.isPicsEnabled(baseSub)) {
                    thumbImage2.setVisibility(View.VISIBLE);
                    ((Reddit) getContext().getApplicationContext())
                            .getImageLoader()
                            .displayImage(url, thumbImage2);
                    setVisibility(View.GONE);
                } else {
                    backdrop.setVisibility(View.VISIBLE);
                    ((Reddit) getContext().getApplicationContext())
                            .getImageLoader()
                            .displayImage(url, backdrop);
                    setVisibility(View.VISIBLE);
                }
                if (wrapArea != null) wrapArea.setVisibility(View.GONE);
            } else if (type == ContentType.Type.REDDIT_GALLERY) {
                JsonNode dataNode = submission.getDataNode();

                // If this is a crosspost, we need to load the gallery data from the parent
                // submission
                if (dataNode.has("crosspost_parent_list")
                        && dataNode.get("crosspost_parent_list").size() > 0) {
                    dataNode = dataNode.get("crosspost_parent_list").get(0);
                }

                if (dataNode.has("gallery_data")) {
                    JsonNode galleryData = dataNode.get("gallery_data");
                    JsonNode mediaMetadata = dataNode.get("media_metadata");

                    if (galleryData.has("items") && galleryData.get("items").size() > 0) {
                        boolean allFailed = true;
                        for (JsonNode item : galleryData.get("items")) {
                            String mediaId = item.get("media_id").asText();
                            if (mediaMetadata != null && mediaMetadata.has(mediaId)) {
                                JsonNode mediaInfo = mediaMetadata.get(mediaId);
                                if (!"failed".equals(mediaInfo.get("status").asText())) {
                                    allFailed = false;
                                    if (mediaInfo.has("p") && mediaInfo.get("p").size() > 0) {
                                        url =
                                                mediaInfo
                                                        .get("p")
                                                        .get(0)
                                                        .get("u")
                                                        .asText()
                                                        .replace("preview", "i")
                                                        .replaceAll("\\?.*", "");

                                        // Handle the URL similar to regular images
                                        if (!full && !SettingValues.isPicsEnabled(baseSub)
                                                || forceThumb) {
                                            if (!submission.isSelfPost() || full) {
                                                if (!full && thumbImage2 != null) {
                                                    thumbImage2.setVisibility(View.VISIBLE);
                                                } else if (wrapArea != null) {
                                                    wrapArea.setVisibility(View.VISIBLE);
                                                }

                                                loadedUrl = url;
                                                if (!full) {
                                                    ((Reddit) getContext().getApplicationContext())
                                                            .getImageLoader()
                                                            .displayImage(url, thumbImage2);
                                                } else {
                                                    ((Reddit) getContext().getApplicationContext())
                                                            .getImageLoader()
                                                            .displayImage(
                                                                    url, thumbImage2, bigOptions);
                                                }
                                            } else if (thumbImage2 != null) {
                                                thumbImage2.setVisibility(View.GONE);
                                            }
                                            setVisibility(View.GONE);
                                        } else {
                                            loadedUrl = url;
                                            if (!full) {
                                                ((Reddit) getContext().getApplicationContext())
                                                        .getImageLoader()
                                                        .displayImage(url, backdrop);
                                            } else {
                                                ((Reddit) getContext().getApplicationContext())
                                                        .getImageLoader()
                                                        .displayImage(url, backdrop, bigOptions);
                                            }
                                            setVisibility(View.VISIBLE);
                                            if (!full && thumbImage2 != null) {
                                                thumbImage2.setVisibility(View.GONE);
                                            } else if (wrapArea != null) {
                                                wrapArea.setVisibility(View.GONE);
                                            }
                                        }

                                        // Dimension handling
                                        if (mediaInfo.has("s")
                                                && mediaInfo.get("s").has("x")
                                                && mediaInfo.get("s").has("y")) {
                                            int width = mediaInfo.get("s").get("x").asInt();
                                            int height = mediaInfo.get("s").get("y").asInt();
                                            double h = getHeightFromAspectRatio(height, width);
                                            if (h > 0) {
                                                backdrop.setLayoutParams(
                                                        new RelativeLayout.LayoutParams(
                                                                LayoutParams.MATCH_PARENT,
                                                                (int) Math.min(h, 3200)));
                                            }
                                        }
                                    }
                                    break; // Break on the first valid image
                                }
                            }
                        }

                        if (allFailed) {
                            // Handle the case where all media failed
                            setVisibility(View.GONE);
                            if (thumbImage2 != null) thumbImage2.setVisibility(View.GONE);
                            if (wrapArea != null) wrapArea.setVisibility(View.GONE);
                        }
                    } else {
                        // Handle the case where gallery_data is missing or empty
                        setVisibility(View.GONE);
                        if (thumbImage2 != null) thumbImage2.setVisibility(View.GONE);
                        if (wrapArea != null) wrapArea.setVisibility(View.GONE);
                    }
                }
            } else if (type != ContentType.Type.IMAGE
                            && type != ContentType.Type.SELF
                            && (!thumbnail.isNull()
                                    && (thumbnailType != Submission.ThumbnailType.URL))
                    || thumbnail.asText().isEmpty() && !submission.isSelfPost()) {

                setVisibility(View.GONE);
                if (!full) {
                    thumbImage2.setVisibility(View.VISIBLE);
                } else {
                    wrapArea.setVisibility(View.VISIBLE);
                }

                thumbImage2.setImageDrawable(
                        ContextCompat.getDrawable(getContext(), R.drawable.web));
                thumbUsed = true;
                loadedUrl = submission.getUrl();
            } else if (type == ContentType.Type.IMAGE
                    && !thumbnail.isNull()
                    && !thumbnail.asText().isEmpty()) {
                if (loadLq
                        && submission.getThumbnails() != null
                        && submission.getThumbnails().getVariations() != null
                        && submission.getThumbnails().getVariations().length > 0) {

                    if (ContentType.isImgurImage(submission.getUrl())) {
                        url = submission.getUrl();
                        url =
                                url.substring(0, url.lastIndexOf("."))
                                        + (SettingValues.lqLow
                                                ? "m"
                                                : (SettingValues.lqMid ? "l" : "h"))
                                        + url.substring(url.lastIndexOf("."));
                    } else {
                        int length = submission.getThumbnails().getVariations().length;
                        if (SettingValues.lqLow && length >= 3) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[2]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else if (SettingValues.lqMid && length >= 4) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[3]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else if (length >= 5) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[length - 1]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission.getThumbnails().getSource().getUrl())
                                            .toString(); // unescape url characters
                        }
                    }
                    lq = true;

                } else {
                    if (submission.getDataNode().has("preview")
                            && submission
                                    .getDataNode()
                                    .get("preview")
                                    .get("images")
                                    .get(0)
                                    .get("source")
                                    .has(
                                            "height")) { // Load the preview image which has
                                                         // probably already been cached in memory
                                                         // instead of the direct link
                        url =
                                submission
                                        .getDataNode()
                                        .get("preview")
                                        .get("images")
                                        .get(0)
                                        .get("source")
                                        .get("url")
                                        .asText();
                    } else {
                        url = submission.getUrl();
                    }
                }

                if (!full && !SettingValues.isPicsEnabled(baseSub) || forceThumb) {

                    if (!submission.isSelfPost() || full) {
                        if (!full) {
                            thumbImage2.setVisibility(View.VISIBLE);
                        } else {
                            wrapArea.setVisibility(View.VISIBLE);
                        }

                        loadedUrl = url;
                        if (!full) {
                            ((Reddit) getContext().getApplicationContext())
                                    .getImageLoader()
                                    .displayImage(url, thumbImage2);
                        } else {
                            ((Reddit) getContext().getApplicationContext())
                                    .getImageLoader()
                                    .displayImage(url, thumbImage2, bigOptions);
                        }
                    } else {
                        thumbImage2.setVisibility(View.GONE);
                    }
                    setVisibility(View.GONE);

                } else {
                    loadedUrl = url;
                    if (!full) {
                        ((Reddit) getContext().getApplicationContext())
                                .getImageLoader()
                                .displayImage(url, backdrop);
                    } else {
                        ((Reddit) getContext().getApplicationContext())
                                .getImageLoader()
                                .displayImage(url, backdrop, bigOptions);
                    }
                    setVisibility(View.VISIBLE);
                    if (!full) {
                        thumbImage2.setVisibility(View.GONE);
                    } else {
                        wrapArea.setVisibility(View.GONE);
                    }
                }
            } else if (submission.getThumbnails() != null) {

                if (loadLq && submission.getThumbnails().getVariations().length != 0) {
                    if (ContentType.isImgurImage(submission.getUrl())) {
                        url = submission.getUrl();
                        url =
                                url.substring(0, url.lastIndexOf("."))
                                        + (SettingValues.lqLow
                                                ? "m"
                                                : (SettingValues.lqMid ? "l" : "h"))
                                        + url.substring(url.lastIndexOf("."));
                    } else {
                        int length = submission.getThumbnails().getVariations().length;
                        if (SettingValues.lqLow && length >= 3) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[2]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else if (SettingValues.lqMid && length >= 4) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[3]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else if (length >= 5) {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission
                                                            .getThumbnails()
                                                            .getVariations()[length - 1]
                                                            .getUrl())
                                            .toString(); // unescape url characters
                        } else {
                            url =
                                    CompatUtil.fromHtml(
                                                    submission.getThumbnails().getSource().getUrl())
                                            .toString(); // unescape url characters
                        }
                    }
                    lq = true;

                } else {
                    url =
                            CompatUtil.fromHtml(
                                            submission
                                                            .getThumbnails()
                                                            .getSource()
                                                            .getUrl()
                                                            .isEmpty()
                                                    ? submission.getThumbnail()
                                                    : submission
                                                            .getThumbnails()
                                                            .getSource()
                                                            .getUrl())
                                    .toString(); // unescape url characters
                }
                if (!SettingValues.isPicsEnabled(baseSub) && !full
                        || forceThumb
                        || (news && submission.getScore() < 5000)) {

                    if (!full) {
                        thumbImage2.setVisibility(View.VISIBLE);
                    } else {
                        wrapArea.setVisibility(View.VISIBLE);
                    }
                    loadedUrl = url;
                    ((Reddit) getContext().getApplicationContext())
                            .getImageLoader()
                            .displayImage(url, thumbImage2);
                    setVisibility(View.GONE);

                } else {
                    loadedUrl = url;

                    if (!full) {
                        ((Reddit) getContext().getApplicationContext())
                                .getImageLoader()
                                .displayImage(url, backdrop);
                    } else {
                        ((Reddit) getContext().getApplicationContext())
                                .getImageLoader()
                                .displayImage(url, backdrop, bigOptions);
                    }
                    setVisibility(View.VISIBLE);
                    if (!full) {
                        thumbImage2.setVisibility(View.GONE);
                    } else {
                        wrapArea.setVisibility(View.GONE);
                    }
                }
            } else if (!thumbnail.isNull()
                    && submission.getThumbnail() != null
                    && (submission.getThumbnailType() == Submission.ThumbnailType.URL
                            || (!thumbnail.isNull()
                                    && submission.isNsfw()
                                    && SettingValues.getIsNSFWEnabled()))) {

                url = submission.getThumbnail();
                if (!full) {
                    thumbImage2.setVisibility(View.VISIBLE);
                } else {
                    wrapArea.setVisibility(View.VISIBLE);
                }
                loadedUrl = url;

                ((Reddit) getContext().getApplicationContext())
                        .getImageLoader()
                        .displayImage(url, thumbImage2);
                setVisibility(View.GONE);

            } else {

                if (!full) {
                    thumbImage2.setVisibility(View.GONE);
                } else {
                    wrapArea.setVisibility(View.GONE);
                }
                setVisibility(View.GONE);
            }

            if (full) {
                if (wrapArea.getVisibility() == View.VISIBLE) {
                    title = secondTitle;
                    info = secondSubTitle;
                    setBottomSheet(wrapArea, submission, full);
                } else {
                    title = findViewById(R.id.textimage);
                    info = findViewById(R.id.subtextimage);
                    if (forceThumb
                            || (submission.isNsfw()
                                            && submission.getThumbnailType()
                                                    == Submission.ThumbnailType.NSFW
                                    || type != ContentType.Type.IMAGE
                                            && type != ContentType.Type.SELF
                                            && !submission.getDataNode().get("thumbnail").isNull()
                                            && (submission.getThumbnailType()
                                                    != Submission.ThumbnailType.URL))) {
                        setBottomSheet(thumbImage2, submission, full);
                    } else {
                        setBottomSheet(this, submission, full);
                    }
                }
            } else {
                title = findViewById(R.id.textimage);
                info = findViewById(R.id.subtextimage);
                setBottomSheet(thumbImage2, submission, full);
                setBottomSheet(this, submission, full);
            }

            if (SettingValues.smallTag && !full && !news) {
                title = findViewById(R.id.tag);
                findViewById(R.id.tag).setVisibility(View.VISIBLE);
                info = null;
            } else {
                findViewById(R.id.tag).setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                info.setVisibility(View.VISIBLE);
            }

            if (SettingValues.smallTag && !full && !news) {
                ((TransparentTagTextView) title).init(getContext());
            }

            title.setText(ContentType.getContentDescription(submission, getContext()));

            if (info != null) info.setText(submission.getDomain());
        }
    }

    public int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    boolean popped;

    public double getHeightFromAspectRatio(int imageHeight, int imageWidth) {
        double ratio = (double) imageHeight / (double) imageWidth;
        double width = getWidth();
        return (width * ratio);
    }

    public void onLinkLongClick(final String url, MotionEvent event) {
        popped = false;

        if (url == null || SettingValues.noPreviewImageLongClick) {
            return;
        }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        Activity activity = null;
        final Context context = getContext();

        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextThemeWrapper) {
            activity = (Activity) ((ContextThemeWrapper) context).getBaseContext();
        } else if (context instanceof ContextWrapper) {
            Context context1 = ((ContextWrapper) context).getBaseContext();
            if (context1 instanceof Activity) {
                activity = (Activity) context1;
            } else if (context1 instanceof ContextWrapper) {
                Context context2 = ((ContextWrapper) context1).getBaseContext();
                if (context2 instanceof Activity) {
                    activity = (Activity) context2;
                } else if (context2 instanceof ContextWrapper) {
                    activity = (Activity) ((ContextThemeWrapper) context2).getBaseContext();
                }
            }
        } else {
            throw new RuntimeException("Could not find activity from context:" + context);
        }

        if (activity != null && !activity.isFinishing()) {
            if (SettingValues.peek) {
                Peek.into(
                                R.layout.peek_view_submission,
                                new SimpleOnPeek() {
                                    @Override
                                    public void onInflated(
                                            final PeekView peekView, final View rootView) {
                                        // do stuff
                                        TextView text = rootView.findViewById(R.id.title);
                                        text.setText(url);
                                        text.setTextColor(Color.WHITE);
                                        ((PeekMediaView) rootView.findViewById(R.id.peek))
                                                .setUrl(url);

                                        peekView.addButton(
                                                (R.id.share),
                                                new OnButtonUp() {
                                                    @Override
                                                    public void onButtonUp() {
                                                        Reddit.defaultShareText(
                                                                "", url, rootView.getContext());
                                                    }
                                                });

                                        peekView.addButton(
                                                (R.id.upvoteb),
                                                new OnButtonUp() {
                                                    @Override
                                                    public void onButtonUp() {
                                                        ((View) getParent())
                                                                .findViewById(R.id.upvote)
                                                                .callOnClick();
                                                    }
                                                });

                                        peekView.setOnRemoveListener(
                                                new OnRemove() {
                                                    @Override
                                                    public void onRemove() {
                                                        ((PeekMediaView)
                                                                        rootView.findViewById(
                                                                                R.id.peek))
                                                                .doClose();
                                                    }
                                                });

                                        peekView.addButton(
                                                (R.id.comments),
                                                new OnButtonUp() {
                                                    @Override
                                                    public void onButtonUp() {
                                                        ((View) getParent().getParent())
                                                                .callOnClick();
                                                    }
                                                });

                                        peekView.setOnPop(
                                                new OnPop() {
                                                    @Override
                                                    public void onPop() {
                                                        popped = true;
                                                        callOnClick();
                                                    }
                                                });
                                    }
                                })
                        .with(new PeekViewOptions().setFullScreenPeek(true))
                        .show((PeekViewActivity) activity, event);
            } else {
                BottomSheet.Builder b = new BottomSheet.Builder(activity).title(url).grid();
                int[] attrs = new int[] {R.attr.tintColor};
                TypedArray ta = getContext().obtainStyledAttributes(attrs);

                int color = ta.getColor(0, Color.WHITE);
                Drawable open = getResources().getDrawable(R.drawable.ic_open_in_new);
                Drawable share = getResources().getDrawable(R.drawable.ic_share);
                Drawable copy = getResources().getDrawable(R.drawable.ic_content_copy);
                final List<Drawable> drawableSet = Arrays.asList(open, share, copy);
                BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

                ta.recycle();

                b.sheet(R.id.open_link, open, getResources().getString(R.string.open_externally));
                b.sheet(R.id.share_link, share, getResources().getString(R.string.share_link));
                b.sheet(
                        R.id.copy_link,
                        copy,
                        getResources().getString(R.string.submission_link_copy));
                final Activity finalActivity = activity;
                b.listener(
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case R.id.open_link:
                                                LinkUtil.openExternally(url);
                                                break;
                                            case R.id.share_link:
                                                Reddit.defaultShareText("", url, finalActivity);
                                                break;
                                            case R.id.copy_link:
                                                LinkUtil.copyUrl(url, finalActivity);
                                                break;
                                        }
                                    }
                                })
                        .show();
            }
        }
    }

    public void setBottomSheet(View v, final Submission submission, final boolean full) {
        handler = new Handler();
        v.setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        x += getScrollX();
                        y += getScrollY();

                        HeaderImageLinkView.this.event = event;

                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            position = event.getY(); // used to see if the user scrolled or not
                        }
                        if (!(event.getAction() == MotionEvent.ACTION_UP
                                || event.getAction() == MotionEvent.ACTION_DOWN)) {
                            if (Math.abs((position - event.getY())) > 25) {
                                handler.removeCallbacksAndMessages(null);
                            }
                            return false;
                        }

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                clickHandled = false;
                                if (SettingValues.peek) {
                                    handler.postDelayed(
                                            longClicked,
                                            android.view.ViewConfiguration.getTapTimeout() + 50);
                                } else {
                                    handler.postDelayed(
                                            longClicked,
                                            android.view.ViewConfiguration.getLongPressTimeout());
                                }

                                break;
                            case MotionEvent.ACTION_UP:
                                handler.removeCallbacksAndMessages(null);

                                if (!clickHandled) {
                                    // regular click
                                    callOnClick();
                                }
                                break;
                        }
                        return true;
                    }
                });
        longClicked =
                new Runnable() {
                    @Override
                    public void run() {
                        // long click
                        clickHandled = true;

                        handler.removeCallbacksAndMessages(null);
                        if (SettingValues.storeHistory && !full) {
                            if (!submission.isNsfw() || SettingValues.storeNSFWHistory) {
                                HasSeen.addSeen(submission.getFullName());
                                ((View) getParent()).findViewById(R.id.title).setAlpha(0.54f);
                                ((View) getParent()).findViewById(R.id.body).setAlpha(0.54f);
                            }
                        }
                        onLinkLongClick(submission.getUrl(), event);
                    }
                };
    }

    public void setSubmission(
            final Submission submission,
            final boolean full,
            String baseSub,
            ContentType.Type type) {
        this.type = type;
        if (!lastDone.equals(submission.getFullName())) {
            lq = false;
            lastDone = submission.getFullName();
            backdrop.setImageResource(
                    android.R.color
                            .transparent); // reset the image view in case the placeholder is still
                                           // visible
            thumbImage2.setImageResource(android.R.color.transparent);
            doImageAndText(submission, full, baseSub, false);
        }
    }

    public void setSubmissionNews(
            final Submission submission,
            final boolean full,
            String baseSub,
            ContentType.Type type) {
        this.type = type;
        if (!lastDone.equals(submission.getFullName())) {
            lq = false;
            lastDone = submission.getFullName();
            backdrop.setImageResource(
                    android.R.color
                            .transparent); // reset the image view in case the placeholder is still
                                           // visible
            thumbImage2.setImageResource(android.R.color.transparent);
            doImageAndText(submission, full, baseSub, true);
        }
    }

    public void setThumbnail(ImageView v) {
        thumbImage2 = v;
    }

    public void setUrl(String url) {}

    public void setWrapArea(View v) {
        wrapArea = v;
        secondTitle = v.findViewById(R.id.contenttitle);
        secondSubTitle = v.findViewById(R.id.contenturl);
    }

    private void init() {
        inflate(getContext(), R.layout.header_image_title_view, this);
        this.title = findViewById(R.id.textimage);
        this.info = findViewById(R.id.subtextimage);
        this.backdrop = findViewById(R.id.leadimage);
    }
}
