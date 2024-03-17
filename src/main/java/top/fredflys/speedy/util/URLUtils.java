package top.fredflys.speedy.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.MalformedInputException;

import top.fredflys.speedy.constant.Constants;

public class URLUtils {
    public static long getResourceSize(HttpURLConnection connection) {
        long resourceSize = connection.getContentLengthLong();

        if (resourceSize <= 0) {
            Log.error("The url is invalid at %s. Aborted.", connection.getURL());
        }
        return resourceSize;
    }

    public static long getResourceSize(String url) {
        HttpURLConnection connection = connect(url);
        return connection.getContentLengthLong();
    }

    public static HttpURLConnection connect(String url){
        HttpURLConnection connection = null;
        try {
            URL target = new URL(url);
            connection = (HttpURLConnection) target.openConnection();
            connection.addRequestProperty("User-Agent", Constants.USER_AGENT);
        } catch (MalformedInputException e) {
            Log.error("Invalid url: at %s", url);
        } catch (IOException e) {
            Log.error("Failed when trying to connect to %s.", url);
        }
        return connection;
    }

    public static HttpURLConnection retrieve(String url, long start, long end) {
        HttpURLConnection connection = connect(url);
        String range = String.format("bytes=%d-%s", start, end == 0 ? "" : end);
        // Log.info("Range: %s", range);
        connection.setRequestProperty("RANGE", range);
        return connection;
    }
}
