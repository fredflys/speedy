package top.fredflys.speedy;

import java.nio.file.Path;

import top.fredflys.speedy.core.ChunkDownloader;
import top.fredflys.speedy.core.Downloader;
import top.fredflys.speedy.util.Utils;

public class Main {
    public static void main(String[] args) {
        Path outputPath = Utils.parseArgs(args);
        Downloader downloader = new Downloader();
        downloader.download(args[0], outputPath);
    }
}