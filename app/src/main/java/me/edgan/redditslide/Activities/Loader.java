package me.edgan.redditslide.Activities;

/** Created by carlo_000 on 1/20/2016. */
import android.os.Bundle;
import android.util.TypedValue;

import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;

/** Created by ccrama on 9/17/2015. */
public class Loader extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        disableSwipeBackLayout();
        super.onCreate(savedInstance);
        applyColorTheme();
        setContentView(R.layout.activity_loading);

        if (SettingValues.oldSwipeMode) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.card_background, typedValue, true);
            getWindow().getDecorView().setBackgroundColor(typedValue.data);
        }

        MainActivity.loader = this;
    }
}
