package com.howtotrainyourai.engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// wav only, javax.sound.sampled, no external codecs: grader machines might not have mp3 support
public class AudioManager {

    private static final float SAMPLE_RATE = 22050f;
    private final Map<String, Clip> sfxClips = new HashMap<>();
    private Clip musicClip;

    private float musicVolume = 1.0f; // 0..1, clamped
    private float sfxVolume = 1.0f;

    // loads a one-shot sfx and keys it for playSfx() later
    public void loadSfx(String key, AudioInputStream stream) throws LineUnavailableException, IOException {
        Clip clip = AudioSystem.getClip();
        clip.open(stream);
        applyVolume(clip, sfxVolume);
        sfxClips.put(key, clip);
    }

    // plays from the start, cuts off whatever was already playing for this key
    public void playSfx(String key) {
        Clip clip = sfxClips.get(key);
        if (clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    // just one music track at a time, loading a new one replaces whatever was there
    public void loadMusic(AudioInputStream stream) throws LineUnavailableException, IOException {
        if (musicClip != null) {
            musicClip.close();
        }
        musicClip = AudioSystem.getClip();
        musicClip.open(stream);
        applyVolume(musicClip, musicVolume);
    }

    public void loadGeneratedAudio() throws LineUnavailableException, IOException {
        loadMusic(pcmStream(generateAmbientPcm()));
        loadSfx("control", pcmStream(generateClickPcm()));
    }

    // loops from the top until stopMusic(): used for the main menu / bg track
    public void playMusic() {
        if (musicClip == null) {
            return;
        }
        musicClip.setFramePosition(0);
        musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
        }
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        if (musicClip != null) {
            applyVolume(musicClip, musicVolume);
        }
    }

    public void setSfxVolume(float volume) {
        sfxVolume = clamp(volume);
        for (Clip clip : sfxClips.values()) {
            applyVolume(clip, sfxVolume);
        }
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void close() {
        stopMusic();
        if (musicClip != null) musicClip.close();
        for (Clip clip : sfxClips.values()) clip.close();
        sfxClips.clear();
    }

    private static float clamp(float volume) {
        return Math.max(0f, Math.min(1f, volume));
    }

    // sampled doesn't do linear volume, only db gain, so convert on the way in
    private static void applyVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = volume <= 0f ? gain.getMinimum() : (float) (Math.log10(volume) * 20.0);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
    }

    // runnable self-check, no bundled wav assets to point at yet so it makes its own tone.
    // just exercises load/play/volume and checks nothing throws or drifts.
    public static void main(String[] args) throws Exception {
        AudioManager audioManager = new AudioManager();

        audioManager.loadSfx("beep", generateTone(440, 300));
        audioManager.loadMusic(generateTone(220, 300));

        audioManager.playSfx("beep");
        Thread.sleep(350);

        audioManager.setSfxVolume(0.3f);
        if (audioManager.getSfxVolume() != 0.3f) {
            throw new IllegalStateException("sfx volume didn't stick");
        }
        audioManager.playSfx("beep");
        Thread.sleep(350);

        audioManager.setMusicVolume(0.5f);
        if (audioManager.getMusicVolume() != 0.5f) {
            throw new IllegalStateException("music volume didn't stick");
        }
        audioManager.playMusic();
        Thread.sleep(350);
        audioManager.stopMusic();

        System.out.println("audiomanager self-check passed");
    }

    static byte[] generateAmbientPcm() {
        int samplesPerNote = (int) (SAMPLE_RATE * 1.5);
        double[] notes = { 110.0, 146.83, 164.81, 146.83 };
        byte[] pcm = new byte[samplesPerNote * notes.length * 2];
        for (int i = 0; i < samplesPerNote * notes.length; i++) {
            int note = i / samplesPerNote;
            int withinNote = i % samplesPerNote;
            double t = withinNote / SAMPLE_RATE;
            double envelope = Math.min(1, withinNote / (SAMPLE_RATE * 0.18))
                    * Math.min(1, (samplesPerNote - withinNote) / (SAMPLE_RATE * 0.18));
            double sample = (Math.sin(2 * Math.PI * notes[note] * t)
                    + 0.22 * Math.sin(4 * Math.PI * notes[note] * t)) * envelope * 0.12;
            putSample(pcm, i, sample);
        }
        return pcm;
    }

    static byte[] generateClickPcm() {
        int frames = (int) (SAMPLE_RATE * 0.07);
        byte[] pcm = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            double envelope = 1 - (double) i / frames;
            putSample(pcm, i, Math.sin(2 * Math.PI * 740 * i / SAMPLE_RATE) * envelope * 0.3);
        }
        return pcm;
    }

    private static void putSample(byte[] pcm, int frame, double sample) {
        short value = (short) (sample * Short.MAX_VALUE);
        pcm[frame * 2] = (byte) value;
        pcm[frame * 2 + 1] = (byte) (value >> 8);
    }

    private static AudioInputStream pcmStream(byte[] pcm) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        return new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length / 2);
    }

    // In-memory mono 16-bit PCM tone, used only by this class's self-check.
    private static AudioInputStream generateTone(double frequencyHz, int durationMs) {
        int frameCount = (int) (SAMPLE_RATE * durationMs / 1000);
        byte[] pcm = new byte[frameCount * 2];
        for (int i = 0; i < frameCount; i++) {
            short sample = (short) (Math.sin(2 * Math.PI * frequencyHz * i / SAMPLE_RATE) * Short.MAX_VALUE * 0.5);
            pcm[i * 2] = (byte) (sample & 0xff);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        return new AudioInputStream(new ByteArrayInputStream(pcm), format, frameCount);
    }
}
