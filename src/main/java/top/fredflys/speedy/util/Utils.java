package top.fredflys.speedy.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static Path parseArgs(String[] args) {
        if (args == null || args.length < 1) {
            Log.error("Usage : java -jar speedy.jar <url> [output path]");
        }

        try {
            new URL(args[0]).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            Log.error("Invalid download url: %s", args[0]);
        }

        String resourceName = getResourceName(args[0]);

        Path outputPath = null;
        Path currentPath = Paths.get(System.getProperty("user.dir"));

        // output directory and file name not specified
        if (args.length == 1) {
            outputPath = Paths.get(currentPath.toString(), resourceName);
            return outputPath;
        }

        File output = new File(args[1]);
        // System.out.println(output.isDirectory());
        // System.out.println(output.isFile());
        
        // a directory is provided
        if (output.isDirectory()) {
            return Paths.get(output.toString(), resourceName);
        }
        
        // output file alreasy exists
        if (output.isFile()) {
            Log.error("Output file exists. Please check the name and try again.");
        }

        // a directory is provided along with a new file name
        File parentFile = new File(getParentPath(args[1]));
        if (parentFile.isDirectory()) {
            return Paths.get(args[1]);
        }

        try {
            return Paths.get(currentPath.toString(), args[1]);
        } catch (InvalidPathException e) {
            Log.error("%s", e.toString());
        }

        return null;
    }

    static String getResourceName(String url) {
        if (url == null) return null;
        
        int lastSlashIndex = url.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            Log.error("Invalid url. Check the url provided and try again.");
        }
        
        return url.substring(lastSlashIndex + 1);
    }

    static String getParentPath(String path) {
        if (path == null) return null;
        
        int lastSlashIndex = path.lastIndexOf("/") == -1 ? path.lastIndexOf("\\") : path.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            Log.error("Invalid path. Check the path provided and try again.");
        }
        
        return path.substring(0, lastSlashIndex);
    }
}
