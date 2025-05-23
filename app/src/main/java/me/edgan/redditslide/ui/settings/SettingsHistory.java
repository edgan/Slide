package me.edgan.redditslide.ui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

public class SettingsHistory extends BaseActivityAnim {

    private SettingsHistoryFragment fragment = new SettingsHistoryFragment(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_history);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        setupAppBar(R.id.toolbar, R.string.settings_title_history, true, true);

        ((ViewGroup) findViewById(R.id.settings_history))
                .addView(
                        getLayoutInflater()
                                .inflate(R.layout.activity_settings_history_child, null));

        fragment.Bind();
    }
}
