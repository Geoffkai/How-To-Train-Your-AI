package com.howtotrainyourai.engine;

public final class AudioManagerPcmCheck {
    public static void main(String[] args) {
        byte[] music = AudioManager.generateAmbientPcm();
        byte[] click = AudioManager.generateClickPcm();
        if (music.length != 22050 * 1.5 * 4 * 2) throw new AssertionError("ambient duration changed");
        if (click.length == 0 || !hasSignal(music) || !hasSignal(click)) {
            throw new AssertionError("generated PCM is empty or silent");
        }
        System.out.println("audio pcm check passed: " + music.length + " music bytes, " + click.length + " effect bytes");
    }

    private static boolean hasSignal(byte[] pcm) {
        for (byte sample : pcm) if (sample != 0) return true;
        return false;
    }
}
