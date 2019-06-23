package de.intranda.iiif.downloader;

import java.io.IOException;

/**
 * Small runnable to show progress in a command-line application
 * 
 * @author Oliver Paetzel
 *
 */
public class StatusRunnable implements Runnable {
    private static final String[] rotateArr = new String[] { "|", "/", "-", "\\" };
    private volatile boolean shouldStop;
    private volatile String message = "";

    public StatusRunnable(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        int count = 0;
        while (!shouldStop) {
            String newMessage = "\r" + rotateArr[count] + " " + message;
            count = (count + 1) % 4;
            try {
                System.out.write(newMessage.getBytes());
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
            }
        }
        String newMessage = "\r✓ " + message + "\n";
        try {
            System.out.write(newMessage.getBytes());
        } catch (IOException e) {
        }
    }

    public void setShouldStop() {
        this.shouldStop = true;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
