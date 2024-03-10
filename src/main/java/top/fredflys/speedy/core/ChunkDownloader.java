package top.fredflys.speedy.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import top.fredflys.speedy.util.Utils;

public class ChunkDownloader extends Downloader implements Runnable {

    private String downloadUrl;
    private long chunkStartPos;
    private long chunkEndPos;
    private Path chunkOutputPath;
    
    public ChunkDownloader (String url, long start, long end, Path path) {
        downloadUrl = url;
        chunkStartPos = start;
        chunkEndPos = end;
        String suffix = String.format(".temp.%d", start);
        chunkOutputPath = Paths.get(path.toString() + suffix);
    }


    @Override
    public void run() {
        download(downloadUrl, chunkOutputPath, chunkStartPos, chunkEndPos);
    }
}
