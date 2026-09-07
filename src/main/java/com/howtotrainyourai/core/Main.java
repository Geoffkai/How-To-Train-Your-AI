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
        frame.setVisible(true);

        cardPanel.showScreen(CardPanel.MENU);
    }
}