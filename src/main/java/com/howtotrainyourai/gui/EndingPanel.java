package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.SessionResult;
import java.awt.*;
import javax.swing.*;

/**
 * End-of-session screen: "AI CORE: FULLY ACTIVATED" on a win, "RESTORATION
 * FAILED" on a wrong answer, "SESSION CLOSED" on Return. Each shows the six
 * capability states and the session summary that was filed in the Training
 * Records, with Retry and Back to Menu.
 *
 * Fed by ScenePanel through show(); it never talks to the engine.
 */
public class EndingPanel extends JPanel {

    private static final Color PLATE = new Color(0x0C, 0x0A, 0x09);
    private static final Color FORM_BG = new Color(0x18, 0x15, 0x11);
    private static final Color CHALK = new Color(0xF2, 0xEC, 0xD8);
    private static final Color MUTED = new Color(0x7C, 0x7E, 0x5D);
    private static final Color GOLD = new Color(0xC2, 0xA6, 0x6E);
    private static final Color GREEN = new Color(0x65, 0xDE, 0x92);
    private static final Color RED = new Color(0xDF, 0x4D, 0x42);

    private static final String[] CAPABILITIES = {
            "MEMORY", "UNDERSTANDING", "APPLICATION", "ANALYSIS", "EVALUATION", "SYNTHESIS" };

    private final JLabel titleLabel = new JLabel();
    private final JLabel bodyLabel = new JLabel();

    public EndingPanel(CardPanel cardPanel) {
        setBackground(PLATE);
        setLayout(new GridBagLayout());

        JPanel form = new JPanel(new BorderLayout(0, 18));
        form.setBackground(FORM_BG);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2),
                BorderFactory.createEmptyBorder(26, 40, 26, 40)));

        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(titleLabel, BorderLayout.NORTH);

        bodyLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        bodyLabel.setVerticalAlignment(SwingConstants.TOP);
        form.add(bodyLabel, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonRow.setOpaque(false);
        JButton retry = createButton("[ RETRY ]");
        retry.addActionListener(e -> cardPanel.showScreen(CardPanel.SETUP));
        JButton menu = createButton("[ BACK TO MENU ]");
        menu.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        buttonRow.add(retry);
        buttonRow.add(menu);
        form.add(buttonRow, BorderLayout.SOUTH);

        add(form);
    }

    /**
     * @param result   the finished session, already written to the records file
     * @param restored which of the six capabilities were restored, in stage order
     */
    public void show(SessionResult result, boolean[] restored) {
        String outcome = result.getResult();
        boolean win = outcome.equals("WIN");
        boolean fail = outcome.equals("FAIL");

        titleLabel.setText(win ? "AI CORE: FULLY ACTIVATED" : fail ? "RESTORATION FAILED" : "SESSION CLOSED");
        titleLabel.setForeground(win ? GREEN : fail ? RED : GOLD);

        StringBuilder html = new StringBuilder("<html>");
        boolean corruptedMarked = false;
        for (int i = 0; i < CAPABILITIES.length; i++) {
            String state;
            Color color;
            if (restored[i]) {
                state = "ONLINE";
                color = GREEN;
            } else if (fail && !corruptedMarked) {
                // the stage the player was working on when the run ended
                state = "CORRUPTED";
                color = RED;
                corruptedMarked = true;
            } else {
                state = "OFFLINE";
                color = MUTED;
            }
            line(html, String.format("%-14s %s", CAPABILITIES[i] + ":", state), color);
        }
        html.append("<br>");
        line(html, String.format("%-23s %s", "TRAINER NAME:", result.getTrainerName()), CHALK);
        line(html, String.format("%-23s %d", "FINAL TOKEN SCORE:", result.getFinalScore()), CHALK);
        line(html, String.format("%-23s %s", "HIGHEST CAPABILITY:", result.getHighestCapability().toUpperCase()), CHALK);
        line(html, String.format("%-23s %s", "RESTORATION PROTOCOL:", result.getProtocol().name().replace('_', ' ')), CHALK);
        line(html, String.format("%-23s %s", "GAME RESULT:",
                win ? "RESTORATION COMPLETE" : fail ? "RESTORATION FAILED" : "SESSION RETURNED"), CHALK);
        html.append("<br>");
        line(html, "Filed in the Training Records.", MUTED);
        bodyLabel.setText(html.append("</html>").toString());

        revalidate();
        repaint();
    }

    private static void line(StringBuilder html, String text, Color color) {
        html.append(String.format("<div style='color:#%06x'>", color.getRGB() & 0xFFFFFF))
                .append(text.replace("&", "&amp;").replace("<", "&lt;").replace(" ", "&nbsp;"))
                .append("</div>");
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        button.setForeground(CHALK);
        button.setBackground(new Color(0x28, 0x22, 0x18));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return button;
    }
}
