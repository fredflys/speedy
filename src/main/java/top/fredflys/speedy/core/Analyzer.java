package top.fredflys.speedy.core;

import top.fredflys.speedy.constant.Constants;

public class Analyzer implements Runnable {

    private long totalInBytes;

    private long localFinishedInBytes;
    private volatile long downloadedInBytes = 0;
    private long previousDownloadedInBytes = 0;

    
    public void setDownloadedInBytes(long downloadedInBytes) {
        this.downloadedInBytes = downloadedInBytes;
    }

    public long getDownloadedInBytes() {
        return downloadedInBytes;
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

        long diffInBytes = downloadedInBytes - previousDownloadedInBytes;
        double downloadSpeedInKB = (double) diffInBytes / Constants.KB;
        double downdloadSpeedInMB = (double) diffInBytes / Constants.MB;
        previousDownloadedInBytes = downloadedInBytes;

        long totalDownloadedInBytes = localFinishedInBytes + downloadedInBytes;
        long remainingSizeInBytes = totalInBytes - totalDownloadedInBytes;
        double expectedRemainingTimeInSeconds = (double) remainingSizeInBytes / diffInBytes;
        if (expectedRemainingTimeInSeconds == Double.POSITIVE_INFINITY) {
            expectedRemainingTimeInSeconds = -1d;
        }

        String template = "Downloaded %.2f mb / %.2f mb at %s/s. Remaing time: %s.";
        String stats = String.format(template, 
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
        System.out.printf("\r%s", stats);
    }

}
