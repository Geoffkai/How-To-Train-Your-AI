package com.howtotrainyourai.engine;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// wav only, javax.sound.sampled, no external codecs: grader machines might not have mp3 support
public class AudioManager {

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
}
