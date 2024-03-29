package top.fredflys.speedy.core;

import java.util.concurrent.atomic.LongAdder;

import top.fredflys.speedy.constant.Constants;
import top.fredflys.speedy.util.Utils;

public class Analyzer implements Runnable {

    private long totalInBytes;

    private long localFinishedInBytes;
    private volatile LongAdder downloadedInBytes = new LongAdder();
    private volatile LongAdder previousDownloadedInBytes = new LongAdder();
    private int prevStatsLength = 0;

    public int getPrevStatsLength() {
        return prevStatsLength;
    }
    public void addDownloadedInBytes(long downloadedInBytes) {
        this.downloadedInBytes.add(downloadedInBytes);
    }

    public long getDownloadedInBytes() {
        return downloadedInBytes.longValue();
    }

    public void setLocalFinishedInBytes(long localFinishedInBytes) {
        this.localFinishedInBytes = localFinishedInBytes;
    }

    public Analyzer(long fileTotalSize, long fileFinishedSize) {
        totalInBytes = fileTotalSize;
        localFinishedInBytes = fileFinishedSize;
    }

    @Override
    public void run() {

        long diffInBytes = downloadedInBytes.longValue() - previousDownloadedInBytes.longValue();
        double downloadSpeedInKB = (double) diffInBytes / Constants.KB;
        double downdloadSpeedInMB = (double) diffInBytes / Constants.MB;
        previousDownloadedInBytes.reset();
        previousDownloadedInBytes.add(downloadedInBytes.longValue());

        long totalDownloadedInBytes = localFinishedInBytes + downloadedInBytes.longValue();
        long remainingSizeInBytes = totalInBytes - totalDownloadedInBytes;
        double expectedRemainingTimeInSeconds = (double) remainingSizeInBytes / diffInBytes;
        // System.out.printf("remaing size: %d. diff in bytes: %d. expected time: %d\n", remainingSizeInBytes, diffInBytes, expectedRemainingTimeInSeconds);
        if (expectedRemainingTimeInSeconds == Double.POSITIVE_INFINITY) {
            expectedRemainingTimeInSeconds = -1d;
        }

        String template = "Downloaded %.2f%% %.2f mb / %.2f mb at %s/s. Remaing time: %s.";
        String stats = String.format(template,
            (double) totalDownloadedInBytes / totalInBytes * 100,
            (double) totalDownloadedInBytes / Constants.MB, 
            (double) totalInBytes / Constants.MB, 
            downdloadSpeedInMB < 1  
                ? String.format("%.2f kb", downloadSpeedInKB)
                : String.format("%.2f mb", downdloadSpeedInMB),
            expectedRemainingTimeInSeconds == -1
                ? "Almost done"
                : String.format("%.1f /s", expectedRemainingTimeInSeconds)
        );


        // download info is displayed on one line 
        Utils.printOnSameLineWithRightPadding(
            String.format("%-80s",stats), 
            prevStatsLength
        );

        prevStatsLength = stats.length();
    }

}
