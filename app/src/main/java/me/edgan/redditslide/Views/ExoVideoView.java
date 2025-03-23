package me.edgan.redditslide.Views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
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

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoSize;

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

    // Static variable to hold the saved SurfaceTexture.
    private static SurfaceTexture sSavedSurfaceTexture;
    // The TextureView used for video playback.
    private TextureView videoTextureView;

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
        Log.d(TAG, "Constructor called");
        setupPlayer();
        if (ui) {
            setupUI();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow() called");
        // If the player was released (player is null), reinitialize it.
        if (player == null) {
            Log.d(TAG, "Player is null on attach; reinitializing player.");
            setupPlayer();
            if (playerUI == null) {
                setupUI();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow() called");
        // Pause playback when view is detached
        pause();

        super.onDetachedFromWindow();
    }

    /** Initializes the player and sets up a TextureView that reuses its SurfaceTexture. */
    private void setupPlayer() {
        Log.d(TAG, "setupPlayer() called");
        // Create a track selector with bitrate settings.
        trackSelector = new DefaultTrackSelector(context);
        if ((SettingValues.lowResAlways
                || (NetworkUtil.isConnected(context)
                    && !NetworkUtil.isConnectedWifi(context)
                    && SettingValues.lowResMobile))
            && SettingValues.lqVideos) {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setForceLowestBitrate(true));
            Log.d(TAG, "Forcing lowest bitrate");
        } else {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setForceHighestSupportedBitrate(true));
            Log.d(TAG, "Forcing highest supported bitrate");
        }

        // Release any existing player.
        if (player != null) {
            Log.d(TAG, "Releasing existing player");
            player.release();
            player = null;
        }

        // Create the player.
        player = new SimpleExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build();
        Log.d(TAG, "Player created");

        // Create an AspectRatioFrameLayout to size the video correctly.
        AspectRatioFrameLayout frame = new AspectRatioFrameLayout(context);
        LayoutParams frameParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        frameParams.addRule(CENTER_IN_PARENT, TRUE);
        frame.setLayoutParams(frameParams);
        frame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        // Add listener to handle video size changes
        player.addListener(new Player.Listener() {
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
        Log.d(TAG, "TextureView added and set as player's output");
        // -------------------------------------------------------

        addView(frame);
        Log.d(TAG, "Frame added to ExoVideoView");

        // Configure player options.
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setVolume(SettingValues.unmuteDefault ? 1f : 0f);
        SettingValues.isMuted = !SettingValues.unmuteDefault;
        Log.d(TAG, "Player configured: repeatMode and volume set");

        // Create audio focus helper.
        audioFocusHelper = new AudioFocusHelper(
                ContextCompat.getSystemService(context, AudioManager.class)
        );
    }

    /** Sets up the player UI (controls) as before. */
    private void setupUI() {
        Log.d(TAG, "setupUI() called");
        playerUI = new PlayerControlView(context);
        playerUI.setPlayer(player);
        playerUI.setShowTimeoutMs(2000);
        if (!SettingValues.oldSwipeMode) {
            playerUI.hide();
        }
        addView(playerUI);
        Log.d(TAG, "PlayerControlView added");

        setOnClickListener((v) -> {
            Log.d(TAG, "ExoVideoView clicked");
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
        Log.d(TAG, "Media source set and player prepared");
    }

    /** Starts video playback. */
    public void play() {
        Log.d(TAG, "play() called");
        player.play();
    }

    /** Pauses video playback. */
    public void pause() {
        if (player != null) {
            player.pause();
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
                request = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(this)
                        .setWillPauseWhenDucked(true)
                        .build();
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

    /**
     * Reset the player to prevent flickering when recycled
     */
    public void resetPlayer() {
        Log.d(TAG, "resetPlayer() called");
        // First pause any ongoing playback
        pause();

        // Clear any existing frame
        if (videoTextureView != null) {
            videoTextureView.setAlpha(0f);  // Hide temporarily to prevent flicker
            // After a small delay, make visible again
            handler.postDelayed(() -> videoTextureView.setAlpha(1f), 50);
        }
    }
}
