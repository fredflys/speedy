package top.fredflys.speedy.core;

import java.nio.file.Path;

public interface Downloader {
    void download(String url, Path outputPath);
}