package me.ccrama.redditslide.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;

public class GifDrawable extends Drawable {
    private final Movie movie;
    private long startTime = 0;
    private int alpha = 255; // Default alpha
    private ColorFilter colorFilter;
    private final Paint paint;

    public GifDrawable(Movie movie, Drawable.Callback callback) {
        this.movie = movie;
        setCallback(callback);
        this.paint = new Paint();
    }

    @Override
    public void draw(Canvas canvas) {
        if (movie == null) return;

        long now = SystemClock.uptimeMillis();

        if (startTime == 0) { // first time
            startTime = now;
        }

        int relTime = (int) ((now - startTime) % movie.duration());
        movie.setTime(relTime);

        // Apply alpha and color filter to Paint
        paint.setAlpha(alpha);
        paint.setColorFilter(colorFilter);

        // Draw the movie with the paint
        movie.draw(canvas, getBounds().left, getBounds().top, paint);

        // Schedule a redraw to animate the GIF
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * Starts the GIF animation.
     */
    public void start() {
        startTime = 0; // Reset to start
        invalidateSelf();
    }

    /**
     * Stops the GIF animation.
     */
    public void stop() {
        // Optionally implement if you need to stop the animation
        // Currently, the animation runs continuously based on draw() method
    }
}