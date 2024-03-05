package top.fredflys.speedy.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {

    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    };

    public static void info(String msg, Object... args) {
        print(LogLevel.INFO, msg, args);
    }

    public static void warn(String msg, Object... args) {
        print(LogLevel.WARN, msg, args);
    }

    public static void error(String msg, Object... args) {
        print(LogLevel.ERROR, msg, args);
        System.exit(1);
    }

    private static void print(LogLevel level, String msg, Object... args) {
        StringBuilder result = new StringBuilder();

        if (args != null && args.length > 0) {
            msg = String.format(msg, args);
        }

        String currentTime = LocalTime.now().format(
            DateTimeFormatter.ofPattern("hh:mm:ss")
        );
        result.append(currentTime);

        if (level == LogLevel.INFO) result.append(" INFO ");
        if (level == LogLevel.WARN) result.append(" WARN ");
        if (level == LogLevel.ERROR) result.append(" ERROR ");

        String threadName = Thread.currentThread().getName();
        result.append(threadName);
        result.append(" ");

        result.append(msg);
        System.out.println(result.toString());
    }
}