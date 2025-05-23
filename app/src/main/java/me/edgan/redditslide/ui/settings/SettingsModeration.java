package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

public class SettingsModeration extends BaseActivityAnim {
    private SettingsModerationFragment fragment = new SettingsModerationFragment(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_moderation);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_moderation, true, true);

        ((ViewGroup) findViewById(R.id.settings_moderation))
                .addView(
                        getLayoutInflater()
                                .inflate(R.layout.activity_settings_moderation_child, null));

        fragment.Bind();
    }
}
