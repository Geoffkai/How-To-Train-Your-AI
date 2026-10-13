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

public class SetupPanel extends JPanel {

    private static final Color CHALK = new Color(0xF2, 0xEC, 0xD8);
    private static final Color MUTED = new Color(0x7C, 0x7E, 0x5D);
    private static final Color GOLD = new Color(0xC2, 0xA6, 0x6E);
    private static final Color PLATE = new Color(0x0C, 0x0A, 0x09);
    private static final Font LABEL_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);
    private static final Font CONTROL_FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);

    private final CardPanel cardPanel;
    private final GameScreen questionScreen;
    private final JTextField nameField;
    private final JRadioButton standardButton;
    private final JRadioButton highRiskButton;
    private final JLabel statusLabel;

    public SetupPanel(CardPanel cardPanel, GameScreen questionScreen) {
        this.cardPanel = cardPanel;
        this.questionScreen = questionScreen;

        setBackground(PLATE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("RESTORATION BAY // OPERATOR INTAKE");
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        titleLabel.setForeground(CHALK);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 28, 0);
        add(titleLabel, gbc);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(0x18, 0x15, 0x11));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.anchor = GridBagConstraints.WEST;
        fgbc.insets = new Insets(0, 0, 12, 12);

        fgbc.gridx = 0;
        fgbc.gridy = 0;
        form.add(createLabel("OPERATOR ID:"), fgbc);

        nameField = new JTextField(18);
        nameField.setFont(CONTROL_FONT);
        nameField.setForeground(CHALK);
        nameField.setCaretColor(CHALK);
        nameField.setBackground(new Color(0x08, 0x0E, 0x0A));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MUTED), BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        fgbc.gridx = 1;
        form.add(nameField, fgbc);

        fgbc.gridx = 0;
        fgbc.gridy = 1;
        fgbc.insets = new Insets(14, 0, 8, 12);
        form.add(createLabel("RESTORE MODE:"), fgbc);

        // Labels spell out what each protocol actually changes, the same way
        // TerminalGameLoop's prompt does -- the player can't pick sensibly
        // from the bare names alone.
        standardButton = new JRadioButton("STANDARD  /  full validation, 3 service tools, Q5 + Q10 safepoints", true);
        highRiskButton = new JRadioButton("HIGH RISK / 2 service tools, Q5 safepoint, double recovery units");
        ButtonGroup protocolGroup = new ButtonGroup();
        protocolGroup.add(standardButton);
        protocolGroup.add(highRiskButton);
        for (JRadioButton button : new JRadioButton[] { standardButton, highRiskButton }) {
            button.setBackground(new Color(0x18, 0x15, 0x11));
            button.setForeground(CHALK);
            button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            button.setFocusPainted(false);
        }

        JPanel protocolPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        protocolPanel.setBackground(new Color(0x18, 0x15, 0x11));
        protocolPanel.add(standardButton);
        protocolPanel.add(highRiskButton);
        fgbc.gridx = 1;
        form.add(protocolPanel, fgbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        add(form, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonRow.setOpaque(false);

        JButton backButton = createButton("[ ABORT ]");
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        JButton beginButton = createButton("[ START RESTORATION ]");
        beginButton.addActionListener(e -> beginSession());
        buttonRow.add(backButton);
        buttonRow.add(beginButton);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(buttonRow, gbc);

        statusLabel = new JLabel("ENTER OPERATOR ID AND SELECT A RESTORE PROTOCOL");
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 0, 0, 0);
        add(statusLabel, gbc);

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
        statusLabel.setText("ENTER OPERATOR ID AND SELECT A RESTORE PROTOCOL");
        statusLabel.setForeground(MUTED);
        nameField.requestFocusInWindow();
    }

    private void beginSession() {
        String trainerName = nameField.getText().trim();
        if (trainerName.isEmpty()) {
            statusLabel.setText("OPERATOR ID REQUIRED // SESSION NOT STARTED");
            statusLabel.setForeground(new Color(0xD6, 0x9B, 0x6B));
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
            statusLabel.setText("BANK ERROR // " + e.getMessage());
            statusLabel.setForeground(new Color(0xD6, 0x9B, 0x6B));
            return;
        }

        questionScreen.startGame(engine);
        cardPanel.showScreen(CardPanel.PLAY);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(CONTROL_FONT);
        button.setForeground(CHALK);
        button.setBackground(new Color(0x28, 0x22, 0x18));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return button;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(GOLD);
        return label;
    }
}
