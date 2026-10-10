package com.howtotrainyourai.gui;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundManager {

    /**
     * Plays a .wav sound clip once in the background.
     */
    public static void playSound(String resourcePath) {
        playSound(resourcePath, -1);
    }

    /**
     * Plays a sound and stops it after durationMs milliseconds (-1 to play until finish).
     */
    public static void playSound(String resourcePath, int durationMs) {
        new Thread(() -> {
            try (InputStream is = SoundManager.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("[SoundManager] Resource not found: " + resourcePath);
                    return;
                }
                try (BufferedInputStream bis = new BufferedInputStream(is);
                     AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis)) {

                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();

                    if (durationMs > 0) {
                        while (!clip.isRunning()) {
                            Thread.sleep(10);
                        }
                        Thread.sleep(durationMs);
                        clip.stop();
                        clip.close();
                    }
                }
            } catch (Exception e) {
                System.err.println("[SoundManager] Playback error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Preloads an audio clip into memory so it can be played instantly without I/O delay.
     */
    public static Clip loadClip(String resourcePath) {
        try (InputStream is = SoundManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[SoundManager] Resource not found for preloading: " + resourcePath);
                return null;
            }
            try (BufferedInputStream bis = new BufferedInputStream(is);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis)) {

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                return clip;
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] Error preloading clip: " + e.getMessage());
            return null;
        }
    }

    /**
     * Plays a preloaded clip and optionally stops it after durationMs.
     */
    public static void playPreloaded(Clip clip, int durationMs) {
        if (clip == null) return;
        new Thread(() -> {
            try {
                long skipMicros = 200_000; // Skip initial 200ms dead air if present
                if (clip.getMicrosecondLength() > skipMicros) {
                    clip.setMicrosecondPosition(skipMicros);
                } else {
                    clip.setFramePosition(0);
                }

                clip.start();
                if (durationMs > 0) {
                    Thread.sleep(durationMs);
                    clip.stop();
                    clip.close();
                }
            } catch (Exception e) {
                System.err.println("[SoundManager] Error playing preloaded clip: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Seamlessly loops a preloaded clip continuously until stopped.
     */
    public static void loopPreloaded(Clip clip) {
        if (clip == null) return;
        new Thread(() -> {
            try {
                clip.setFramePosition(0);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (Exception e) {
                System.err.println("[SoundManager] Error looping clip: " + e.getMessage());
            }
        }).start();
    }
}