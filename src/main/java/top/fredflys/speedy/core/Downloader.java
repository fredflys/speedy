package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.Utils;

public class Downloader {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private HttpURLConnection connect(String url) throws IOException   {
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.addRequestProperty("User-Agent", Constants.USER_AGENT);
        return connection;
    }

    public void download(String url, Path outputPath) {
        HttpURLConnection connection = null;
        long resourceSize = 0;
        try {
           connection = connect(url);
           resourceSize = connection.getContentLengthLong();

           if (resourceSize <= 0) {
               Log.error("The url is invalid at %s. Aborted.", url);
           }
        } catch (IOException e) {
            Log.error(e.toString());
        }

        long localFileSize = Utils.getFileSize(outputPath.toString());
        if (localFileSize == -1) {
            Log.error("%s is not a valid path for the output file.", outputPath.toString());
        }
        if (localFileSize >= resourceSize) {
            Log.info("The target url has been downloaded to %s.", outputPath.toString());
            System.exit(0);
        }

        Analyzer analyzer = new Analyzer(resourceSize, localFileSize);
        scheduler.scheduleAtFixedRate(analyzer, 1, 1, TimeUnit.SECONDS);

        try (
            BufferedInputStream bufferIn = new BufferedInputStream(
                connection.getInputStream()
            );
            BufferedOutputStream bufferOut = new BufferedOutputStream(
                new FileOutputStream(outputPath.toString())
            );
        ) {
            int len = -1;
            byte[] buffer = new byte[Constants.DOWNLOAD_BUFFER_SIZE];
            while ((len = bufferIn.read(buffer)) != -1) {
                bufferOut.write(buffer, 0, len);
                analyzer.setDownloadedInBytes(analyzer.getDownloadedInBytes() + len);
            }
        } catch (FileNotFoundException e) {
            Log.error("File not found: %s", e.toString());
        } catch (Exception e) {
            System.err.println("Error: " + e);
            Log.error(e.toString());
        } finally {
            if (connection != null) connection.disconnect();
            scheduler.shutdownNow();
            Utils.printOnSameLineWithRightPadding(
                String.format("Download finished. File is saved to %s.", outputPath.toString()), 
                analyzer.getPrevStatsLength()
            );
        }
    }
}
