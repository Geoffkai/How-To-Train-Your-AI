package com.howtotrainyourai.core;

import com.howtotrainyourai.gui.CardPanel;
import com.howtotrainyourai.gui.MainMenuPanel;
import com.howtotrainyourai.gui.PlayPanel;
import com.howtotrainyourai.gui.SettingsPanel;
import com.howtotrainyourai.gui.TutorialPanel;
import java.awt.*;
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardPanel cardPanel = new CardPanel();

        cardPanel.addScreen(CardPanel.MENU, new MainMenuPanel(cardPanel));
        cardPanel.addScreen(CardPanel.PLAY, new PlayPanel(cardPanel));
        cardPanel.addScreen(CardPanel.TUTORIAL, new TutorialPanel(cardPanel));
        cardPanel.addScreen(CardPanel.SETTINGS, new SettingsPanel(cardPanel));

        frame.add(cardPanel);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setVisible(true);

        cardPanel.showScreen(CardPanel.MENU);
    }
}