package me.ccrama.redditslide.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import org.apache.commons.text.StringEscapeUtils;

import me.ccrama.redditslide.Activities.GalleryImage;

/**
 * Created by TacoTheDank on 04/04/2021.
 */
public class JsonUtil {
    public static void getGalleryData(final JsonNode data, final ArrayList<GalleryImage> urls) {
        for (JsonNode identifier : data.get("gallery_data").get("items")) {
            String mediaId = identifier.get("media_id").asText();
            if (data.has("media_metadata") && data.get("media_metadata").has(mediaId)) {
                JsonNode mediaNode = data.get("media_metadata").get(mediaId);
                GalleryImage image = new GalleryImage(mediaNode.get("s"));

                // Set metadata
                image.mediaId = mediaId;
                image.metadata = new GalleryImage.MediaMetadata();
                image.metadata.e = mediaNode.get("e").asText();
                image.metadata.m = mediaNode.get("m").asText();
                image.metadata.animated = "AnimatedImage".equals(image.metadata.e);

                // Set source data
                image.metadata.source = new GalleryImage.MediaMetadata.Source();
                JsonNode s = mediaNode.get("s");
                if (s.has("mp4")) {
                    image.metadata.source.mp4 = StringEscapeUtils.unescapeHtml4(s.get("mp4").asText());
                }
                if (s.has("gif")) {
                    image.metadata.source.gif = StringEscapeUtils.unescapeHtml4(s.get("gif").asText());
                }
                if (s.has("u")) {
                    image.metadata.source.u = StringEscapeUtils.unescapeHtml4(s.get("u").asText());
                }

                urls.add(image);
            }
        }
    }
}
