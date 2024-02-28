package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;

public class Downloader {
    
    private HttpURLConnection connect(String url) throws IOException   {
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.addRequestProperty("User-Agent", Constants.USER_AGENT);
        return connection;
    }

    public void download(String url, Path outputPath) {
        HttpURLConnection connection = null;
        try {
           connection = connect(url);
        } catch (IOException e) {
            System.err.println("Error: " + e);
        }

        try (
            BufferedInputStream bufferIn = new BufferedInputStream(
                connection.getInputStream()
            );
            BufferedOutputStream bufferOut = new BufferedOutputStream(
                new FileOutputStream(outputPath.toString())
            );
        ) {
            int len = -1;
            while ((len = bufferIn.read()) != -1) {
                bufferOut.write(len);
            }
        } catch (FileNotFoundException e) {
            Log.error("File not found: %s", e.toString());
        } catch (Exception e) {
            System.err.println("Error: " + e);
            Log.error(e.toString());
        }
    }
}
