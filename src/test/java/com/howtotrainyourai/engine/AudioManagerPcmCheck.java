package com.howtotrainyourai.engine;

public final class AudioManagerPcmCheck {
    public static void main(String[] args) {
        byte[] music = AudioManager.generateAmbientPcm();
        byte[] click = AudioManager.generateClickPcm();
        byte[] ceiling = AudioManager.generateCeilingLightPcm();
        if (music.length != 22050 * 3 / 2 * 4 * 2) throw new AssertionError("ambient duration changed");
        if (click.length == 0 || !hasSignal(music) || !hasSignal(click)
                || ceiling.length != (int) (22050 * 0.42) * 2) {
            throw new AssertionError("generated PCM is empty or silent");
        }
        int earlyPeak = peak(ceiling, 0, 22050 / 50);
        int latePeak = peak(ceiling, ceiling.length - 22050 / 50 * 2, ceiling.length);
        if (earlyPeak < 5000 || earlyPeak > 30000 || latePeak >= earlyPeak) {
            throw new AssertionError("ceiling-light bang lacks a sharp, decaying transient");
        }
        System.out.println("audio pcm check passed: " + music.length + " music bytes, "
                + ceiling.length + " ceiling-effect bytes");
    }

    private static boolean hasSignal(byte[] pcm) {
        for (byte sample : pcm) if (sample != 0) return true;
        return false;
    }

    private static int peak(byte[] pcm, int start, int end) {
        int peak = 0;
        for (int i = start; i < end; i += 2) {
            int sample = (short) ((pcm[i] & 0xff) | pcm[i + 1] << 8);
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }
}
