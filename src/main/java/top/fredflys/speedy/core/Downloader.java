package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.Utils;

public class Downloader {
    public  int workers = Constants.WORKERS;

    public ScheduledExecutorService statsScheduler = Executors.newScheduledThreadPool(1);
    public ThreadPoolExecutor downloadThreadsPool = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(workers));

    public HttpURLConnection retrieve(String url, long start, long end) {
        HttpURLConnection connection = connect(url);
        String range = String.format("bytes=%d-%s", start, end == 0 ? "" : end);
        // Log.info("Range: %s", range);
        connection.setRequestProperty("RANGE", range);
        return connection;
    }

    public HttpURLConnection connect(String url){
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

    public long getResourceSize(HttpURLConnection connection) {
        long resourceSize = connection.getContentLengthLong();

        if (resourceSize <= 0) {
            Log.error("The url is invalid at %s. Aborted.", connection.getURL());
        }
        return resourceSize;
    }

    public long getResourceSize(String url) {
        HttpURLConnection connection = connect(url);
        return connection.getContentLengthLong();
    }

    public void download(String url, Path outputPath) {
        HttpURLConnection connection = connect(url);

        long resourceSize = getResourceSize(connection);
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
                analyzer.setDownloadedInBytes(analyzer.getDownloadedInBytes() + len);
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

    public void download(String url, Path outpuPath, long start, long end) {
        HttpURLConnection connection = retrieve(url, start, end);

        try (
            InputStream bufferIn = new BufferedInputStream(connection.getInputStream());
            RandomAccessFile randomOut = new RandomAccessFile(outpuPath.toString(), "rw");
        ) {
            byte[] temporaryBuffer = new byte[Constants.DOWNLOAD_BUFFER_SIZE];
            int len = -1;
            while ((len = bufferIn.read(temporaryBuffer)) != -1) {
                // Log.info("Download from %s to %s", start, end);
                randomOut.write(temporaryBuffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            Log.error("Failed when trying to read the local downloaded file: %s",e);
        } 
        catch (IOException e) {
            Log.error("Failed when downlading from the url: %s", e);
        } finally {
            if (connection != null) connection.disconnect();
        }

    }

    public void split(String url, Path outputPath) {
        long totalResourceSize = getResourceSize(url);
        long start, end;
        long chunkSize = totalResourceSize / workers;
        for (int worker = 0; worker < workers; worker++) {
            start = worker * chunkSize;
            end = start + chunkSize - 1;
            if (worker == workers - 1) {
                end = 0;
            }
            
            ChunkDownloader chunkDownloader = new ChunkDownloader(url, start, end, outputPath);
            downloadThreadsPool.execute(chunkDownloader);
        }
    }
}
