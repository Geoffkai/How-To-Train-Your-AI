package com.howtotrainyourai.gui;

import java.awt.*;
import javax.swing.*;

public class SettingsPanel extends JPanel {

    public SettingsPanel(CardPanel cardPanel) {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 48));
        add(titleLabel, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
        backButton.setBackground(new Color(224, 224, 224));
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 30));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(backButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }
}