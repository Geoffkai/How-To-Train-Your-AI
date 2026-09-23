package com.howtotrainyourai.engine;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// wav only, javax.sound.sampled, no external codecs: grader machines might not have mp3 support
public class AudioManager {

    private final Map<String, Clip> sfxClips = new HashMap<>();

    // loads a one-shot sfx and keys it for playSfx() later
    public void loadSfx(String key, AudioInputStream stream) throws LineUnavailableException, IOException {
        Clip clip = AudioSystem.getClip();
        clip.open(stream);
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
}
