package com.howtotrainyourai.core;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.QuestionSpeechService;
import com.howtotrainyourai.gui.CardPanel;
import com.howtotrainyourai.gui.MainMenuPanel;
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
        // aqua (macos) ignores custom JButton colors/borders otherwise, every screen's
        // buttons need this or they render invisible/unstyled on a mac
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // worst case we're back to whatever the platform default is
        }
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
            audioManager.playMusic();
        } catch (Exception e) {
            System.err.println("game audio unavailable: " + e.getMessage());
            audioManager.close();
        }
        QuestionSpeechService speechService = new QuestionSpeechService();

        // Register all screens
        cardPanel.addScreen(CardPanel.SPLASH, new SplashScreenPanel(cardPanel));

        // ScenePanel first: SetupPanel needs it, because SetupPanel is what starts a
        // session and hands the live engine over. Nothing here builds an engine --
        // that happens per session in SetupPanel, via GameEngineFactory.
        ScenePanel scenePanel = new ScenePanel(cardPanel, audioManager, speechService);
        TutorialPanel tutorialPanel = new TutorialPanel(cardPanel);
        SettingsPanel settingsPanel = new SettingsPanel(cardPanel, audioManager, speechService);
        cardPanel.addScreen(CardPanel.MENU, new MainMenuPanel(cardPanel));
        cardPanel.addScreen(CardPanel.SETUP, new SetupPanel(cardPanel, scenePanel));
        cardPanel.addScreen(CardPanel.PLAY, scenePanel);
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

        Rectangle usableScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(960, usableScreen.width), Math.min(600, usableScreen.height)));
        double initialScale = Math.min(1, Math.min(usableScreen.width / 1280.0, usableScreen.height / 800.0));
        frame.setSize((int) (1280 * initialScale), (int) (800 * initialScale));
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Boot directly into the splash screen
        cardPanel.showScreen(CardPanel.SPLASH);
    }
}
