package me.edgan.redditslide.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.util.MiscUtil;

/** Created by ccrama on 3/5/2015. */
public class SettingsTheme extends BaseActivityAnim implements RestartActivity {

    private SettingsThemeFragment fragment = new SettingsThemeFragment(this);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_theme);

        MiscUtil.setupOldSwipeModeBackground(this, getWindow().getDecorView());

        setupAppBar(R.id.toolbar, R.string.title_edit_theme, true, true);

        ((ViewGroup) findViewById(R.id.settings_theme))
                .addView(getLayoutInflater().inflate(R.layout.activity_settings_theme_child, null));

        fragment.Bind();
    }

    @Override
    public void restartActivity() {
        Intent i = new Intent(this, SettingsTheme.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        overridePendingTransition(0, 0);

        finish();
        overridePendingTransition(0, 0);
    }
}
