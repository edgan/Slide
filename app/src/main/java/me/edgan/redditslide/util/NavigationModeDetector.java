package me.edgan.redditslide.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;

public class NavigationModeDetector {

    public static final int NAVIGATION_MODE_GESTURE = 2;
    public static final int NAVIGATION_MODE_THREE_BUTTON = 0;
    public static final int NAVIGATION_MODE_UNKNOWN = -1;

    /**
     * Detects the navigation mode on the device.
     *
     * @param context The application context.
     * @param view The root view (used for fallback detection with WindowInsets).
     * @return An integer indicating the navigation mode: - NAVIGATION_MODE_GESTURE (2) for gesture
     *     navigation - NAVIGATION_MODE_THREE_BUTTON (0) for 3-button navigation -
     *     NAVIGATION_MODE_UNKNOWN (-1) if the mode cannot be determined
     */
    public static int getNavigationMode(Context context, View view) {
        // First, try to detect navigation mode from system settings (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                String navigationModeSetting =
                        Settings.Secure.getString(context.getContentResolver(), "navigation_mode");

                if ("2".equals(navigationModeSetting)) {
                    return NAVIGATION_MODE_GESTURE;
                } else if ("0".equals(navigationModeSetting)) {
                    return NAVIGATION_MODE_THREE_BUTTON;
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Fall back to indirect method if system settings detection fails
            }

            // Fallback: Use WindowInsets to determine gesture navigation
            if (view != null) {
                WindowInsets insets = view.getRootWindowInsets();
                if (insets != null) {
                    int navBarHeight = insets.getStableInsetBottom();
                    return (navBarHeight == 0)
                            ? NAVIGATION_MODE_GESTURE
                            : NAVIGATION_MODE_THREE_BUTTON;
                }
            }
        }

        // Default to unknown navigation mode
        return NAVIGATION_MODE_UNKNOWN;
    }
}
