package me.edgan.redditslide;

import android.content.SharedPreferences;

import net.dean.jraw.models.Submission;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

/** Created by carlo_000 on 1/13/2016. */
public class PostMatch {
    /**
     * Checks if a string is totally or partially contained in a set of strings
     *
     * @param target string to check
     * @param strings set of strings to check in
     * @param totalMatch only allow total match, no partial matches
     * @return if the string is contained in the set of strings
     */
    public static boolean contains(String target, Set<String> strings, boolean totalMatch) {
        // filters are always stored lowercase
        if (totalMatch) {
            return strings.contains(target.toLowerCase(Locale.ENGLISH).trim());
        } else if (strings.contains(target.toLowerCase(Locale.ENGLISH).trim())) {
            return true;
        } else {
            for (String s : strings) {
                if (target.toLowerCase(Locale.ENGLISH).trim().contains(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if a domain should be filtered or not: returns true if the target domain ends with the
     * comparison domain and if supplied, target path begins with the comparison path
     *
     * @param target URL to check
     * @param strings The URLs to check against
     * @return If the target is covered by any strings
     * @throws MalformedURLException
     */
    public static boolean isDomain(String target, Set<String> strings)
            throws MalformedURLException {
        URL domain = new URL(target);
        for (String s : strings) {
            if (!s.contains("/")) {
                if (ContentType.hostContains(domain.getHost(), s)) {
                    return true;
                } else {
                    continue;
                }
            }

            if (!s.contains("://")) {
                s = "http://" + s;
            }

            try {
                URL comparison = new URL(s.toLowerCase(Locale.ENGLISH));

                if (ContentType.hostContains(domain.getHost(), comparison.getHost())
                        && domain.getPath().startsWith(comparison.getPath())) {
                    return true;
                }
            } catch (MalformedURLException ignored) {
            }
        }
        return false;
    }

    public static boolean openExternal(String url) {
        try {
            return isDomain(url.toLowerCase(Locale.ENGLISH), SettingValues.alwaysExternal);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static SharedPreferences filters;

    public static boolean doesMatch(Submission s, String baseSubreddit, boolean ignore18) {
        if (Hidden.id.contains(s.getFullName()))
            return true; // if it's hidden we're not going to show it regardless

        String title = s.getTitle();
        String body = s.getSelftext();
        String domain = s.getUrl();
        String subreddit = s.getSubredditName();
        String flair =
                s.getSubmissionFlair().getText() != null ? s.getSubmissionFlair().getText() : "";

        if (contains(title, SettingValues.titleFilters, false)) return true;

        if (contains(body, SettingValues.textFilters, false)) return true;

        if (contains(s.getAuthor(), SettingValues.userFilters, false)) return true;

        try {
            if (isDomain(domain.toLowerCase(Locale.ENGLISH), SettingValues.domainFilters))
                return true;
        } catch (MalformedURLException ignored) {
        }

        if (!subreddit.equalsIgnoreCase(baseSubreddit)
                && contains(subreddit, SettingValues.subredditFilters, true)) {
            return true;
        }

        boolean contentMatch = false;

        if (baseSubreddit == null || baseSubreddit.isEmpty()) {
            baseSubreddit = "frontpage";
        }

        baseSubreddit = baseSubreddit.toLowerCase(Locale.ENGLISH);
        boolean albums = isAlbums(baseSubreddit);
        boolean gallery = isGallery(baseSubreddit);
        boolean gifs = isGif(baseSubreddit);
        boolean images = isImage(baseSubreddit);
        boolean links = isLinks(baseSubreddit);
        boolean nsfwGallery = isNsfwGallery(baseSubreddit);
        boolean nsfwGifs = isNsfwGif(baseSubreddit);
        boolean nsfwImages = isNsfwImage(baseSubreddit);
        boolean nsfwLinks = isNsfwLink(baseSubreddit);
        boolean nsfwSelftext = isNsfwSelftext(baseSubreddit);
        boolean selftext = isSelftext(baseSubreddit);
        boolean videos = isVideo(baseSubreddit);

        ContentType.Type contentType = ContentType.getContentType(s);

        // Handle NSFW content types
        if (s.isNsfw() && SettingValues.showNSFWContent) {
            if (ignore18) {
                contentMatch = false;
            } else {
            switch (contentType) {
                case GIF:
                    if (nsfwGifs) {
                        contentMatch = true;
                    }
                    break;
                case IMAGE:
                    if (nsfwImages) {
                        contentMatch = true;
                    }
                    break;
                case LINK:
                    if (nsfwLinks) {
                        contentMatch = true;
                    }
                    break;
                case REDDIT_GALLERY:
                    if (nsfwGallery) {
                        contentMatch = true;
                    }
                    break;
                case SELF:
                    if (nsfwSelftext) {
                        contentMatch = true;
                    }
                    break;
            }
        }
        } else {
            // Handle regular content types
            switch (contentType) {
                case ALBUM:
                    if (albums) {
                        contentMatch = true;
                    }
                    break;
                case DEVIANTART:
                case GIF:
                    if (gifs) {
                        contentMatch = true;
                    }
                    break;
                case IMAGE:
                    if (images) {
                        contentMatch = true;
                    }
                    break;
                case IMGUR:
                    if (images) {
                        contentMatch = true;
                    }
                    break;
                case REDDIT:
                case EMBEDDED:
                case LINK:
                    if (links) {
                        contentMatch = true;
                    }
                    break;
                case SELF:
                    if (selftext) {
                        contentMatch = true;
                    }
                    break;
                case NONE:
                    if (selftext) {
                        contentMatch = true;
                    }
                    break;
                case REDDIT_GALLERY:
                    if (gallery) {
                        contentMatch = true;
                    }
                    break;
                case XKCD:
                    if (images) {
                        contentMatch = true;
                    }
                    break;
                case VREDDIT_REDIRECT:
                case STREAMABLE:
                case VIDEO:
                    if (videos) {
                        contentMatch = true;
                    }
                    break;
            }
        }

        if (!flair.isEmpty())
            for (String flairText : SettingValues.flairFilters) {
                if (flairText.toLowerCase(Locale.ENGLISH).startsWith(baseSubreddit)) {
                    String[] split = flairText.split(":");
                    if (split[0].equalsIgnoreCase(baseSubreddit)) {
                        if (flair.equalsIgnoreCase(split[1].trim())) {
                            contentMatch = true;
                            break;
                        }
                    }
                }
            }

        return contentMatch;
    }

    public static boolean doesMatch(Submission s) {
        String title = s.getTitle();
        String body = s.getSelftext();
        String domain = s.getUrl();
        String subreddit = s.getSubredditName();

        boolean domainc = false;

        boolean titlec = contains(title, SettingValues.titleFilters, false);

        boolean bodyc = contains(body, SettingValues.textFilters, false);

        try {
            domainc = isDomain(domain.toLowerCase(Locale.ENGLISH), SettingValues.domainFilters);
        } catch (MalformedURLException ignored) {
        }

        boolean subredditc =
                subreddit != null
                        && !subreddit.isEmpty()
                        && contains(subreddit, SettingValues.subredditFilters, true);

        return (titlec || bodyc || domainc || subredditc);
    }

    public static void setChosen(boolean[] values, String subreddit) {
        subreddit = subreddit.toLowerCase(Locale.ENGLISH);
        SharedPreferences.Editor e = filters.edit();
        e.putBoolean(subreddit + "_albumsFilter", values[0]);
        e.putBoolean(subreddit + "_galleryFilter", values[1]);
        e.putBoolean(subreddit + "_gifsFilter", values[2]);
        e.putBoolean(subreddit + "_imagesFilter", values[3]);
        e.putBoolean(subreddit + "_linksFilter", values[4]);
        e.putBoolean(subreddit + "_selftextFilter", values[5]);
        e.putBoolean(subreddit + "_videoFilter", values[6]);
        if (values.length > 7 && SettingValues.showNSFWContent) {
            e.putBoolean(subreddit + "_nsfwGalleryFilter", values[7]);
            e.putBoolean(subreddit + "_nsfwGifsFilter", values[8]);
            e.putBoolean(subreddit + "_nsfwImagesFilter", values[9]);
            e.putBoolean(subreddit + "_nsfwLinkFilter", values[10]);
            e.putBoolean(subreddit + "_nsfwSelftextFilter", values[11]);
        }
        e.apply();
    }

    public static boolean isAlbums(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_albumsFilter", false);
    }

    public static boolean isGallery(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_galleryFilter", false);
    }

    public static boolean isGif(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_gifsFilter", false);
    }

    public static boolean isImage(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_imagesFilter", false);
    }

    public static boolean isLinks(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_linksFilter", false);
    }

    public static boolean isNsfwGallery(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_nsfwGalleryFilter", false);
    }

    public static boolean isNsfwGif(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_nsfwGifsFilter", false);
    }

    public static boolean isNsfwImage(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_nsfwImagesFilter", false);
    }

    public static boolean isNsfwLink(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_nsfwLinkFilter", false);
    }

    public static boolean isNsfwSelftext(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_nsfwSelftextFilter", false);
    }

    public static boolean isSelftext(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_selftextFilter", false);
    }

    public static boolean isVideo(String baseSubreddit) {
        return filters.getBoolean(baseSubreddit + "_videoFilter", false);
    }
}
