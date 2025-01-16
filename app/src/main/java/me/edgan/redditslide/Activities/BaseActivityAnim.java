package me.edgan.redditslide.Activities;

import android.os.Bundle;

import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SwipeLayout.app.SwipeBackActivityBase;

/**
 * Used as the base if an enter or exit animation is required (if the user can swipe out of the
 * activity)
 */
public class BaseActivityAnim extends BaseActivity implements SwipeBackActivityBase {
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Reddit.peek) {
            overridePendingTransition(R.anim.pop_in, 0);
        } else {
            overridePendingTransition(R.anim.slide_in, 0);
        }
    }
}
