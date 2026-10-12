package com.howtotrainyourai.engine;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class QuestionSpeechService implements AutoCloseable {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "question-speech");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong request = new AtomicLong();
    private volatile Voice voice;
    private volatile boolean enabled = true;
    private volatile float volume = 1f;
    private volatile boolean closed;

    public void speak(String text) {
        long current = request.incrementAndGet();
        if (closed || !enabled || text == null || text.isBlank()) return;
        Voice speaking = voice;
        if (speaking != null && speaking.getAudioPlayer() != null) speaking.getAudioPlayer().cancel();
        worker.execute(() -> {
            if (closed || !enabled || current != request.get()) return;
            try {
                Voice currentVoice = voice();
                currentVoice.setVolume(volume);
                if (closed || !enabled || current != request.get()) return;
                currentVoice.speak(text);
                if (current != request.get()) currentVoice.getAudioPlayer().cancel();
            } catch (RuntimeException | LinkageError e) {
                enabled = false;
                System.err.println("question speech unavailable: " + e.getMessage());
            }
        });
    }

    public void cancel() {
        request.incrementAndGet();
        Voice current = voice;
        if (current != null && current.getAudioPlayer() != null) current.getAudioPlayer().cancel();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) cancel();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public float getVolume() {
        return volume;
    }

    private Voice voice() {
        if (voice == null) {
            synchronized (this) {
                if (voice == null) {
                    Voice loaded = Arrays.stream(new KevinVoiceDirectory().getVoices())
                            .filter(candidate -> "kevin16".equals(candidate.getName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("kevin16 voice not found"));
                    loaded.allocate();
                    voice = loaded;
                }
            }
        }
        return voice;
    }

    @Override
    public void close() {
        closed = true;
        cancel();
        worker.execute(() -> {
            if (voice != null) {
                try {
                    voice.deallocate();
                } catch (RuntimeException e) {
                    System.err.println("question speech shutdown failed: " + e.getMessage());
                }
            }
        });
        worker.shutdown();
    }
}
