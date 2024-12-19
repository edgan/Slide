package me.ccrama.redditslide.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.style.DynamicDrawableSpan;
import android.view.View;
import android.util.Log;
import android.graphics.Rect;

import me.ccrama.redditslide.SettingValues;

public class AnimatedImageSpan extends DynamicDrawableSpan {
    private final GifDrawable drawable;
    private final View view;
    private final Handler handler;
    private boolean isAttached = false;
    private final Runnable invalidateRunnable;
    private static final int FRAME_DELAY = 16;  // Approximately 60fps

    public AnimatedImageSpan(GifDrawable drawable, View view) {
        super(ALIGN_BASELINE); // Keep ALIGN_BASELINE but we'll handle positioning manually
        this.drawable = drawable;
        this.view = view;
        this.handler = new Handler(Looper.getMainLooper());

        invalidateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAttached) {
                    view.postInvalidate();
                    handler.postDelayed(this, FRAME_DELAY);
                }
            }
        };
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                    float x, int top, int y, int bottom, Paint paint) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            Log.e("EmoteDebug", "Drawable is null in span draw");
            return;
        }

        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int drawableHeight = drawable.getBounds().height();

        // Calculate the space needed above the baseline
        int transY = y - drawableHeight + (fm.descent - fm.ascent) / 4;

        canvas.save();
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public Drawable getDrawable() {
        drawable.invalidateSelf();
        return drawable;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            Paint.FontMetricsInt originalFm = new Paint.FontMetricsInt();
            paint.getFontMetricsInt(originalFm);

            int drawableHeight = rect.height();
            // We want the drawable to extend both above and below the text
            // First, get the normal text height
            int textHeight = originalFm.descent - originalFm.ascent;

            // If drawable is taller than text, we need to expand the line height
            if (drawableHeight > textHeight) {
                int heightDiff = drawableHeight - textHeight;
                // Distribute the extra height equally above and below
                int extraSpace = drawableHeight;

                fm.top = originalFm.top - extraSpace;
                fm.ascent = originalFm.ascent - extraSpace;
                fm.descent = originalFm.descent + extraSpace;
                fm.bottom = originalFm.bottom + extraSpace;
            } else {
                // If drawable is shorter, preserve text metrics
                fm.top = originalFm.top;
                fm.ascent = originalFm.ascent;
                fm.descent = originalFm.descent;
                fm.bottom = originalFm.bottom;
            }
        }

        return rect.right;
    }

    public void start() {
        drawable.start();
        if (!isAttached) {
            isAttached = true;
            handler.post(invalidateRunnable);
        }
    }

    public void stop() {
        drawable.stop();
        if (isAttached) {
            isAttached = false;
            handler.removeCallbacks(invalidateRunnable);
        }
    }

    public GifDrawable getGifDrawable() {
        return drawable;
    }

    public void onAttached() {
        Log.d("EmoteDebug", "Span attached to view");
        if (!isAttached) {
            isAttached = true;
            handler.post(invalidateRunnable);
            if (SettingValues.commentEmoteAnimation) {
                drawable.start();
            }
        }
    }

    public void onDetached() {
        Log.d("EmoteDebug", "Span detached from view");
        if (isAttached) {
            isAttached = false;
            handler.removeCallbacks(invalidateRunnable);
            drawable.stop();
        }
    }
}
