package me.edgan.redditslide.ImgurAlbum;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SecretConstants;
import me.edgan.redditslide.util.HttpUtil;
import me.edgan.redditslide.util.LogUtil;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Created by carlo_000 on 2/1/2016. */
public class AlbumUtils {

    public static SharedPreferences albumRequests;

    public static String substringAfterLastDash(String s) {
        if (s == null) {
            return null;
        }

        int lastDashIndex = s.lastIndexOf('-');

        // Only return the substring if a dash is found.
        if (lastDashIndex != -1) {
            return s.substring(lastDashIndex + 1);
        } else {
            return null;
        }
    }

    private static String getHash(String s) {
        String last = substringAfterLastDash(s);
        LogUtil.v("1 " + last);

        if (last != null) {
            return last;
        }

        if (s.contains("/comment/")) {
            s = s.substring(0, s.indexOf("/comment"));
        }

        String next = s.substring(s.lastIndexOf("/"));
        if (next.contains(".")) {
            next = next.substring(0, next.indexOf("."));
        }

        if (next.startsWith("/")) {
            next = next.substring(1);
        }

        LogUtil.v("2 " + next);
        if (next.length() < 5) {
            return getHash(s.replace(next, ""));
        } else {
            return next;
        }
    }

    private static String cutEnds(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }

    public static class GetAlbumWithCallback
            extends AsyncTask<String, Void, ArrayList<JsonElement>> {

        public String hash;
        public String type;

        public Activity baseActivity;

        private OkHttpClient client;
        private Gson gson;
        private String imgurKey;

        public void onError() {}

        public GetAlbumWithCallback(@NonNull String url, @NonNull Activity baseActivity) {

            this.baseActivity = baseActivity;
            if (url.contains("/layout/")) {
                url = url.substring(0, url.indexOf("/layout"));
            }
            String rawDat = cutEnds(url);

            if (rawDat.endsWith("/")) {
                rawDat = rawDat.substring(0, rawDat.length() - 1);
            }

            if (rawDat.substring(rawDat.lastIndexOf("/") + 1).length() < 4) {
                rawDat = rawDat.replace(rawDat.substring(rawDat.lastIndexOf("/")), "");
            }
            if (rawDat.contains("?")) {
                rawDat = rawDat.substring(0, rawDat.indexOf("?"));
            }

            hash = getHash(rawDat);
            String apiType = substringAfterLastDash(url);

            if (apiType == null) {
                type = "image";
            } else {
                type = "album";
            }

            client = Reddit.client;
            gson = new Gson();
            imgurKey = SecretConstants.getImgurApiKey(baseActivity);
        }

        public void doWithData(List<Image> data) {
            if (data == null || data.isEmpty()) {
                onError();
            }
        }

        public void doWithDataSingle(final SingleImage data) {
            doWithData(
                    new ArrayList<Image>() {
                        {
                            this.add(convertToSingle(data));
                        }
                    });
        }

        public Image convertToSingle(SingleImage data) {
            try {
                final Image toDo = new Image();
                toDo.setAnimated(data.getAnimated() || data.getLink().contains(".gif"));
                if (data.getAdditionalProperties().containsKey("mp4")
                        && !data.getAdditionalProperties().get("mp4").equals("")) {
                    toDo.setHash(getHash(data.getAdditionalProperties().get("mp4").toString()));
                } else {
                    toDo.setHash(getHash(data.getLink()));
                }
                toDo.setTitle(data.getTitle());
                toDo.setExt(data.getLink().substring(data.getLink().lastIndexOf(".")));
                toDo.setHeight(data.getHeight());
                toDo.setWidth(data.getWidth());
                return toDo;
            } catch (Exception e) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    String dataJson = data != null ? objectMapper.writeValueAsString(data) : "null";
                    LogUtil.e(e, "convertToSingle error, data [" + dataJson + "]");
                } catch (JsonProcessingException ex) {
                    LogUtil.e(ex, "Error serializing data to JSON for logging");
                }
                onError();
                return null;
            }
        }

        JsonElement[] target;
        int count;
        int done;

        AlbumImage album;

        public void parseJson(JsonElement baseData) {
            try {
                if (!baseData.toString().contains("\"data\":[]")) {
                    album = new ObjectMapper().readValue(baseData.toString(), AlbumImage.class);
                    baseActivity.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    doWithData(album.getData().getImages());
                                }
                            });
                } else {
                    String apiUrl = "https://api.imgur.com/3/" + type + "/" + hash;
                    LogUtil.v("3 " + type);
                    LogUtil.v(apiUrl);
                    JsonObject result = HttpUtil.getImgurJsonObject(client, gson, apiUrl, imgurKey);
                    try {
                        if (result == null) {
                            onError();
                            return;
                        }
                        final SingleImage single =
                                new ObjectMapper()
                                        .readValue(result.toString(), SingleAlbumImage.class)
                                        .getData();
                        if (single.getLink() != null) {
                            baseActivity.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            doWithDataSingle(single);
                                        }
                                    });
                        } else {
                            onError();
                        }
                    } catch (Exception e) {
                        LogUtil.e(e, "Error " + apiUrl);
                    }
                }
            } catch (IOException e) {
                LogUtil.e(e, "parseJson error, baseData [" + baseData + "]");
            }
        }

        @Override
        protected ArrayList<JsonElement> doInBackground(final String... sub) {
            if (hash.startsWith("/")) {
                hash = hash.substring(1);
            }
            if (hash.contains(",")) {
                target = new JsonElement[hash.split(",").length];
                count = 0;
                done = 0;
                for (String s : hash.split(",")) {
                    final int pos = count;
                    count++;
                    String apiUrl = "https://api.imgur.com/3/" + type + "/" + s;
                    LogUtil.v("4 " + type);
                    LogUtil.v(apiUrl);
                    JsonObject result = HttpUtil.getImgurJsonObject(client, gson, apiUrl, imgurKey);
                    target[pos] = result;
                    done += 1;
                    if (done == target.length) {
                        final ArrayList<Image> jsons = new ArrayList<>();
                        for (JsonElement el : target) {
                            if (el != null) {
                                try {
                                    SingleImage single =
                                            new ObjectMapper()
                                                    .readValue(
                                                            el.toString(), SingleAlbumImage.class)
                                                    .getData();
                                    LogUtil.v(el.toString());
                                    jsons.add(convertToSingle(single));
                                } catch (IOException e) {
                                    LogUtil.e(e, "Error " + apiUrl);
                                }
                            }
                        }
                        if (jsons.isEmpty()) {
                            onError();
                        } else {
                            baseActivity.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            doWithData(jsons);
                                        }
                                    });
                        }
                    }
                }
            } else {
                if (baseActivity != null) {
                    final String apiUrl = getUrl(hash);
                    if (albumRequests.contains(apiUrl)
                            && JsonParser.parseString(albumRequests.getString(apiUrl, ""))
                                    .getAsJsonObject()
                                    .has("data")) {
                        parseJson(
                                JsonParser.parseString(albumRequests.getString(apiUrl, ""))
                                        .getAsJsonObject());
                    } else {
                        LogUtil.v(apiUrl);
                        // This call requires no imgur headers, don't pass in the headers Map
                        final JsonObject result = HttpUtil.getJsonObject(client, gson, apiUrl);
                        if (result != null && result.has("data")) {
                            albumRequests.edit().putString(apiUrl, result.toString()).apply();
                            parseJson(result);
                        } else {
                            onError();
                        }
                    }
                    return null;
                }
            }
            return null;
        }
    }

    public static String getUrl(String hash) {
        return "http://imgur.com/ajaxalbums/getimages/" + hash + "/hit.json?all=true";
    }
}
