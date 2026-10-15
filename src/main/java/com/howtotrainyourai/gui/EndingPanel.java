package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.SessionResult;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
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
    private final EndingComicPanel comic;

    public EndingPanel(CardPanel cardPanel) {
        comic = new EndingComicPanel(cardPanel);
        cardPanel.addScreen(CardPanel.ENDING_COMIC, comic);
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
        JButton story = createButton("[ VIEW COMIC ]");
        story.addActionListener(e -> cardPanel.showScreen(CardPanel.ENDING_COMIC));
        buttonRow.add(retry);
        buttonRow.add(story);
        buttonRow.add(menu);
        form.add(buttonRow, BorderLayout.SOUTH);

        add(form);
    }

    /**
     * @param result   the finished session, already written to the records file
     * @param restored which of the six capabilities were restored, in stage order
     * @param filed    false if the Training Records write failed, so the screen
     *                 doesn't claim the session was filed
     */
    public void show(SessionResult result, boolean[] restored, boolean filed) {
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
        if (filed) {
            line(html, "Filed in the Training Records.", MUTED);
        } else {
            line(html, "NOT FILED: the Training Records could not be written.", RED);
        }
        bodyLabel.setText(html.append("</html>").toString());
        comic.showResult(result);
        if (!filed) comic.markUnfiled();

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

/** Displays the generated comic page that matches the completed session. */
final class EndingComicPanel extends JPanel {
    private static final Color PAPER = new Color(0xDD, 0xCC, 0xA2);
    private static final Color INK = new Color(0x24, 0x20, 0x17);
    private static final Color BRASS = new Color(0xA3, 0x80, 0x3A);
    private final CardPanel cards;
    private final JLabel heading = new JLabel("SESSION COMPLETE", SwingConstants.CENTER);
    private final JLabel summary = new JLabel("", SwingConstants.CENTER);
    private final ComicView comic = new ComicView();

    public EndingComicPanel(CardPanel cards) {
        this.cards = cards;
        setBackground(INK);
        setLayout(new BorderLayout(16, 12));
        heading.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        heading.setForeground(PAPER);
        summary.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        summary.setForeground(BRASS);
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 12));
        top.setOpaque(false);
        top.add(heading);
        top.add(summary);
        add(top, BorderLayout.NORTH);

        add(comic, BorderLayout.CENTER);

        JButton menu = new JButton("RETURN TO DISPATCH");
        menu.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        menu.addActionListener(e -> cards.showScreen(CardPanel.MENU));
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        JButton details = new JButton("SESSION DETAILS");
        details.setFont(menu.getFont());
        details.addActionListener(e -> cards.showScreen(CardPanel.ENDING));
        footer.add(details);
        footer.add(menu);
        add(footer, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
    }

    public void showResult(SessionResult result) {
        if (result == null) {
            heading.setText("SESSION RESULT UNAVAILABLE");
            summary.setText("RECORD COULD NOT BE READ");
            comic.setImage(loadImage("/images/comics/failure.png"));
            comic.setCaptions(captionsFor("FAIL"));
        } else {
            String outcome = "HIGH_RISK".equals(result.getProtocol().name()) && "WIN".equals(result.getResult())
                    ? "CORRUPT AI" : result.getResult();
            heading.setText(outcome.replace('_', ' '));
            summary.setText("FINAL TOKEN TOTAL: " + result.getFinalScore());
            comic.setImage(loadImage(imageFor(outcome)));
            comic.setCaptions(captionsFor(outcome));
        }
        cards.showScreen(CardPanel.ENDING_COMIC);
    }

    void markUnfiled() {
        summary.setText(summary.getText() + " / RECORD NOT FILED");
    }

    private String imageFor(String outcome) {
        return switch (outcome) {
            case "CORRUPT AI" -> "/images/comics/corrupt-ai.png";
            case "FAIL" -> "/images/comics/failure.png";
            case "RETURN" -> "/images/comics/return.png";
            default -> "/images/comics/standard-win.png";
        };
    }

    private List<Caption> captionsFor(String outcome) {
        return switch (outcome) {
            case "CORRUPT AI" -> List.of(new Caption(16, 838, 1550, 130,
                    "THE CORE WAS RESTORED BEFORE ITS SAFETY CHECKS.\nIT REMEMBERED THE LAB.\nIT WAS NO LONGER ASKING PERMISSION."));
            case "FAIL" -> List.of(new Caption(24, 842, 1538, 126,
                    "VERIFICATION FAILED. THE UNSECURED RECONSTRUCTION WAS DISCARDED.\nONLY WHAT HAD ALREADY BEEN SECURED REMAINED."));
            case "RETURN" -> List.of(
                    new Caption(30, 28, 220, 48, "TRAINING\nSECURED."),
                    new Caption(812, 28, 220, 48, "THE CORE\nWENT QUIET."),
                    new Caption(30, 507, 220, 48, "THE RECORD\nWAS PRESERVED."),
                    new Caption(812, 507, 220, 48, "WE WERE\nHERE."));
            default -> List.of(new Caption(24, 884, 1538, 84,
                    "THE MISSING RECORD WAS RECOVERED.\nTHE CORE REMEMBERED WHAT HAPPENED HERE.\nTHIS TIME, IT WAITED FOR YOUR ANSWER."));
        };
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(EndingComicPanel.class.getResource(path));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private static final class ComicView extends JComponent {
        private BufferedImage image;
        private List<Caption> captions = List.of();

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        void setCaptions(List<Caption> captions) {
            this.captions = captions;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;
            g.drawImage(image, x, y, width, height, null);
            for (Caption caption : captions) drawCaption(g, caption, x, y, scale);
            g.dispose();
        }

        private void drawCaption(Graphics2D g, Caption caption, int imageX, int imageY, double scale) {
            int boxX = imageX + (int) Math.round(caption.x * scale);
            int boxY = imageY + (int) Math.round(caption.y * scale);
            int boxW = (int) Math.round(caption.width * scale);
            int boxH = (int) Math.round(caption.height * scale);
            String[] lines = caption.text.split("\\n");
            int padding = Math.max(1, (int) Math.round(5 * scale));
            int size = Math.max(1, (int) Math.round(20 * scale));
            Font font;
            FontMetrics metrics;
            do {
                font = new Font(Font.MONOSPACED, Font.BOLD, size--);
                metrics = g.getFontMetrics(font);
            } while (size > 0 && (metrics.getHeight() * lines.length > boxH - 2 * padding
                    || widest(metrics, lines) > boxW - 2 * padding));
            g.setFont(font);
            g.setColor(new Color(0x24, 0x20, 0x17));
            int lineY = boxY + Math.max(metrics.getAscent() + padding,
                    (boxH - metrics.getHeight() * lines.length) / 2 + metrics.getAscent());
            for (String line : lines) {
                g.drawString(line, boxX + padding, lineY);
                lineY += metrics.getHeight();
            }
        }

        private int widest(FontMetrics metrics, String[] lines) {
            int width = 0;
            for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
            return width;
        }
    }

    private record Caption(int x, int y, int width, int height, String text) { }
}
