package me.edgan.redditslide.util;

import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

/** Created by TacoTheDank on 04/22/2021. */
public class CompatUtil {
    public static Spanned fromHtml(@NonNull String source) {
        if (source == null) {
            return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY);
        }
        return HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }
}
