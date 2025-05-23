package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

/** Created by l3d00m on 11/13/2015. */
public class SettingsFont extends BaseActivityAnim {

    private SettingsFontFragment fragment = new SettingsFontFragment(this);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_font);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_title_font, true, true);

        ((ViewGroup) findViewById(R.id.settings_font))
                .addView(getLayoutInflater().inflate(R.layout.activity_settings_font_child, null));

        fragment.Bind();
    }
}
