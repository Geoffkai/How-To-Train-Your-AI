package com.howtotrainyourai.gui;

import com.howtotrainyourai.core.GameEngineFactory;
import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.Protocol;
import java.awt.*;
import javax.swing.*;

/**
 * Collects the two things GameEngine.startSession() needs -- trainer name and
 * protocol -- then starts the session and hands the live engine to the question
 * screen.
 *
 * This is the GUI's equivalent of TerminalGameLoop's opening prompts. It's also
 * the only screen that builds an engine, which keeps session creation in one
 * place instead of spread across the menu and the question screen.
 */

// Placeholder lang po to
public class SetupPanel extends JPanel {

    private final CardPanel cardPanel;
    private final GameScreen questionScreen;
    private final JTextField nameField;
    private final JRadioButton standardButton;
    private final JRadioButton highRiskButton;

    public SetupPanel(CardPanel cardPanel, GameScreen questionScreen) {
        this.cardPanel = cardPanel;
        this.questionScreen = questionScreen;

        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Begin Restoration");
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 40, 0);
        add(titleLabel, gbc);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.anchor = GridBagConstraints.WEST;
        fgbc.insets = new Insets(0, 0, 10, 10);

        fgbc.gridx = 0;
        fgbc.gridy = 0;
        form.add(new JLabel("Trainer name:"), fgbc);

        nameField = new JTextField(18);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fgbc.gridx = 1;
        form.add(nameField, fgbc);

        fgbc.gridx = 0;
        fgbc.gridy = 1;
        fgbc.insets = new Insets(20, 0, 10, 10);
        form.add(new JLabel("Protocol:"), fgbc);

        // Labels spell out what each protocol actually changes, the same way
        // TerminalGameLoop's prompt does -- the player can't pick sensibly
        // from the bare names alone.
        standardButton = new JRadioButton("Standard    (3 lifelines, checkpoints at Q5 and Q10)", true);
        highRiskButton = new JRadioButton("High Risk   (2 lifelines, checkpoint at Q5, double tokens)");
        ButtonGroup protocolGroup = new ButtonGroup();
        protocolGroup.add(standardButton);
        protocolGroup.add(highRiskButton);
        for (JRadioButton button : new JRadioButton[] { standardButton, highRiskButton }) {
            button.setBackground(Color.WHITE);
            button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        }

        JPanel protocolPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        protocolPanel.setBackground(Color.WHITE);
        protocolPanel.add(standardButton);
        protocolPanel.add(highRiskButton);
        fgbc.gridx = 1;
        form.add(protocolPanel, fgbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 40, 0);
        add(form, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonRow.setBackground(Color.WHITE);

        JButton backButton = createButton("Back");
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        JButton beginButton = createButton("Begin");
        beginButton.addActionListener(e -> beginSession());
        buttonRow.add(backButton);
        buttonRow.add(beginButton);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(buttonRow, gbc);

        // Enter anywhere in the name field starts the session.
        nameField.addActionListener(e -> beginSession());

        // CardLayout.show() fires componentShown, so a second visit starts clean
        // instead of inheriting the previous run's name and protocol.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                reset();
            }
        });
    }

    /** Clears the form so a second run doesn't inherit the last one's name. */
    public void reset() {
        nameField.setText("");
        standardButton.setSelected(true);
        nameField.requestFocusInWindow();
    }

    private void beginSession() {
        String trainerName = nameField.getText().trim();
        if (trainerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a trainer name first.",
                    "Name required", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }

        Protocol protocol = highRiskButton.isSelected() ? Protocol.HIGH_RISK : Protocol.STANDARD;

        // Building the engine reads the CSV bank, and startSession() builds the
        // 15-question set from it -- either can throw if the bank is missing or
        // too thin for a full session. Stay on this screen and say so, rather
        // than pushing a dead question screen in front of the player.
        GameEngine engine;
        try {
            engine = GameEngineFactory.createDefault();
            engine.startSession(trainerName, protocol);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not start a session:\n" + e.getMessage(),
                    "Question bank problem", JOptionPane.ERROR_MESSAGE);
            return;
        }

        questionScreen.startGame(engine);
        cardPanel.showScreen(CardPanel.PLAY);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 18));
        button.setBackground(new Color(224, 224, 224));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        return button;
    }
}
