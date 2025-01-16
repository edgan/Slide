package me.edgan.redditslide.SwipeLayout.app;

import me.edgan.redditslide.SwipeLayout.SwipeBackLayout;

/**
 * @author Yrom
 */
public interface SwipeBackActivityBase {
    /**
     * @return the SwipeBackLayout associated with this activity.
     */
    SwipeBackLayout getSwipeBackLayout();

    void setSwipeBackEnable(boolean enable);

    /** Scroll out contentView and finish the activity */
    void scrollToFinishActivity();
}
