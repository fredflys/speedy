package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.URLUtils;
import top.fredflys.speedy.util.Utils;

public class ThreadPoolDownloader implements Downloader {
    public  int workers = Constants.WORKERS;
    public ThreadPoolExecutor downloadThreadsPool = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(workers));

    @Override
    public void download(String url, Path outputPath) {
        long totalResourceSize = URLUtils.getResourceSize(url);
        long start, end;
        long chunkSize = totalResourceSize / workers;
        for (int worker = 0; worker < workers; worker++) {
            start = worker * chunkSize;
            end = start + chunkSize - 1;
            if (worker == workers - 1) {
                end = 0;
            }
            
            ChunkDownloadJob task = new ChunkDownloadJob(url, start, end, outputPath, worker);
            downloadThreadsPool.execute(task);

        }

        downloadThreadsPool.shutdown();
        try {
            if (downloadThreadsPool.awaitTermination(10, TimeUnit.MINUTES)) {
                merge(outputPath);
                return;
            } 

            Log.error("Download timed out. Please try again later.");
        } catch (InterruptedException e) {
            Log.error("Download by chunk failed: %s", e);
        }
    }
    
    public void merge(Path outputPath) {
        byte[] temporaryBuffer = new byte[Constants.DOWNLOAD_BUFFER_SIZE];
        BufferedInputStream bufferIn = null;
        try (
            RandomAccessFile outputFile = new RandomAccessFile(outputPath.toString(), "rw");
        ) {
            String currentTempFileName;
            int len;;
            for (int worker = 0; worker < workers; worker++) {
                currentTempFileName = String.format("%s.temp.%d", outputPath.toString(), worker);
                bufferIn = new BufferedInputStream(new FileInputStream(currentTempFileName));
                while ((len = bufferIn.read(temporaryBuffer)) != -1) {
                    outputFile.write(temporaryBuffer, 0, len);
                }

                if (bufferIn != null) bufferIn.close();
            }

        } catch (IOException e) {
            Log.error("Output path is invalid: %s.", outputPath);
        } finally {
            for (int worker = 0; worker < workers; worker++) {
                Path currentTempFilePath = Paths.get(String.format("%s.temp.%d", outputPath.toString(), worker));
                try {
                    Files.deleteIfExists(currentTempFilePath);
                } catch (IOException e) {
                    Log.warn("Failed when trying to delete a chunk file: %s.", currentTempFilePath);
                }
            }
        }
    }
}
