package me.edgan.redditslide.ForceTouch.callback;

import android.view.View;

import me.edgan.redditslide.ForceTouch.PeekView;

/** Provides callbacks for the lifecycle events of the PeekView */
public interface OnPeek {

    void onInflated(PeekView rootView, View contentView);

    void shown();

    void dismissed();
}
