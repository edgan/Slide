package me.ccrama.redditslide;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Movie;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.FileInputStream;
import org.apache.commons.io.FileUtils;
import java.io.IOException;
import android.text.style.DynamicDrawableSpan;
import android.text.Layout;
import android.graphics.Rect;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.cocosw.bottomsheet.BottomSheet;
import com.devspark.robototextview.widget.RobotoTextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ccrama.redditslide.Activities.Album;
import me.ccrama.redditslide.Activities.AlbumPager;
import me.ccrama.redditslide.Activities.MediaView;
import me.ccrama.redditslide.Activities.TumblrPager;
import me.ccrama.redditslide.ForceTouch.PeekView;
import me.ccrama.redditslide.ForceTouch.PeekViewActivity;
import me.ccrama.redditslide.ForceTouch.builder.Peek;
import me.ccrama.redditslide.ForceTouch.builder.PeekViewOptions;
import me.ccrama.redditslide.ForceTouch.callback.OnButtonUp;
import me.ccrama.redditslide.ForceTouch.callback.OnPop;
import me.ccrama.redditslide.ForceTouch.callback.OnRemove;
import me.ccrama.redditslide.ForceTouch.callback.SimpleOnPeek;
import me.ccrama.redditslide.SubmissionViews.OpenVRedditTask;
import me.ccrama.redditslide.Views.CustomQuoteSpan;
import me.ccrama.redditslide.Views.PeekMediaView;
import me.ccrama.redditslide.Visuals.Palette;
import me.ccrama.redditslide.handler.TextViewLinkHandler;
import me.ccrama.redditslide.util.BlendModeUtil;
import me.ccrama.redditslide.util.ClipboardUtil;
import me.ccrama.redditslide.util.CompatUtil;
import me.ccrama.redditslide.util.LinkUtil;
import me.ccrama.redditslide.util.LogUtil;
import me.ccrama.redditslide.util.GifDrawable;
import me.ccrama.redditslide.util.GifUtils;
import me.ccrama.redditslide.util.AnimatedImageSpan;

/**
 * Created by carlo_000 on 1/11/2016.
 */
public class SpoilerRobotoTextView extends RobotoTextView implements ClickableText {
    private              List<CharacterStyle> storedSpoilerSpans  = new ArrayList<>();
    private              List<Integer>        storedSpoilerStarts = new ArrayList<>();
    private              List<Integer>        storedSpoilerEnds   = new ArrayList<>();
    private static final Pattern              htmlSpoilerPattern  =
            Pattern.compile("<a href=\"[#/](?:spoiler|sp|s)\">([^<]*)</a>");
    private static final Pattern nativeSpoilerPattern =
            Pattern.compile("<span class=\"[^\"]*md-spoiler-text+[^\"]*\">([^<]*)</span>");

    public SpoilerRobotoTextView(Context context) {
        super(context);
        setLineSpacing(0, 1.1f);
    }

    public SpoilerRobotoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLineSpacing(0, 1.1f);
    }

    public SpoilerRobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLineSpacing(0, 1.1f);
    }

    public boolean isSpoilerClicked() {
        return spoilerClicked;
    }

    public void resetSpoilerClicked() {
        spoilerClicked = false;
    }

    public boolean spoilerClicked = false;

    private static SpannableStringBuilder removeNewlines(SpannableStringBuilder s) {
        int start = 0;
        int end = s.length();
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }

        return (SpannableStringBuilder) s.subSequence(start, end);
    }

    /**
     * Set the text from html. Handles formatting spoilers, links etc. <p/> The text must be valid
     * html.
     *
     * @param text html text
     */
    public void setTextHtml(CharSequence text) {
        setTextHtml(text, "");
    }

    /**
     * Set the text from html. Handles formatting spoilers, links etc. <p/> The text must be valid
     * html.
     *
     * @param baseText  html text
     * @param subreddit the subreddit to theme
     */
    public void setTextHtml(CharSequence baseText, String subreddit) {
        String text = wrapAlternateSpoilers(saveEmotesFromDestruction(baseText.toString().trim()));
        SpannableStringBuilder builder = (SpannableStringBuilder) CompatUtil.fromHtml(text);

        replaceQuoteSpans(
                builder); //replace the <blockquote> blue line with something more colorful

        if (text.contains("free_emotes_pack") || text.contains("giphy")) {
            setEmoteText(text, this);
        }
        if (text.contains("<a")) {
            setEmoteSpans(builder); //for emote enabled subreddits
        }
        if (text.contains("[")) {
            setCodeFont(builder);
            setSpoilerStyle(builder, subreddit);
        }
        if (text.contains("[[d[")) {
            setStrikethrough(builder);
        }
        if (text.contains("[[h[")) {
            setHighlight(builder, subreddit);
        }
        if (subreddit != null && !subreddit.isEmpty()) {
            setMovementMethod(new TextViewLinkHandler(this, subreddit, builder));
            setFocusable(false);
            setClickable(false);
            if (subreddit.equals("FORCE_LINK_CLICK")) {
                setLongClickable(false);
            }

        }

        builder = removeNewlines(builder);

        builder.append(" ");

        super.setText(builder, BufferType.SPANNABLE);
    }


    /**
     * Replaces the blue line produced by <blockquote>s with something more visible
     *
     * @param spannable parsed comment text #fromHtml
     */
    private void replaceQuoteSpans(Spannable spannable) {
        QuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);

        for (QuoteSpan quoteSpan : quoteSpans) {
            final int start = spannable.getSpanStart(quoteSpan);
            final int end = spannable.getSpanEnd(quoteSpan);
            final int flags = spannable.getSpanFlags(quoteSpan);

            spannable.removeSpan(quoteSpan);

            //If the theme is Light or Sepia, use a darker blue; otherwise, use a lighter blue
            final int barColor = ContextCompat.getColor(getContext(),
                    SettingValues.currentTheme == 1 || SettingValues.currentTheme == 5
                            ? R.color.md_blue_600 : R.color.md_blue_400);

            final int BAR_WIDTH = 4;
            final int GAP = 5;

            spannable.setSpan(new CustomQuoteSpan(
                            barColor, //bar color
                            BAR_WIDTH, //bar width
                            GAP), //bar + text gap
                    start, end, flags);
        }
    }

    private String wrapAlternateSpoilers(String html) {
        String replacement = "<a href=\"/spoiler\">spoiler&lt; [[s[$1]s]]</a>";

        html = htmlSpoilerPattern.matcher(html).replaceAll(replacement);
        html = nativeSpoilerPattern.matcher(html).replaceAll(replacement);
        return html;
    }

    private String saveEmotesFromDestruction(String html) {
        //Emotes often have no spoiler caption, and therefore are converted to empty anchors. Html.fromHtml removes anchors with zero length node text. Find zero length anchors that start with "/" and add "." to them.
        Pattern htmlEmotePattern = Pattern.compile("<a href=\"/.*\"></a>");
        Matcher htmlEmoteMatcher = htmlEmotePattern.matcher(html);
        while (htmlEmoteMatcher.find()) {
            String newPiece = htmlEmoteMatcher.group();
            //Ignore empty tags marked with sp.
            if (!htmlEmoteMatcher.group().contains("href=\"/sp\"")) {
                newPiece = newPiece.replace("></a", ">.</a");
                html = html.replace(htmlEmoteMatcher.group(), newPiece);
            }
        }
        return html;
    }

    // List to keep track of active GifDrawables to manage their lifecycle
    private List<GifDrawable> activeGifDrawables = new ArrayList<>();

    private SpannableStringBuilder currentBuilder;
    private final Object spanLock = new Object();

    private static class PendingEmoteSpan {
        DynamicDrawableSpan span;
        String emoteName;
        int index;

        PendingEmoteSpan(DynamicDrawableSpan span, String emoteName, int index) {
            this.span = span;
            this.emoteName = emoteName;
            this.index = index;
        }
    }

    private final List<EmoteDrawInfo> emoteDrawables = new ArrayList<>();

    private static class EmoteDrawInfo {
        GifDrawable drawable;
        int position;  // Character position in text
        float x;       // Cached x position
        float y;       // Cached y position
        boolean isValid;

        EmoteDrawInfo(GifDrawable drawable, int position) {
            this.drawable = drawable;
            this.position = position;
        }
    }

    private final List<PendingEmoteSpan> pendingSpans = new ArrayList<>();

    public static boolean findObjectReplacementChar(CharSequence text) {
        boolean atStart = text.charAt(0) == '\uFFFC';

        if (atStart) return true;

        return false;
    }

    private void setEmoteSpan(DynamicDrawableSpan span, String emoteName, int position) {
        if (span == null || emoteName == null) {
            Log.e("EmoteDebug", "Null span or emote name in setEmoteSpan");
            return;
        }

        synchronized(spanLock) {
            try {
                if (!isAttachedToWindow()) {
                    Log.d("EmoteDebug", "View not attached, queueing update for " + emoteName);
                    pendingSpans.add(new PendingEmoteSpan((AnimatedImageSpan) span, emoteName, position));
                    return;
                }

                // Get current text and ensure it's a SpannableStringBuilder
                CharSequence current = getText();
                SpannableStringBuilder text;
                if (current instanceof SpannableStringBuilder) {
                    text = (SpannableStringBuilder) current;
                } else {
                    text = new SpannableStringBuilder(current != null ? current : "");
                    setText(text);
                }

                // Find position for emote
                String content = text.toString();
                int pos = -1; // Initialize pos
                int count = 0;

                while (count <= position) {
                    pos = content.indexOf('\uFFFC', pos + 1); // '\uFFFC' is the object replacement character
                    if (pos == -1) {
                        Log.e("EmoteDebug", "Could not find position for emote " + emoteName);
                        return;
                    }
                    count++;
                }

                // Remove any existing spans at this position
                ImageSpan[] existingSpans = text.getSpans(pos, pos + 1, ImageSpan.class);
                for (ImageSpan existingSpan : existingSpans) {
                    text.removeSpan(existingSpan);
                }

                // Set the new AnimatedImageSpan
                text.setSpan(span, pos, pos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (SettingValues.commentEmoteAnimation) {
                    // Start the GIF animation and add to active list if it's an AnimatedImageSpan
                    if (span instanceof AnimatedImageSpan) {
                        AnimatedImageSpan animatedSpan = (AnimatedImageSpan) span;
                        animatedSpan.start();
                        emoteDrawables.add(new EmoteDrawInfo(animatedSpan.getGifDrawable(), pos));
                    }
                }

                // Force layout update
                requestLayout();

            } catch (Exception e) {
                Log.e("EmoteDebug", "Error setting emote drawable for " + emoteName, e);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        synchronized(spanLock) {
            // First apply any pending spans
            if (!pendingSpans.isEmpty()) {
                Log.d("EmoteDebug", "Applying " + pendingSpans.size() + " pending spans");
                for (PendingEmoteSpan pendingSpan : pendingSpans) {
                    setEmoteSpan(pendingSpan.span, pendingSpan.emoteName, pendingSpan.index);
                }
                pendingSpans.clear();
            }

            // Start all spans
            CharSequence text = getText();
            if (text instanceof Spannable) {
                Spannable spannable = (Spannable) text;
                DynamicDrawableSpan[] spans = spannable.getSpans(0, spannable.length(), DynamicDrawableSpan.class);
                for (DynamicDrawableSpan span : spans) {
                    if (span instanceof AnimatedImageSpan) {
                        ((AnimatedImageSpan) span).onAttached();
                    }
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        synchronized(spanLock) {
            // Stop all spans
            CharSequence text = getText();
            if (text instanceof Spannable) {
                Spannable spannable = (Spannable) text;
                DynamicDrawableSpan[] spans = spannable.getSpans(0, spannable.length(), DynamicDrawableSpan.class);
                for (DynamicDrawableSpan span : spans) {
                    if (span instanceof AnimatedImageSpan) {
                        ((AnimatedImageSpan) span).onDetached();
                    }
                }
            }
        }
        super.onDetachedFromWindow();
    }

    private void cleanupGifs() {
        synchronized(spanLock) {
            // Stop all active GIF animations
            for (EmoteDrawInfo info : emoteDrawables) {
                info.drawable.stop();
            }
            emoteDrawables.clear();

            // Remove all AnimatedImageSpan instances from the text
            CharSequence currentText = getText();
            if (currentText instanceof Spannable) {
                Spannable spannable = (Spannable) currentText;
                AnimatedImageSpan[] spans = spannable.getSpans(0, spannable.length(), AnimatedImageSpan.class);
                for (AnimatedImageSpan span : spans) {
                    spannable.removeSpan(span);
                }
            }

            // Clear pending spans
            pendingSpans.clear();
        }
    }

    private void setEmoteText(String text, TextView textView) {
        if (text == null || textView == null) {
            Log.e("EmoteDebug", "Null text or textview in setEmoteText");
            return;
        }

        try {
            // Clear existing state
            cleanupGifs();

            Pattern redditPattern = Pattern.compile(
                "<img\\s+src=\\\"(https://www\\.redditstatic\\.com/marketplace-assets/v1/core/emotes/snoomoji_emotes/free_emotes_pack/([^/\\\"]+)\\.gif)\\\"[^>]*>");
            Pattern giphyPattern = Pattern.compile(
                "<img\\s+src=\\\"(https://external-preview\\.redd\\.it/([^?]+)\\?width=([0-9]+)&height=([0-9]+)&s=([^\\\"]+))\\\"[^>]*>"
            );

            List<EmoteSpanRequest> spanRequests = new ArrayList<>();
            StringBuilder processedText = new StringBuilder();

            // Strip divs first
            text = text.replaceAll("<div class=\\\"md\\\"><div>", "")
                      .replaceAll("</div>", "");

            // Process the text for both patterns
            processPattern(text, redditPattern, processedText, spanRequests);
            processPattern(text, giphyPattern, processedText, spanRequests);

            // Create builder and ensure it's a SpannableStringBuilder
            SpannableStringBuilder builder = new SpannableStringBuilder(processedText);

            // Set initial text with the builder
            setText(builder);

            // Load GIFs
            for (EmoteSpanRequest request : spanRequests) {
                loadGifEmote(request, textView, request.start);
            }

        } catch (Exception e) {
            Log.e("EmoteDebug", "Error in setEmoteText", e);
        }
    }

    private void processPattern(String text, Pattern pattern, StringBuilder processedText, List<EmoteSpanRequest> spanRequests) {
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        int emoteCount = spanRequests.size();

        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();

            // Append text before the match
            if (matchStart > lastEnd) {
                processedText.append(text.substring(lastEnd, matchStart));
            }

            try {
                String gifUrl = matcher.group(1);
                String emoteName = matcher.group(2);

                if (gifUrl != null && emoteName != null) {
                    processedText.append("\uFFFC"); // Object replacement character

                    spanRequests.add(new EmoteSpanRequest(
                        gifUrl,
                        emoteCount,
                        emoteCount + 1,
                        emoteName
                    ));

                    emoteCount++;
                }
            } catch (IllegalStateException | IndexOutOfBoundsException e) {
                Log.e("EmoteDebug", "Error processing match groups", e);
            }

            lastEnd = matchEnd;
        }

        // Safely append remaining text after last match
        if (lastEnd < text.length()) {
            processedText.append(text.substring(lastEnd));
        }
    }

    private void loadGifEmote(EmoteSpanRequest request, TextView textView, int pos) {
        Log.d("EmoteDebug", "Starting GIF download for: " + request.gifUrl);

        GifUtils.downloadGif(request.gifUrl, new GifUtils.GifDownloadCallback() {
            @Override
            public void onGifDownloaded(File gifFile) {
                float scale = 0.5f;
                try {
                    Movie movie = Movie.decodeFile(gifFile.getAbsolutePath());
                    if (movie != null) {
                        // After decoding the movie and obtaining width and height
                        int intrinsicHeight = movie.height();
                        int intrinsicWidth = movie.width();

                        int height = (int) (intrinsicHeight * scale);
                        int width = (int) (intrinsicWidth * scale);

                        GifDrawable gifDrawable = new GifDrawable(movie, null);
                        gifDrawable.setBounds(0, 0, 0, height);

                        Log.d("EmoteDebug", "Created drawable for " + request.emoteName + " with bounds " + height + "x" + width);

                        // Wrap GifDrawable in AnimatedImageSpan
                        AnimatedImageSpan animatedSpan = new AnimatedImageSpan(gifDrawable, SpoilerRobotoTextView.this);

                        // Post to UI thread
                        textView.post(() -> {
                            synchronized(spanLock) {
                                try {
                                    if (!isAttachedToWindow()) {
                                        Log.d("EmoteDebug", "View not attached, queueing update for " + request.emoteName);
                                        pendingSpans.add(new PendingEmoteSpan(animatedSpan, request.emoteName, pos));
                                        return;
                                    }

                                    CharSequence currentText = getText();
                                    if (currentText instanceof Spannable) {
                                        Spannable spannable = (Spannable) currentText;
                                        int start = spannable.toString().indexOf(currentText.toString());
                                        int end = start + currentText.toString().length();

                                        boolean found = findObjectReplacementChar(currentText);
                                        if (found) {
                                            spannable.setSpan(animatedSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                                        } else {
                                            spannable.setSpan(animatedSpan, end - 2, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                                        }
                                    }

                                    // Explicitly call onAttached for the new span
                                    animatedSpan.onAttached();

                                    if (SettingValues.commentEmoteAnimation) {
                                        animatedSpan.start();
                                    }

                                    emoteDrawables.add(new EmoteDrawInfo(animatedSpan.getGifDrawable(), pos));

                                    // Force layout update
                                    requestLayout();

                                } catch (Exception e) {
                                    Log.e("EmoteDebug", "Error setting emote drawable for " + request.emoteName, e);
                                }
                            }
                        });
                    } else {
                        Log.e("EmoteDebug", "Failed to decode movie for: " + request.emoteName);
                    }
                } catch (Exception e) {
                    Log.e("EmoteDebug", "Error processing GIF: " + request.emoteName, e);
                }
            }

            @Override
            public void onGifDownloadFailed(Exception e) {
                Log.e("EmoteDebug", "Failed to download GIF: " + request.gifUrl, e);
            }
        }, textView.getContext());
    }

    // Helper class to keep track of span requests
    private static class EmoteSpanRequest {
        String gifUrl;
        int start;
        int end;
        String emoteName;

        EmoteSpanRequest(String gifUrl, int start, int end, String emoteName) {
            this.gifUrl = gifUrl;
            this.start = start;
            this.end = end;
            this.emoteName = emoteName;
        }
    }

    private void setEmoteSpans(SpannableStringBuilder builder) {
        for (URLSpan span : builder.getSpans(0, builder.length(), URLSpan.class)) {
            if (SettingValues.typeInText) {
                setLinkTypes(builder, span);
            }
            if (SettingValues.largeLinks) {
                setLargeLinks(builder, span);
            }
            File emoteDir = new File(Environment.getExternalStorageDirectory(), "RedditEmotes");
            File emoteFile = new File(emoteDir, span.getURL().replace("/", "").replaceAll("-.*", "")
                    + ".png"); //BPM uses "-" to add dynamics for emotes in browser. Fall back to original here if exists.
            boolean startsWithSlash = span.getURL().startsWith("/");
            boolean hasOnlyOneSlash = StringUtils.countMatches(span.getURL(), "/") == 1;

            if (emoteDir.exists() && startsWithSlash && hasOnlyOneSlash && emoteFile.exists()) {
                //We've got an emote match
                int start = builder.getSpanStart(span);
                int end = builder.getSpanEnd(span);
                CharSequence textCovers = builder.subSequence(start, end);

                //Make sure bitmap loaded works well with screen density.
                BitmapFactory.Options options = new BitmapFactory.Options();
                DisplayMetrics metrics = new DisplayMetrics();
                ContextCompat.getSystemService(getContext(), WindowManager.class).getDefaultDisplay().getMetrics(metrics);
                options.inDensity = 240;
                options.inScreenDensity = metrics.densityDpi;
                options.inScaled = true;

                //Since emotes are not directly attached to included text, add extra character to attach image to.
                builder.removeSpan(span);
                if (builder.subSequence(start, end).charAt(0) != '.') {
                    builder.insert(start, ".");
                }
                Bitmap emoteBitmap = BitmapFactory.decodeFile(emoteFile.getAbsolutePath(), options);
                builder.setSpan(new ImageSpan(getContext(), emoteBitmap), start, start + 1,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                //Check if url span has length. If it does, it's a spoiler/caption
                if (textCovers.length() > 1) {
                    builder.setSpan(new URLSpan("/sp"), start + 1, end + 1,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    builder.setSpan(new StyleSpan(Typeface.ITALIC), start + 1, end + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                builder.append("\n"); //Newline to fix text wrapping issues
            }
        }
    }

    private void setLinkTypes(SpannableStringBuilder builder, URLSpan span) {
        String url = span.getURL();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String text = builder.subSequence(builder.getSpanStart(span), builder.getSpanEnd(span))
                .toString();
        if (!text.equalsIgnoreCase(url)) {
            ContentType.Type contentType = ContentType.getContentType(url);
            String bod;
            try {
                bod = " (" + ((url.contains("/") && url.startsWith("/") && !(url.split("/").length
                        > 2)) ? url
                        : (getContext().getString(ContentType.getContentID(contentType, false)) + (
                                contentType == ContentType.Type.LINK ? " " + Uri.parse(url)
                                        .getHost() : ""))) + ")";
            } catch (Exception e) {
                bod = " ("
                        + getContext().getString(ContentType.getContentID(contentType, false))
                        + ")";
            }
            SpannableStringBuilder b = new SpannableStringBuilder(bod);
            b.setSpan(new StyleSpan(Typeface.BOLD), 0, b.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.setSpan(new RelativeSizeSpan(0.8f), 0, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.insert(builder.getSpanEnd(span), b);
        }
    }

    private void setLargeLinks(SpannableStringBuilder builder, URLSpan span) {
        builder.setSpan(new RelativeSizeSpan(1.3f), builder.getSpanStart(span),
                builder.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setStrikethrough(SpannableStringBuilder builder) {
        final int offset = "[[d[".length(); // == "]d]]".length()

        int start = -1;
        int end;

        for (int i = 0; i < builder.length() - 3; i++) {
            if (builder.charAt(i) == '['
                    && builder.charAt(i + 1) == '['
                    && builder.charAt(i + 2) == 'd'
                    && builder.charAt(i + 3) == '[') {
                start = i + offset;
            } else if (builder.charAt(i) == ']'
                    && builder.charAt(i + 1) == 'd'
                    && builder.charAt(i + 2) == ']'
                    && builder.charAt(i + 3) == ']') {
                end = i;
                builder.setSpan(new StrikethroughSpan(), start, end,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                builder.delete(end, end + offset);
                builder.delete(start - offset, start);
                i -= offset + (end - start); // length of text
            }
        }
    }

    private void setHighlight(SpannableStringBuilder builder, String subreddit) {
        final int offset = "[[h[".length(); // == "]h]]".length()

        int start = -1;
        int end;
        for (int i = 0; i < builder.length() - 4; i++) {
            if (builder.charAt(i) == '['
                    && builder.charAt(i + 1) == '['
                    && builder.charAt(i + 2) == 'h'
                    && builder.charAt(i + 3) == '[') {
                start = i + offset;
            } else if (builder.charAt(i) == ']'
                    && builder.charAt(i + 1) == 'h'
                    && builder.charAt(i + 2) == ']'
                    && builder.charAt(i + 3) == ']') {
                end = i;
                builder.setSpan(new BackgroundColorSpan(Palette.getColor(subreddit)), start, end,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                builder.delete(end, end + offset);
                builder.delete(start - offset, start);
                i -= offset + (end - start); // length of text
            }
        }
    }

    @Override
    public void onLinkClick(String url, int xOffset, String subreddit, URLSpan span) {
        if (url == null) {
            ((View) getParent()).callOnClick();
            return;
        }

        ContentType.Type type = ContentType.getContentType(url);
        Context context = getContext();
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextThemeWrapper) {
            activity =
                    (Activity) ((ContextThemeWrapper) context).getBaseContext();
        } else if (context instanceof ContextWrapper) {
            Context context1 = ((ContextWrapper) context).getBaseContext();
            if (context1 instanceof Activity) {
                activity = (Activity) context1;
            } else if (context1 instanceof ContextWrapper) {
                Context context2 = ((ContextWrapper) context1).getBaseContext();
                if (context2 instanceof Activity) {
                    activity = (Activity) context2;
                } else if (context2 instanceof ContextWrapper) {
                    activity =
                            (Activity) ((ContextThemeWrapper) context2).getBaseContext();
                }
            }
        } else {
            throw new RuntimeException("Could not find activity from context:" + context);
        }

        if (!PostMatch.openExternal(url) || type == ContentType.Type.VIDEO) {
            switch (type) {
                case DEVIANTART:
                case IMGUR:
                case XKCD:
                    if (SettingValues.image) {
                        Intent intent2 = new Intent(activity, MediaView.class);
                        intent2.putExtra(MediaView.EXTRA_URL, url);
                        intent2.putExtra(MediaView.SUBREDDIT, subreddit);
                        activity.startActivity(intent2);
                    } else {
                        LinkUtil.openExternally(url);
                    }
                    break;
                case REDDIT:
                    OpenRedditLink.openUrl(activity, url, true);
                    break;
                case LINK:
                    LogUtil.v("Opening link");
                    LinkUtil.openUrl(url, Palette.getColor(subreddit), activity);
                    break;
                case SELF:
                case NONE:
                    break;
                case STREAMABLE:
                    openStreamable(url, subreddit);
                    break;
                case ALBUM:
                    if (SettingValues.album) {
                        Intent i;
                        if (SettingValues.albumSwipe) {
                            i = new Intent(activity, AlbumPager.class);
                            i.putExtra(Album.EXTRA_URL, url);
                            i.putExtra(AlbumPager.SUBREDDIT, subreddit);
                        } else {
                            i = new Intent(activity, Album.class);
                            i.putExtra(Album.SUBREDDIT, subreddit);
                            i.putExtra(Album.EXTRA_URL, url);
                        }
                        activity.startActivity(i);
                    } else {
                        LinkUtil.openExternally(url);
                    }
                    break;
                case TUMBLR:
                    if (SettingValues.image) {
                        Intent i = new Intent(activity, TumblrPager.class);
                        i.putExtra(Album.EXTRA_URL, url);
                        activity.startActivity(i);
                    } else {
                        LinkUtil.openExternally(url);
                    }
                    break;
                case IMAGE:
                    openImage(url, subreddit);
                    break;
                case VREDDIT_REDIRECT:
                    openVReddit(url, subreddit, activity);
                    break;
                case GIF:
                case VREDDIT_DIRECT:
                    openGif(url, subreddit, activity);
                    break;
                case VIDEO:
                    if (!LinkUtil.tryOpenWithVideoPlugin(url)) {
                        LinkUtil.openUrl(url, Palette.getStatusBarColor(), activity);
                    }
                case SPOILER:
                    spoilerClicked = true;
                    setOrRemoveSpoilerSpans(xOffset, span);
                    break;
                case EXTERNAL:
                    LinkUtil.openExternally(url);
                    break;
            }
        } else {
            LinkUtil.openExternally(url);
        }
    }

    @Override
    public void onLinkLongClick(final String baseUrl, MotionEvent event) {
        if (baseUrl == null || SettingValues.noPreviewImageLongClick) {
            return;
        }
        final String url = StringEscapeUtils.unescapeHtml4(baseUrl);

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        Activity activity = null;
        final Context context = getContext();
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextThemeWrapper) {
            activity =
                    (Activity) ((ContextThemeWrapper) context).getBaseContext();
        } else if (context instanceof ContextWrapper) {
            Context context1 = ((ContextWrapper) context).getBaseContext();
            if (context1 instanceof Activity) {
                activity = (Activity) context1;
            } else if (context1 instanceof ContextWrapper) {
                Context context2 = ((ContextWrapper) context1).getBaseContext();
                if (context2 instanceof Activity) {
                    activity = (Activity) context2;
                } else if (context2 instanceof ContextWrapper) {
                    activity =
                            (Activity) ((ContextThemeWrapper) context2).getBaseContext();
                }
            }
        } else {
            throw new RuntimeException("Could not find activity from context:" + context);
        }

        if (activity != null && !activity.isFinishing()) {
            if (SettingValues.peek) {
                Peek.into(R.layout.peek_view, new SimpleOnPeek() {
                    @Override
                    public void onInflated(final PeekView peekView, final View rootView) {
                        //do stuff
                        TextView text = rootView.findViewById(R.id.title);
                        text.setText(url);
                        text.setTextColor(Color.WHITE);
                        ((PeekMediaView) rootView.findViewById(R.id.peek)).setUrl(url);

                        peekView.addButton((R.id.copy), new OnButtonUp() {
                            @Override
                            public void onButtonUp() {
                                ClipboardUtil.copyToClipboard(rootView.getContext(), "Link", url);
                                Toast.makeText(rootView.getContext(),
                                        R.string.submission_link_copied, Toast.LENGTH_SHORT).show();
                            }
                        });

                        peekView.setOnRemoveListener(new OnRemove() {
                            @Override
                            public void onRemove() {
                                ((PeekMediaView) rootView.findViewById(R.id.peek)).doClose();
                            }
                        });

                        peekView.addButton((R.id.share), new OnButtonUp() {
                            @Override
                            public void onButtonUp() {
                                Reddit.defaultShareText("", url, rootView.getContext());
                            }
                        });

                        peekView.addButton((R.id.pop), new OnButtonUp() {
                            @Override
                            public void onButtonUp() {
                                Reddit.defaultShareText("", url, rootView.getContext());
                            }
                        });

                        peekView.addButton((R.id.external), new OnButtonUp() {
                            @Override
                            public void onButtonUp() {
                                LinkUtil.openExternally(url);
                            }
                        });
                        peekView.setOnPop(new OnPop() {
                            @Override
                            public void onPop() {
                                onLinkClick(url, 0, "", null);
                            }
                        });
                    }
                })
                        .with(new PeekViewOptions().setFullScreenPeek(true))
                        .show((PeekViewActivity) activity, event);
            } else {
                BottomSheet.Builder b = new BottomSheet.Builder(activity).title(url).grid();
                int[] attrs = new int[]{R.attr.tintColor};
                TypedArray ta = getContext().obtainStyledAttributes(attrs);

                int color = ta.getColor(0, Color.WHITE);
                Drawable open = getResources().getDrawable(R.drawable.ic_open_in_new);
                Drawable share = getResources().getDrawable(R.drawable.ic_share);
                Drawable copy = getResources().getDrawable(R.drawable.ic_content_copy);
                final List<Drawable> drawableSet = Arrays.asList(open, share, copy);
                BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color);

                ta.recycle();

                b.sheet(R.id.open_link, open,
                        getResources().getString(R.string.open_externally));
                b.sheet(R.id.share_link, share, getResources().getString(R.string.share_link));
                b.sheet(R.id.copy_link, copy,
                        getResources().getString(R.string.submission_link_copy));
                final Activity finalActivity = activity;
                b.listener(new DialogInterface.OnClickListener() {
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
                }).show();
            }
        }
    }

    private void openVReddit(String url, String subreddit, Activity activity) {
        new OpenVRedditTask(activity, subreddit).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, url);
    }


    private void openGif(String url, String subreddit, Activity activity) {
        if (SettingValues.gif) {
            if(GifUtils.AsyncLoadGif.getVideoType(url).shouldLoadPreview()){
                LinkUtil.openUrl(url, Palette.getColor(subreddit), activity);
            } else {
                Intent myIntent = new Intent(getContext(), MediaView.class);
                myIntent.putExtra(MediaView.EXTRA_URL, url);
                myIntent.putExtra(MediaView.SUBREDDIT, subreddit);
                getContext().startActivity(myIntent);
            }
        } else {
            LinkUtil.openExternally(url);
        }
    }

    private void openStreamable(String url, String subreddit) {
        if (SettingValues.video) { //todo maybe streamable here?
            Intent myIntent = new Intent(getContext(), MediaView.class);

            myIntent.putExtra(MediaView.EXTRA_URL, url);
            myIntent.putExtra(MediaView.SUBREDDIT, subreddit);
            getContext().startActivity(myIntent);

        } else {
            LinkUtil.openExternally(url);
        }
    }

    private void openImage(String submission, String subreddit) {
        if (SettingValues.image) {
            Intent myIntent = new Intent(getContext(), MediaView.class);
            myIntent.putExtra(MediaView.EXTRA_URL, submission);
            myIntent.putExtra(MediaView.SUBREDDIT, subreddit);
            getContext().startActivity(myIntent);
        } else {
            LinkUtil.openExternally(submission);
        }

    }

    public void setOrRemoveSpoilerSpans(int endOfLink, URLSpan span) {
        if (span != null) {
            int offset = (span.getURL().contains("hidden")) ? -1 : 2;
            Spannable text = (Spannable) getText();
            // add 2 to end of link since there is a white space between the link text and the spoiler
            ForegroundColorSpan[] foregroundColors =
                    text.getSpans(endOfLink + offset, endOfLink + offset,
                            ForegroundColorSpan.class);

            if (foregroundColors.length > 1) {
                text.removeSpan(foregroundColors[1]);
            } else {
                for (int i = 1; i < storedSpoilerStarts.size(); i++) {
                    if (storedSpoilerStarts.get(i) < endOfLink + offset
                            && storedSpoilerEnds.get(i) > endOfLink + offset) {
                        try {
                            text.setSpan(storedSpoilerSpans.get(i), storedSpoilerStarts.get(i),
                                    storedSpoilerEnds.get(i) > text.toString().length() ?
                                            storedSpoilerEnds.get(i)
                                                    + offset : storedSpoilerEnds.get(i),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        } catch (Exception ignored) {
                            //catch out of bounds
                            ignored.printStackTrace();
                        }
                    }
                }
            }
            setText(text);
        }
    }

    /**
     * Set the necessary spans for each spoiler. <p/> The algorithm works in the same way as
     * <code>setCodeFont</code>.
     *
     * @param sequence
     * @return
     */
    private CharSequence setSpoilerStyle(SpannableStringBuilder sequence, String subreddit) {
        int start = 0;
        int end = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (sequence.charAt(i) == '[' && i < sequence.length() - 3) {
                if (sequence.charAt(i + 1) == '['
                        && sequence.charAt(i + 2) == 's'
                        && sequence.charAt(i + 3) == '[') {
                    start = i;
                }
            } else if (sequence.charAt(i) == ']' && i < sequence.length() - 3) {
                if (sequence.charAt(i + 1) == 's'
                        && sequence.charAt(i + 2) == ']'
                        && sequence.charAt(i + 3) == ']') {
                    end = i;
                }
            }

            if (end > start) {
                sequence.delete(end, end + 4);
                sequence.delete(start, start + 4);

                BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(
                        Palette.getDarkerColor(Palette.getColor(subreddit)));
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(
                        Palette.getDarkerColor(Palette.getColor(subreddit)));
                ForegroundColorSpan underneathColorSpan = new ForegroundColorSpan(Color.WHITE);

                URLSpan urlSpan = sequence.getSpans(start, start, URLSpan.class)[0];
                sequence.setSpan(urlSpan, sequence.getSpanStart(urlSpan), start - 1,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);


                sequence.setSpan(new URLSpanNoUnderline("#spoilerhidden"), start, end - 4,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                // spoiler text has a space at the front
                sequence.setSpan(backgroundColorSpan, start, end - 4,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sequence.setSpan(underneathColorSpan, start, end - 4,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sequence.setSpan(foregroundColorSpan, start, end - 4,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                storedSpoilerSpans.add(underneathColorSpan);
                storedSpoilerSpans.add(foregroundColorSpan);
                storedSpoilerSpans.add(backgroundColorSpan);
                // Shift 1 to account for remove of beginning "<"

                storedSpoilerStarts.add(start - 1);
                storedSpoilerStarts.add(start - 1);
                storedSpoilerStarts.add(start - 1);
                storedSpoilerEnds.add(end - 5);
                storedSpoilerEnds.add(end - 5);
                storedSpoilerEnds.add(end - 5);

                sequence.delete(start - 2, start - 1); // remove the trailing <
                start = 0;
                end = 0;
                i = i - 5; // move back to compensate for removal of [[s[
            }
        }

        return sequence;
    }

    private static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    /**
     * Sets the styling for string with code segments. <p/> The general process is to search for
     * <code>[[&lt;[</code> and <code>]&gt;]]</code> tokens to find the code fragments within the
     * escaped text. A <code>Spannable</code> is created which which breaks up the origin sequence
     * into non-code and code fragments, and applies a monospace font to the code fragments.
     *
     * @param sequence the Spannable generated from Html.fromHtml
     * @return the message with monospace font applied to code fragments
     */
    private SpannableStringBuilder setCodeFont(SpannableStringBuilder sequence) {
        int start = 0;
        int end = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (sequence.charAt(i) == '[' && i < sequence.length() - 3) {
                if (sequence.charAt(i + 1) == '['
                        && sequence.charAt(i + 2) == '<'
                        && sequence.charAt(i + 3) == '[') {
                    start = i;
                }
            } else if (sequence.charAt(i) == ']' && i < sequence.length() - 3) {
                if (sequence.charAt(i + 1) == '>'
                        && sequence.charAt(i + 2) == ']'
                        && sequence.charAt(i + 3) == ']') {
                    end = i;
                }
            }

            if (end > start) {
                sequence.delete(end, end + 4);
                sequence.delete(start, start + 4);
                sequence.setSpan(new TypefaceSpan("monospace"), start, end - 4,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                start = 0;
                end = 0;
                i = i - 4; // move back to compensate for removal of [[<[
            }
        }

        return sequence;
    }


}
