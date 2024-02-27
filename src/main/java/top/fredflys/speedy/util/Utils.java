package top.fredflys.speedy.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static Path parseArgs(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Usage : java -jar speedy.jar <url> [-o]");
            System.exit(1);
        }

        try {
            new URL(args[0]).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            System.err.println("Error: " + e);
        }

        String resourceName = getResourceName(args[0]);

        Path outputPath = null;
        Path currentPath = Paths.get(System.getProperty("user.dir"));

        // output directory and file name not specified
        if (args.length == 1) {
            outputPath = Paths.get(currentPath.toString(), resourceName);
            System.out.println(outputPath.toString());
            return outputPath;
        }

        File output = new File(args[1]);
        System.out.println(output.isDirectory());
        System.out.println(output.isFile());
        
        if (output.isDirectory()) {
            return Paths.get(output.toString(), resourceName);
        }

        return Paths.get(currentPath.toString(), args[1]);
    }

    public static String getResourceName(String url) {
        if (url == null) return null;
        
        int lastSlashIndex = url.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            System.err.println("Invalid url. Check the url provided and try again.");
            System.exit(1);
        }
        
        return url.substring(lastSlashIndex + 1);
    }
}
