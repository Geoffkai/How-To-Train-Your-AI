package com.howtotrainyourai.core;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.QuestionSpeechService;
import com.howtotrainyourai.gui.CardPanel;
import com.howtotrainyourai.gui.EndingPanel;
import com.howtotrainyourai.gui.MainMenuPanel;
import com.howtotrainyourai.gui.IntroPanel;
import com.howtotrainyourai.gui.ScenePanel;
import com.howtotrainyourai.gui.SettingsPanel;
import com.howtotrainyourai.gui.SplashScreenPanel;
import com.howtotrainyourai.gui.SetupPanel;
import com.howtotrainyourai.gui.TutorialPanel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("How to Train Your AI");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        CardPanel cardPanel = new CardPanel();
        AudioManager audioManager = new AudioManager();
        try {
            audioManager.loadGeneratedAudio();
            audioManager.setMusicVolume(0.35f);
            audioManager.setSfxVolume(0.35f);
        } catch (Exception e) {
            System.err.println("game audio unavailable: " + e.getMessage());
            audioManager.close();
        }
        // home track everywhere except the question screens (and the splash, which has its own hum)
        cardPanel.setScreenListener(screen -> {
            if (CardPanel.SPLASH.equals(screen)) {
                return;
            }
            boolean questions = CardPanel.INTRO.equals(screen) || CardPanel.PLAY.equals(screen);
            audioManager.playTrack(questions ? "/audio/QuestionBGM.wav" : "/audio/HomeScreen.wav");
        });
        QuestionSpeechService speechService = new QuestionSpeechService();

        // Register all screens
        cardPanel.addScreen(CardPanel.SPLASH, new SplashScreenPanel(cardPanel));

        ScenePanel scenePanel = new ScenePanel(cardPanel, audioManager, speechService);
        IntroPanel introPanel = new IntroPanel(cardPanel, scenePanel);
        TutorialPanel tutorialPanel = new TutorialPanel(cardPanel);
        SettingsPanel settingsPanel = new SettingsPanel(cardPanel, audioManager, speechService);
        cardPanel.addScreen(CardPanel.MENU, new MainMenuPanel(cardPanel));
        cardPanel.addScreen(CardPanel.SETUP, new SetupPanel(cardPanel, introPanel));
        cardPanel.addScreen(CardPanel.INTRO, introPanel);
        cardPanel.addScreen(CardPanel.PLAY, scenePanel);
        EndingPanel endingPanel = new EndingPanel(cardPanel);
        scenePanel.setEndingPanel(endingPanel);
        cardPanel.addScreen(CardPanel.ENDING, endingPanel);
        cardPanel.addScreen(CardPanel.TUTORIAL, tutorialPanel);
        cardPanel.addScreen(CardPanel.SETTINGS, settingsPanel);

        frame.add(cardPanel);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                scenePanel.shutdown();
                tutorialPanel.shutdown();
                settingsPanel.shutdown();
                speechService.close();
                audioManager.close();
                frame.dispose();
            }
        });

        // Strip OS window title bar, borders, and X button
        frame.setUndecorated(true);
        frame.setResizable(false);

        // Fill the entire monitor display
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Boot directly into the splash screen
        cardPanel.showScreen(CardPanel.SPLASH);
    }
}
