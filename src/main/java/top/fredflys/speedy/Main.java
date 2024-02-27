package top.fredflys.speedy;

import java.nio.file.Path;

import top.fredflys.speedy.util.Utils;

public class Main {
    public static void main(String[] args) {
        Path outputPath = Utils.parseArgs(args);
    }
}