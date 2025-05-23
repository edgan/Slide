package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

/** Created by ccrama on 3/5/2015. */
public class SettingsData extends BaseActivityAnim {

    private SettingsDataFragment fragment = new SettingsDataFragment(this);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_datasaving);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_data, true, true);

        ((ViewGroup) findViewById(R.id.settings_datasaving))
                .addView(
                        getLayoutInflater()
                                .inflate(R.layout.activity_settings_datasaving_child, null));

        fragment.Bind();
    }
}
