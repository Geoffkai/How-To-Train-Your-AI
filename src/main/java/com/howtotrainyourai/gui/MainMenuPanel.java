package com.howtotrainyourai.gui;

import java.awt.*;
import javax.swing.*;

public class MainMenuPanel extends JPanel {

    public MainMenuPanel(CardPanel cardPanel) {
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        // Title
        JLabel titleLabel = new JLabel("How to Train Your AI");
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 48));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 80, 0);
        add(titleLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setPreferredSize(new Dimension(320, 220));

        JButton playButton = createMenuButton("Play");
        JButton tutorialButton = createMenuButton("Tutorial");
        JButton settingsButton = createMenuButton("Settings");
        JButton exitButton = createMenuButton("Exit");

        playButton.addActionListener(e -> cardPanel.showScreen(CardPanel.PLAY));
        settingsButton.addActionListener(e -> cardPanel.showScreen(CardPanel.SETTINGS));
        tutorialButton.addActionListener(e -> cardPanel.showScreen(CardPanel.TUTORIAL));
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(playButton);
        buttonPanel.add(tutorialButton);
        buttonPanel.add(settingsButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(buttonPanel, gbc);
    }

    // Helper method to create a styled button
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 20));
        button.setBackground(new Color(224, 224, 224));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        return button;
    }
}