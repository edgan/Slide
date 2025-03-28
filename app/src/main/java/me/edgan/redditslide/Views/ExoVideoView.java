package me.edgan.redditslide.Views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.VideoSize;

import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.GifUtils;
import me.edgan.redditslide.util.NetworkUtil;

/**
 * ExoVideoView that uses a TextureView (with a cached SurfaceTexture) so that when recycled
 * (e.g., when scrolling in the gallery) the video decoder's surface is reused and the video does
 * not go blank.
 */
public class ExoVideoView extends RelativeLayout {
    private static final String TAG = "ExoVideoView";

    private Context context;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerControlView playerUI;
    private boolean muteAttached = false;
    private boolean hqAttached = false;
    private AudioFocusHelper audioFocusHelper;
    private Handler handler = new Handler(Looper.getMainLooper());


    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private AspectRatioFrameLayout videoFrame;

    // Variables for panning
    private float lastTouchX;
    private float lastTouchY;
    private float positionX = 0f;
    private float positionY = 0f;
    private boolean isDragging = false;

    // Static variable to hold the saved SurfaceTexture.
    private static SurfaceTexture sSavedSurfaceTexture;
    // The TextureView used for video playback.
    private TextureView videoTextureView;

    public interface OnPlaybackStateChangedListener {
        void onPlaybackStateChanged(boolean isPlaying);
    }

    private OnPlaybackStateChangedListener playbackStateChangedListener;

    public void setOnPlaybackStateChangedListener(OnPlaybackStateChangedListener listener) {
        this.playbackStateChangedListener = listener;
    }

    public ExoVideoView(final Context context) {
        this(context, null, true);
    }

    public ExoVideoView(final Context context, final boolean ui) {
        this(context, null, ui);
    }

    public ExoVideoView(final Context context, final AttributeSet attrs) {
        this(context, attrs, true);
    }

    public ExoVideoView(final Context context, final AttributeSet attrs, final boolean ui) {
        super(context, attrs);
        this.context = context;
        setupPlayer();
        if (ui && !isVerticalMode()) {
            setupUI();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // If the player was released (player is null), reinitialize it.
        if (player == null) {
            Log.d(TAG, "Player is null on attach; reinitializing player.");
            setupPlayer();
            if (playerUI == null && !isVerticalMode()) {
                setupUI();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // Pause playback when view is detached
        stop();

        super.onDetachedFromWindow();
    }

    /** Initializes the player and sets up a TextureView that reuses its SurfaceTexture. */
    private void setupPlayer() {
        // Create a track selector with bitrate settings.
        trackSelector = new DefaultTrackSelector(context);
        if ((SettingValues.lowResAlways
                || (NetworkUtil.isConnected(context)
                    && !NetworkUtil.isConnectedWifi(context)
                    && SettingValues.lowResMobile))
            && SettingValues.lqVideos) {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setForceLowestBitrate(true));
        } else {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setForceHighestSupportedBitrate(true));
        }

        // Release any existing player.
        if (player != null) {
            player.release();
            player = null;
        }

        // Create the player.
        player = new SimpleExoPlayer.Builder(context).setTrackSelector(trackSelector).build();

        // Create an AspectRatioFrameLayout to size the video correctly.
        AspectRatioFrameLayout frame = new AspectRatioFrameLayout(context);

        this.videoFrame = frame;
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.addRule(CENTER_IN_PARENT, TRUE);
        frame.setLayoutParams(params);
        frame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        // Initialize scale gesture detector
        scaleGestureDetector = new ScaleGestureDetector(context, new VideoScaleListener());

        // Add a Player.Listener for aspect ratio changes, logging, etc.
        player.addListener(
            new Player.Listener() {
                // Make the video use the correct aspect ratio
                @Override
                public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                    Log.d(TAG, "onVideoSizeChanged: width=" + videoSize.width + ", height=" + videoSize.height + ", unappliedRotationDegrees=" + videoSize.unappliedRotationDegrees);
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        // Calculate the correct aspect ratio
                        float aspectRatio = (float) videoSize.width / videoSize.height;

                        // Apply any needed rotation
                        if (videoSize.unappliedRotationDegrees == 90 ||
                            videoSize.unappliedRotationDegrees == 270) {
                            aspectRatio = 1.0f / aspectRatio;
                        }

                        // Set the aspect ratio
                        Log.d(TAG, "Setting aspect ratio to: " + aspectRatio);
                        frame.setAspectRatio(aspectRatio);
                    }
                }

                // Logging
                @Override
                public void onTracksChanged(@NonNull Tracks tracks) {
                    StringBuilder toLog = new StringBuilder();
                    for (int groupIndex = 0; groupIndex < tracks.getGroups().size(); groupIndex++) {
                        Tracks.Group group = tracks.getGroups().get(groupIndex);
                        for (int trackIndex = 0; trackIndex < group.getMediaTrackGroup().length; trackIndex++) {
                            Format format = group.getTrackFormat(trackIndex);
                            boolean isSelected = group.isTrackSelected(trackIndex);

                            toLog.append("Format:\t")
                                    .append(format)
                                    .append(isSelected ? " (selected)" : "")
                                    .append("\n");
                        }
                    }
                }
            });

        // --- Use a TextureView with a cached SurfaceTexture ---
        videoTextureView = new TextureView(context);
        videoTextureView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        videoTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: surface=" + surface +
                        " (" + System.identityHashCode(surface) + "), width=" + width + ", height=" + height);
                if (sSavedSurfaceTexture != null) {
                    Log.d(TAG, "Reattaching saved SurfaceTexture: " + System.identityHashCode(sSavedSurfaceTexture));
                    videoTextureView.setSurfaceTexture(sSavedSurfaceTexture);
                } else {
                    sSavedSurfaceTexture = surface;
                    Log.d(TAG, "Saving new SurfaceTexture: " + System.identityHashCode(surface));
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged: surface=" + surface +
                        " (" + System.identityHashCode(surface) + "), width=" + width + ", height=" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed: surface=" + surface +
                        " (" + System.identityHashCode(surface) + ")");
                // Return false to indicate that we are managing the SurfaceTexture lifecycle.
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // Optionally log frame updates.
                // Log.d(TAG, "onSurfaceTextureUpdated: surface=" + surface);
            }
        });

        frame.addView(videoTextureView);
        player.setVideoTextureView(videoTextureView);

        addView(frame);

        // Configure player options.
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setVolume(SettingValues.unmuteDefault ? 1f : 0f);
        SettingValues.isMuted = !SettingValues.unmuteDefault;

        // Create audio focus helper.
        audioFocusHelper = new AudioFocusHelper(
                ContextCompat.getSystemService(context, AudioManager.class)
        );
    }

    private void setupUI() {
        playerUI = new PlayerControlView(context);
        playerUI.setPlayer(player);
        playerUI.setShowTimeoutMs(2000);  // Controls will hide after 2 seconds

        // Add the player UI with proper positioning constraints
        RelativeLayout.LayoutParams playerUIParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        playerUIParams.addRule(ALIGN_PARENT_BOTTOM, TRUE);
        playerUIParams.bottomMargin = (int) (64 * context.getResources().getDisplayMetrics().density);
        addView(playerUI);

        playerUI.startAnimation(new PlayerUIFadeInAnimation(playerUI, true, 0));
        setOnClickListener((v) -> {
            playerUI.clearAnimation();
            if (playerUI.isVisible()) {
                playerUI.startAnimation(new PlayerUIFadeInAnimation(playerUI, false, 300));
            } else {
                playerUI.startAnimation(new PlayerUIFadeInAnimation(playerUI, true, 300));
            }
        });
    }

    /**
     * Sets the player's URI and prepares for playback.
     */
    public void setVideoURI(Uri uri, VideoType type, Player.Listener listener) {
        Log.d(TAG, "setVideoURI() called with uri: " + uri.toString());
        DataSource.Factory downloader =
                new OkHttpDataSource.Factory(Reddit.client)
                        .setDefaultRequestProperties(GifUtils.AsyncLoadGif.makeHeaderMap(uri.getHost()));
        DataSource.Factory cacheDataSourceFactory =
                new CacheDataSource.Factory()
                        .setCache(Reddit.videoCache)
                        .setUpstreamDataSourceFactory(downloader);

        MediaSource videoSource;
        switch (type) {
            case DASH:
                Log.d(TAG, "Creating DASH media source");
                videoSource =
                        new DashMediaSource.Factory(cacheDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(uri));
                break;
            case STANDARD:
            default:
                Log.d(TAG, "Creating standard media source");
                videoSource =
                        new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(uri));
                break;
        }

        player.setMediaSource(videoSource);
        player.prepare();
        if (listener != null) {
            player.addListener(listener);
        }
    }

    /** Starts video playback. */
    public void play() {
        player.play();
        if (playbackStateChangedListener != null) {
            playbackStateChangedListener.onPlaybackStateChanged(true);
        }
    }

    /** Pauses video playback. */
    public void pause() {
        if (player != null) {
            player.pause();
            if (playbackStateChangedListener != null) {
                playbackStateChangedListener.onPlaybackStateChanged(false);
            }
        }
    }

    /** Stops video playback and releases the player. */
    public void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        audioFocusHelper.loseFocus();
    }

    /** Seeks to a specified position (in milliseconds). */
    public void seekTo(long time) {
        Log.d(TAG, "seekTo() called with time: " + time);
        if (player != null) {
            player.seekTo(time);
        }
    }

    /** Returns the current playback position (in milliseconds). */
    public long getCurrentPosition() {
        long pos = player != null ? player.getCurrentPosition() : 0;
        Log.d(TAG, "getCurrentPosition() called, returning: " + pos);
        return pos;
    }

    /** Returns whether the player is currently playing. */
    public boolean isPlaying() {
        boolean playing = player != null &&
                player.getPlaybackState() == Player.STATE_READY &&
                player.getPlayWhenReady();
        Log.d(TAG, "isPlaying() called, returning: " + playing);
        return playing;
    }

    /**
     * Attaches a mute button to this view.
     */
    public void attachMuteButton(final ImageView mute) {
        Log.d(TAG, "attachMuteButton() called");
        mute.setVisibility(GONE);
        player.addListener(new Player.Listener() {
            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                Log.d(TAG, "attachMuteButton onTracksChanged");
                if (muteAttached && !tracks.getGroups().isEmpty()) {
                    return;
                } else {
                    muteAttached = true;
                }
                boolean foundAudio = false;
                for (Tracks.Group group : tracks.getGroups()) {
                    for (int trackIndex = 0; trackIndex < group.getMediaTrackGroup().length; trackIndex++) {
                        if (group.isTrackSelected(trackIndex)) {
                            Format format = group.getTrackFormat(trackIndex);
                            if (format != null && MimeTypes.isAudio(format.sampleMimeType)) {
                                foundAudio = true;
                                break;
                            }
                        }
                    }
                    if (foundAudio) {
                        break;
                    }
                }
                if (foundAudio) {
                    mute.setVisibility(VISIBLE);
                    if (!SettingValues.isMuted) {
                        player.setVolume(1f);
                        mute.setImageResource(R.drawable.ic_volume_on);
                        BlendModeUtil.tintImageViewAsSrcAtop(mute, Color.WHITE);
                        audioFocusHelper.gainFocus();
                    } else {
                        player.setVolume(0f);
                        mute.setImageResource(R.drawable.ic_volume_off);
                        BlendModeUtil.tintImageViewAsSrcAtop(mute, getResources().getColor(R.color.md_red_500));
                    }
                    mute.setOnClickListener((v) -> {
                        if (SettingValues.isMuted) {
                            Log.d(TAG, "Mute button clicked: unmuting");
                            player.setVolume(1f);
                            SettingValues.isMuted = false;
                            SettingValues.prefs.edit().putBoolean(SettingValues.PREF_MUTE, false).apply();
                            mute.setImageResource(R.drawable.ic_volume_on);
                            BlendModeUtil.tintImageViewAsSrcAtop(mute, Color.WHITE);
                            audioFocusHelper.gainFocus();
                        } else {
                            Log.d(TAG, "Mute button clicked: muting");
                            player.setVolume(0f);
                            SettingValues.isMuted = true;
                            SettingValues.prefs.edit().putBoolean(SettingValues.PREF_MUTE, true).apply();
                            mute.setImageResource(R.drawable.ic_volume_off);
                            BlendModeUtil.tintImageViewAsSrcAtop(mute, getResources().getColor(R.color.md_red_500));
                            audioFocusHelper.loseFocus();
                        }
                    });
                }
            }
        });
    }

    /**
     * Attaches an HQ (high quality) button to this view.
     */
    public void attachHqButton(final ImageView hq) {
        Log.d(TAG, "attachHqButton() called");
        hq.setVisibility(GONE);
        player.addListener(new Player.Listener() {
            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                Log.d(TAG, "attachHqButton onTracksChanged");
                if (hqAttached || tracks.getGroups().isEmpty() ||
                        trackSelector.getParameters().forceHighestSupportedBitrate) {
                    return;
                } else {
                    hqAttached = true;
                }
                int videoTrackCounter = 0;
                for (Tracks.Group group : tracks.getGroups()) {
                    for (int trackIndex = 0; trackIndex < group.getMediaTrackGroup().length; trackIndex++) {
                        Format format = group.getTrackFormat(trackIndex);
                        if (format != null && MimeTypes.isVideo(format.sampleMimeType)) {
                            videoTrackCounter++;
                            if (videoTrackCounter > 1) {
                                break;
                            }
                        }
                    }
                    if (videoTrackCounter > 1) {
                        break;
                    }
                }
                if (videoTrackCounter > 1) {
                    hq.setVisibility(VISIBLE);
                    hq.setOnClickListener((v) -> {
                        Log.d(TAG, "HQ button clicked: forcing high bitrate");
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setForceLowestBitrate(false)
                                        .setForceHighestSupportedBitrate(true));
                        hq.setVisibility(GONE);
                    });
                }
            }
        });
    }

    /** Enum for video types. */
    public enum VideoType {
        STANDARD,
        DASH
    }

    /** Helper class to manage audio focus. */
    private class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
        private AudioManager manager;
        private boolean wasPlaying;
        private AudioFocusRequestCompat request;

        AudioFocusHelper(AudioManager manager) {
            this.manager = manager;
            if (request == null) {
                AudioAttributesCompat audioAttributes =
                        new AudioAttributesCompat.Builder()
                                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
                                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                                .build();

                AudioFocusRequestCompat.Builder builder = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(this);

                if (SettingValues.pauseOnAudioFocus) {
                    builder.setWillPauseWhenDucked(true);
                }

                request = builder.build();

            }
        }

        void loseFocus() {
            Log.d(TAG, "AudioFocusHelper: losing focus");
            AudioManagerCompat.abandonAudioFocusRequest(manager, request);
        }

        void gainFocus() {
            Log.d(TAG, "AudioFocusHelper: gaining focus");
            AudioManagerCompat.requestAudioFocus(manager, request);
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "AudioFocusHelper: onAudioFocusChange: " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                wasPlaying = player.getPlayWhenReady();
                player.pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                player.setPlayWhenReady(wasPlaying);
            }
        }
    }

    /** Simple animation for fading in/out the player UI. */
    static class PlayerUIFadeInAnimation extends AnimationSet {
        private PlayerControlView animationView;
        private boolean toVisible;

        PlayerUIFadeInAnimation(PlayerControlView view, boolean toVisible, long duration) {
            super(false);
            this.toVisible = toVisible;
            this.animationView = view;
            float startAlpha = toVisible ? 0 : 1;
            float endAlpha = toVisible ? 1 : 0;
            AlphaAnimation alphaAnimation = new AlphaAnimation(startAlpha, endAlpha);
            alphaAnimation.setDuration(duration);
            addAnimation(alphaAnimation);
            setAnimationListener(new PlayerUIFadeInAnimationListener());
        }

        private class PlayerUIFadeInAnimationListener implements Animation.AnimationListener {
            @Override
            public void onAnimationStart(Animation animation) {
                animationView.show();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (toVisible) {
                    animationView.show();
                } else {
                    animationView.hide();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Purposefully left blank
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Handle scale gestures
        scaleGestureDetector.onTouchEvent(event);

        // Only handle panning when zoomed in
        if (scaleFactor > 1.0f) {
            final int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    // Start tracking touch position for potential dragging
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    isDragging = false;
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    // Only process if not in a scaling operation
                    if (!scaleGestureDetector.isInProgress()) {
                        // Calculate distance moved
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;

                        // If movement is significant enough, consider it a drag
                        if (!isDragging && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            // Update position with constraints to keep video partially visible
                            positionX += dx;
                            positionY += dy;

                            // Calculate maximum allowed movement based on scale
                            float maxDeltaX = (videoFrame.getWidth() * (scaleFactor - 1)) / 2;
                            float maxDeltaY = (videoFrame.getHeight() * (scaleFactor - 1)) / 2;

                            // Constrain movement
                            positionX = Math.max(-maxDeltaX, Math.min(maxDeltaX, positionX));
                            positionY = Math.max(-maxDeltaY, Math.min(maxDeltaY, positionY));

                            // Apply translation
                            videoFrame.setTranslationX(positionX);
                            videoFrame.setTranslationY(positionY);
                        }

                        // Update last position
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                    }
                    break;
                }

                case MotionEvent.ACTION_UP: {
                    // Handle click if it wasn't a drag
                    if (!isDragging) {
                        return super.onTouchEvent(event);
                    }
                    isDragging = false;
                    break;
                }

                case MotionEvent.ACTION_CANCEL: {
                    isDragging = false;
                    break;
                }
            }

            // If we're handling a drag, intercept the event
            if (isDragging) {
                return true;
            }
        }

        // Continue with normal touch handling if not scaling or panning
        if (!scaleGestureDetector.isInProgress() && !isDragging) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    /**
     * Scale gesture listener to handle pinch-to-zoom events
     */
    private class VideoScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Limit the scale factor to reasonable bounds
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));

            // Apply the scale to the video frame
            if (videoFrame != null) {
                videoFrame.setScaleX(scaleFactor);
                videoFrame.setScaleY(scaleFactor);
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // Change resize mode when scaling begins to allow proper zooming
            if (videoFrame != null) {
                videoFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // If scale is back to normal (or very close), reset to FIT mode and reset position
            if (scaleFactor <= 1.05f) {
                scaleFactor = 1.0f;
                resetPosition();
                if (videoFrame != null) {
                    videoFrame.setScaleX(1.0f);
                    videoFrame.setScaleY(1.0f);
                    videoFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                }
            }
        }
    }

    /**
     * Resets any applied zoom to default scale and position
     */
    public void resetZoom() {
        scaleFactor = 1.0f;
        resetPosition();
        if (videoFrame != null) {
            videoFrame.setScaleX(1.0f);
            videoFrame.setScaleY(1.0f);
            videoFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }
    }

    /**
     * Resets the panning position to center
     */
    private void resetPosition() {
        positionX = 0f;
        positionY = 0f;
        if (videoFrame != null) {
            videoFrame.setTranslationX(0f);
            videoFrame.setTranslationY(0f);
        }
    }

    private boolean isVerticalMode() {
        // Get the context's activity
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                String activityName = context.getClass().getSimpleName();
                return activityName.equals("Album") || activityName.equals("RedditGallery");
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return false;
    }
}
