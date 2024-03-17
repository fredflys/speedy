package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.Utils;

public class ChunkDownloader extends Downloader implements Runnable {

    private String downloadUrl;
    private long chunkStartPos;
    private long chunkEndPos;
    private Path chunkOutputPath;
    
    public ChunkDownloader (String url, long start, long end, Path path, int partNo) {
        downloadUrl = url;
        chunkStartPos = start;
        chunkEndPos = end;
        String suffix = String.format(".temp.%d", partNo);
        chunkOutputPath = Paths.get(path.toString() + suffix);
    }

    public ChunkDownloader() {
        super();
    }

    @Override
    public void download(String url, Path outputPath) {
        long totalResourceSize = getResourceSize(url);
        long start, end;
        long chunkSize = totalResourceSize / workers;
        for (int worker = 0; worker < workers; worker++) {
            start = worker * chunkSize;
            end = start + chunkSize - 1;
            if (worker == workers - 1) {
                end = 0;
            }
            
            ChunkDownloader chunkDownloader = new ChunkDownloader(url, start, end, outputPath, worker);
            downloadThreadsPool.execute(chunkDownloader);

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

    public void downloadChunk(String url, Path outpuPath, long start, long end) {
        HttpURLConnection connection = retrieve(url, start, end);

        try (
            InputStream bufferIn = new BufferedInputStream(connection.getInputStream());
            RandomAccessFile randomOut = new RandomAccessFile(outpuPath.toString(), "rw");
        ) {
            byte[] temporaryBuffer = new byte[Constants.DOWNLOAD_BUFFER_SIZE];
            int len = -1;
            while ((len = bufferIn.read(temporaryBuffer)) != -1) {
                Log.info("Download from %s to %s", start, end);
                randomOut.write(temporaryBuffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            Log.error("Failed when trying to read the local downloaded file: %s",e);
        } catch (IOException e) {
            Log.error("Failed when downlading from the url: %s", e);
        } finally {
            if (connection != null) connection.disconnect();
            
            // downloadThreadsPool.awaitTermination();
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

    @Override
    public void run() {
        downloadChunk(downloadUrl, chunkOutputPath, chunkStartPos, chunkEndPos);
    }
}
