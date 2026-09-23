package com.howtotrainyourai.gui;

import javax.swing.*;
import java.awt.*;

// question screen skeleton: ladder + lifeline row up top, nothing wired yet.
// question card and result banner come next.
public class PlayPanel extends JPanel {

    private static final int TOTAL_QUESTIONS = 15;

    public PlayPanel(CardPanel cardPanel) {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);

        JPanel ladderPanel = new JPanel(new GridLayout(1, TOTAL_QUESTIONS, 4, 0));
        ladderPanel.setBackground(Color.WHITE);
        for (int i = 0; i < TOTAL_QUESTIONS; i++) {
            JLabel segment = new JLabel();
            segment.setOpaque(true);
            segment.setBackground(new Color(224, 224, 224));
            segment.setPreferredSize(new Dimension(20, 12));
            ladderPanel.add(segment);
        }
        topPanel.add(ladderPanel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel lifelinePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        lifelinePanel.setBackground(Color.WHITE);
        for (String label : new String[] { "BC", "PR", "OV" }) {
            lifelinePanel.add(createLifelineButton(label));
            // real lifeline behavior is week 4 scope, layout only here
        }
        topPanel.add(lifelinePanel);

        add(topPanel, BorderLayout.NORTH);

        JLabel placeholder = new JLabel("question card goes here", SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.PLAIN, 20));
        add(placeholder, BorderLayout.CENTER);

        JButton backButton = createLifelineButton("Back");
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 30));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createLifelineButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(new Color(224, 224, 224));
        button.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        return button;
    }
}
