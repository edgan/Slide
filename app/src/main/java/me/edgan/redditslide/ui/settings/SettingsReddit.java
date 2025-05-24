package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.util.MiscUtil;

/** Created by l3d00m on 11/13/2015. */
public class SettingsReddit extends BaseActivityAnim {

    SettingsRedditFragment fragment = new SettingsRedditFragment(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_reddit);

        MiscUtil.setupOldSwipeModeBackground(this, getWindow().getDecorView());

        setupAppBar(R.id.toolbar, R.string.settings_reddit_prefs, true, true);

        ((ViewGroup) findViewById(R.id.settings_reddit))
                .addView(
                        getLayoutInflater().inflate(R.layout.activity_settings_reddit_child, null));

        fragment.Bind();
    }
}
