package me.edgan.redditslide.Tumblr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.edgan.redditslide.Constants;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.util.HttpUtil;
import me.edgan.redditslide.util.LogUtil;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Created by carlo_000 on 2/1/2016. */
public class TumblrUtils {

    public static SharedPreferences tumblrRequests;

    public static class GetTumblrPostWithCallback
            extends AsyncTask<String, Void, ArrayList<JsonElement>> {

        public String blog, id;
        public Activity baseActivity;

        private OkHttpClient client;
        private Gson gson;

        public void onError() {}

        public GetTumblrPostWithCallback(@NonNull String url, @NonNull Activity baseActivity) {

            this.baseActivity = baseActivity;
            Uri i = Uri.parse(url);

            id = i.getPathSegments().get(1);
            blog = i.getHost().split("\\.")[0];

            client = Reddit.client;
            gson = new Gson();
        }

        public void doWithData(List<Photo> data) {
            if (data == null || data.isEmpty()) {
                onError();
            }
        }

        TumblrPost post;

        public void parseJson(JsonElement baseData) {
            try {
                post = new ObjectMapper().readValue(baseData.toString(), TumblrPost.class);
                baseActivity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                doWithData(post.getResponse().getPosts().get(0).getPhotos());
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
                LogUtil.e(e, "parseJson error, baseData [" + baseData + "]");
            }
        }

        @Override
        protected ArrayList<JsonElement> doInBackground(final String... sub) {
            if (baseActivity != null) {
                String apiUrl =
                        "https://api.tumblr.com/v2/blog/"
                                + blog
                                + "/posts?api_key="
                                + Constants.TUMBLR_API_KEY
                                + "&id="
                                + id;
                LogUtil.v(apiUrl);
                if (tumblrRequests.contains(apiUrl)
                        && JsonParser.parseString(tumblrRequests.getString(apiUrl, ""))
                                .getAsJsonObject()
                                .has("response")) {
                    parseJson(
                            JsonParser.parseString(tumblrRequests.getString(apiUrl, ""))
                                    .getAsJsonObject());
                } else {
                    LogUtil.v(apiUrl);
                    final JsonObject result = HttpUtil.getJsonObject(client, gson, apiUrl);
                    if (result != null
                            && result.has("response")
                            && result.get("response").getAsJsonObject().has("posts")
                            && result.get("response")
                                    .getAsJsonObject()
                                    .get("posts")
                                    .getAsJsonArray()
                                    .get(0)
                                    .getAsJsonObject()
                                    .has("photos")) {
                        tumblrRequests.edit().putString(apiUrl, result.toString()).apply();
                        parseJson(result);
                    } else {
                        onError();
                    }
                }
                return null;
            }
            return null;
        }
    }
}
