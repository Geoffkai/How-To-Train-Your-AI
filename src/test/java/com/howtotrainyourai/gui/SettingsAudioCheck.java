package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.QuestionSpeechService;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public final class SettingsAudioCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        AudioManager audio = new AudioManager();
        QuestionSpeechService speech = new QuestionSpeechService();
        SettingsPanel[] settingsPanel = new SettingsPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                SettingsPanel settings = new SettingsPanel(new CardPanel(), audio, speech);
                settingsPanel[0] = settings;
                settings.setSize(1600, 1000);
                settings.doLayout();
                JSlider[] sliders = new JSlider[3];
                JCheckBox speak = null;
                for (Component component : settings.getComponents()) {
                    if (component instanceof JSlider slider) {
                        for (int i = 0; i < sliders.length; i++) {
                            if (sliders[i] == null) { sliders[i] = slider; break; }
                        }
                    } else if (component instanceof JCheckBox checkbox) {
                        speak = checkbox;
                    }
                }
                if (speak == null || sliders[2] == null) throw new AssertionError("settings audio controls missing");
                for (JSlider slider : sliders) {
                    if (slider == null || slider.getWidth() == 0 || slider.getHeight() == 0) {
                        throw new AssertionError("volume control not laid out");
                    }
                }
                sliders[0].setValue(42);
                sliders[1].setValue(31);
                sliders[2].setValue(67);
                if (audio.getMusicVolume() != 0.42f || audio.getSfxVolume() != 0.31f
                        || speech.getVolume() != 0.67f) throw new AssertionError("volume change not applied");
                speak.doClick();
                if (speech.isEnabled()) throw new AssertionError("speak toggle not applied");
            });
            System.out.println("settings audio check passed");
        } finally {
            if (settingsPanel[0] != null) SwingUtilities.invokeAndWait(() -> settingsPanel[0].shutdown());
            speech.close();
            audio.close();
        }
    }
}
