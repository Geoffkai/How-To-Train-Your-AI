package com.howtotrainyourai.engine;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sound.sampled.AudioFileFormat;

public final class FreeTtsSpeechCheck {
    public static void main(String[] args) throws Exception {
        Voice voice = Arrays.stream(new KevinVoiceDirectory().getVoices())
                .filter(candidate -> "kevin16".equals(candidate.getName()))
                .findFirst().orElseThrow();
        Path directory = Files.createTempDirectory("freetts-check-");
        Path output = directory.resolve("question");
        voice.allocate();
        SingleFileAudioPlayer player = new SingleFileAudioPlayer(output.toString(), AudioFileFormat.Type.WAVE);
        voice.setAudioPlayer(player);
        try {
            voice.speak("Which search algorithm explores the shallowest unexpanded node first?");
        } finally {
            voice.deallocate();
            player.close();
        }
        Path wav = directory.resolve("question.wav");
        if (Files.size(wav) <= 44) throw new AssertionError("speech WAV is empty");
        long size = Files.size(wav);
        try (var stream = javax.sound.sampled.AudioSystem.getAudioInputStream(wav.toFile())) {
            byte[] samples = stream.readAllBytes();
            boolean audible = false;
            for (byte sample : samples) audible |= sample != 0;
            if (!audible) throw new AssertionError("speech output is silent");
        }
        Files.delete(wav);
        Files.delete(directory);
        System.out.println("FreeTTS speech check passed: " + size + " bytes");
    }
}
