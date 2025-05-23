package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

/** Created by l3d00m on 11/13/2015. */
public class SettingsReddit extends BaseActivityAnim {

    SettingsRedditFragment fragment = new SettingsRedditFragment(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_reddit);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_reddit_prefs, true, true);

        ((ViewGroup) findViewById(R.id.settings_reddit))
                .addView(
                        getLayoutInflater().inflate(R.layout.activity_settings_reddit_child, null));

        fragment.Bind();
    }
}
