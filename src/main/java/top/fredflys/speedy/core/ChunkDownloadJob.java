package top.fredflys.speedy.core;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Log;
import top.fredflys.speedy.util.URLUtils;

public class ChunkDownloadJob implements Runnable {
    private String downloadUrl;
    private long chunkStartPos;
    private long chunkEndPos;
    private Path chunkOutputPath;

    public ChunkDownloadJob (String url, long start, long end, Path path, int partNo) {
        downloadUrl = url;
        chunkStartPos = start;
        chunkEndPos = end;
        String suffix = String.format(".temp.%d", partNo);
        chunkOutputPath = Paths.get(path.toString() + suffix);
    }

    @Override
    public void run() {
        downloadChunk(downloadUrl, chunkOutputPath, chunkStartPos, chunkEndPos);
    }
    
    public void downloadChunk(String url, Path outpuPath, long start, long end) {
        HttpURLConnection connection = URLUtils.retrieve(url, start, end);

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
        }
    }
}
