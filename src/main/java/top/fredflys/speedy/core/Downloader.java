package top.fredflys.speedy.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import top.fredflys.speedy.constant.Constants;

public class Downloader {
    
    static HttpURLConnection connect(String url) throws IOException   {
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.addRequestProperty("User-Agent", Constants.USER_AGENT);
        return connection;
    }
    
    static void download(String url) {
        
    }
}
