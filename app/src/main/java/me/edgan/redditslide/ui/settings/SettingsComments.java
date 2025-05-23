package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

public class SettingsComments extends BaseActivityAnim {

    private SettingsCommentsFragment fragment = new SettingsCommentsFragment(this);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_comments);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_title_comments, true, true);

        ((ViewGroup) findViewById(R.id.settings_comments))
                .addView(
                        getLayoutInflater()
                                .inflate(R.layout.activity_settings_comments_child, null));

        fragment.Bind();
    }
}
