package me.ccrama.redditslide.Activities;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.text.StringEscapeUtils;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ccrama on 09/22/2020.
 */
public class GalleryImage implements Serializable {
    public String url;
    public int width;
    public int height;

    public String mediaId;
    public MediaMetadata metadata;

    public GalleryImage(JsonNode data) {
        if(data.has("u")) {
            url = StringEscapeUtils.unescapeHtml4(data.get("u").asText());
        } else if(data.has("mp4")) {
            url = StringEscapeUtils.unescapeHtml4(data.get("mp4").asText());
        }
        width = data.get("x").asInt();
        height = data.get("y").asInt();

        // Add metadata population
        metadata = new MediaMetadata();
        if(data.has("e")) {
            metadata.e = data.get("e").asText();
        }
        if(data.has("m")) {
            metadata.m = data.get("m").asText();
        }
        if(data.has("s")) {
            JsonNode s = data.get("s");
            metadata.source = new MediaMetadata.Source();
            if(s.has("mp4")) {
                metadata.source.mp4 = StringEscapeUtils.unescapeHtml4(s.get("mp4").asText());
            }
            if(s.has("gif")) {
                metadata.source.gif = StringEscapeUtils.unescapeHtml4(s.get("gif").asText());
            }
            if(s.has("y")) {
                metadata.source.y = s.get("y").asInt();
            }
            if(s.has("x")) {
                metadata.source.x = s.get("x").asInt();
            }
        }

        // Set animated based on type
        metadata.animated = "AnimatedImage".equals(metadata.e);
    }

    public boolean isAnimated() {
        if (metadata != null) {
            // Check metadata first
            if (metadata.animated) {
                return true;
            }
            if ("AnimatedImage".equals(metadata.e)) {
                return true;
            }
            if (metadata.m != null && metadata.m.contains("gif")) {
                return true;
            }
        }

        // Fallback to URL check if metadata is missing
        return url != null && (url.endsWith(".gif") || url.endsWith(".gifv") || url.endsWith(".mp4"));
    }

    public String getImageUrl() {
        // For animated content, use the direct MP4 URL from metadata
        if (isAnimated() && metadata != null && metadata.source != null) {
            if (metadata.source.mp4 != null && !metadata.source.mp4.isEmpty()) {
                // Return the complete MP4 URL directly
                return metadata.source.mp4;
            }
        }
        // For non-animated content or if no MP4 URL exists
        if (metadata != null && metadata.source != null && metadata.source.u != null) {
            // Use the direct URL from source if available
            return metadata.source.u;
        }
        // Fall back to original URL if nothing else works
        return url;
    }

    public static class MediaMetadata implements Serializable {
        private static final long serialVersionUID = 1L;

        public String e;  // type (e.g., "Image", "AnimatedImage")
        public String m;  // mimetype (e.g., "image/gif", "image/jpg")
        public String s;  // status
        public long id;   // media id
        public boolean animated;  // whether media is animated
        public String ext;  // file extension with dot (e.g., ".gif")

        public Preview[] p;  // array of preview images
        public Source source;  // source object containing URLs

        @Override
        public String toString() {
            return "MediaMetadata{" +
                "e='" + e + '\'' +
                ", m='" + m + '\'' +
                ", s='" + s + '\'' +
                ", id=" + id +
                ", animated=" + animated +
                ", ext='" + ext + '\'' +
                ", p=" + (p != null ? Arrays.toString(p) : "null") +
                ", source=" + source +
                '}';
        }

        public static class Preview implements Serializable {
            private static final long serialVersionUID = 1L;
            public int y;    // height
            public int x;    // width
            public String u; // preview URL
        }

        public static class Source implements Serializable {
            private static final long serialVersionUID = 1L;
            public int y;       // height
            public int x;       // width
            public String u;    // direct URL for non-animated
            public String gif;  // gif URL for animated
            public String mp4;  // mp4 URL for animated
        }
    }
}
