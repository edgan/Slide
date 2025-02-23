package me.edgan.redditslide;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Created by Deadl on 26/11/2015. */
public class SecretConstants {
    private static String apiKey;
    private static String apiImgurKey;
    private static String googleLongClientID;
    private static String googleShortClientID;
    private static String redGifClientId;
    private static String redGifClientSecret;

    private static String base64EncodedPublicKey;

    public static String getBase64EncodedPublicKey(Context context) {
        if (base64EncodedPublicKey == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                base64EncodedPublicKey = properties.getProperty("base64EncodedPublicKey");
            } catch (IOException e) {
                // file not found
                base64EncodedPublicKey = "";
            }
        }
        return base64EncodedPublicKey;
    }

    public static String getApiKey(Context context) {
        if (apiKey == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                apiKey = properties.getProperty("apiKey");
            } catch (IOException e) {
                // file not found
                apiKey = "";
            }
        }
        return apiKey;
    }

    public static String getGoogleLongClientID(Context context) {
        if (googleLongClientID == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                googleLongClientID = properties.getProperty("googleLongClientID");
            } catch (IOException e) {
                // file not found
                googleLongClientID = "";
            }
        }
        return googleLongClientID;
    }

    public static String getGoogleShortClientID(Context context) {
        if (googleShortClientID == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                googleShortClientID = properties.getProperty("googleShortClientID");
            } catch (IOException e) {
                // file not found
                googleShortClientID = "";
            }
        }
        return googleShortClientID;
    }

    public static String getImgurApiKey(Context context) {
        if (apiImgurKey == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                apiImgurKey = properties.getProperty("imgur");
            } catch (IOException e) {
                // file not found
                apiImgurKey = "3P3GlZj91emshgWU6YuQL98Q9Zihp1c2vCSjsnOQLIchXPzDLh"; // Testing key, will not work in production
            }
        }
        return apiImgurKey;
    }

    public static String getRedGifsClientId(Context context) {
        if (redGifClientId == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                redGifClientId = properties.getProperty("redGifClientId");
            } catch (IOException e) {
                // file not found
                redGifClientId =
                        "93013a4b39f-2031-b319-3021-c4ea3ba7dc12"; // Example id, will
                                                                              // not work in
                                                                              // production
            }
        }
        return redGifClientId;
    }

    public static String getRedGifsClientSecret(Context context) {
        if (redGifClientSecret == null) {
            InputStream input;
            try {
                input = context.getAssets().open("secretconstants.properties");
                Properties properties = new Properties();
                properties.load(input);
                redGifClientSecret = properties.getProperty("redGifClientSecret");
            } catch (IOException e) {
                // file not found
                redGifClientSecret =
                        "OcGrW2TjlEa3N349Vs+gQSKu5vTBx19jC/gDXzIFOe4="; // Example secret, will
                                                                              // not work in
                                                                              // production
            }
        }
        return redGifClientSecret;
    }

}
