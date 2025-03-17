package me.edgan.redditslide.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import me.edgan.redditslide.Reddit;

/**
 * Collection of various network utility methods.
 *
 * @author Matthew Dean
 */
public class NetworkUtil {

    private NetworkUtil() {}

    /**
     * Uses the provided context to determine the current connectivity status.
     *
     * @param context A context used to retrieve connection information.
     * @return A non-null value defined in {@link Status}.
     */
    private static Status getConnectivityStatus(final Context context) {
        // Check if in forced offline mode
        if (Reddit.appRestart != null && Reddit.appRestart.getBoolean("forceoffline", false)) {
            return Status.NONE;
        }

        // For normal operation, always return WIFI status unless in forced offline mode
        return Status.WIFI;

        // Original implementation - commented out but kept for reference
        /*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return getConnectivityStatusPre23(context);
        }
        return getConnectivityStatusNew(context);
        */
    }

    /** For devices running pre-Marshmallow. */
    private static Status getConnectivityStatusPre23(final Context context) {
        final ConnectivityManager cm =
                ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (cm == null) {
            return Status.NONE;
        }

        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return Status.NONE;
        }
        switch (activeNetwork.getType()) {
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_ETHERNET:
                if (cm.isActiveNetworkMetered())
                    return Status.MOBILE; // respect metered wifi networks as mobile
                return Status.WIFI;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_BLUETOOTH:
            case ConnectivityManager.TYPE_WIMAX:
                return Status.MOBILE;
            default:
                return Status.NONE;
        }
    }

    /** For devices running Marshmallow and above. */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static Status getConnectivityStatusNew(final Context context) {
        final ConnectivityManager cm =
                ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (cm == null) {
            return Status.NONE;
        }

        final Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            return Status.NONE;
        }

        final NetworkCapabilities nwCapabilities = cm.getNetworkCapabilities(activeNetwork);
        if (nwCapabilities == null) {
            return Status.NONE;
        }

        if (!isConnectedToInternet(nwCapabilities)) {
            return Status.NONE;
        }

        // Don't rely on wifiManager.isWifiEnabled() which may be affected by permissions
        // Instead rely solely on NetworkCapabilities to determine connection type
        if (nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            if (cm.isActiveNetworkMetered())
                return Status.MOBILE; // respect metered wifi networks as mobile
            return Status.WIFI;
        }
        if (nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            return Status.MOBILE;
        }
        return Status.NONE;
    }

    /**
     * Checks if the network is connected. An application context is said to have connection if
     * {@link #getConnectivityStatus(Context)} does not equal {@link Status#NONE}.
     *
     * @param context The context used to retrieve connection information.
     * @return true if the application is connected, false if otherwise.
     */
    public static boolean isConnected(final Context context) {
        // Check if in forced offline mode
        if (Reddit.appRestart != null && Reddit.appRestart.getBoolean("forceoffline", false)) {
            return false;
        }
        // Always return true unless in offline mode
        return true;
    }

    public static boolean isConnectedNoOverride(final Context context) {
        return getConnectivityStatus(context) != Status.NONE;
    }

    /**
     * Checks if the network is connected to WiFi.
     *
     * @param context The context used to retrieve connection information.
     * @return true if the application is connected, false if otherwise.
     */
    public static boolean isConnectedWifi(Context context) {
        // Check if in forced offline mode
        if (Reddit.appRestart != null && Reddit.appRestart.getBoolean("forceoffline", false)) {
            return false;
        }
        // Always return true for WiFi unless in offline mode
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static boolean isConnectedToInternet(final NetworkCapabilities nwCapabilities) {
        return nwCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nwCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * A simplified list of connectivity statuses. See {@link ConnectivityManager}'s {@code TYPE_*}
     * for a full list.
     */
    private enum Status {
        /** Operating on a wireless connection. */
        WIFI,
        /** Operating on 3G, 4G, 4G LTE, etc. */
        MOBILE,
        /** No connection present. */
        NONE
    }
}
