package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.URLUtils;
import top.fredflys.speedy.util.Utils;

public class SIngleThreadDownloader implements Downloader{
    public ScheduledExecutorService statsScheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void download(String url, Path outputPath) {
        HttpURLConnection connection = URLUtils.connect(url);

        long resourceSize = URLUtils.getResourceSize(connection);
        long localFileSize = Utils.getFileSize(outputPath.toString());

        if (localFileSize >= resourceSize) {
            Log.info("The target url has been downloaded to %s.", outputPath.toString());
            System.exit(0);
        }

        Analyzer analyzer = new Analyzer(resourceSize, localFileSize);
        statsScheduler.scheduleAtFixedRate(analyzer, 1, 1, TimeUnit.SECONDS);

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
                analyzer.addDownloadedInBytes(len);
            }
        } catch (FileNotFoundException e) {
            Log.error("File not found: %s", e.toString());
        } catch (Exception e) {
            System.err.println("Error: " + e);
            Log.error(e.toString());
        } finally {
            if (connection != null) connection.disconnect();
            statsScheduler.shutdownNow();
            Utils.printOnSameLineWithRightPadding(
                String.format("Download finished. File is saved to %s.", outputPath.toString()), 
                analyzer.getPrevStatsLength()
            );
        }
    }
    
}
