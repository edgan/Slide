package me.edgan.redditslide.Adapters;

import net.dean.jraw.models.CommentNode;

/** Created by carlo_000 on 10/27/2015. */
public class CommentUrlObject {
    public String url;
    String subredditName;

    public CommentNode comment;

    public CommentUrlObject(CommentNode comment, String url, String subredditName) {
        this.comment = comment;
        this.subredditName = subredditName;
        this.url = url;
    }

    public String getSubredditName() {
        return subredditName;
    }

    public String getUrl() {
        return url;
    }
}
