package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.GameEngine;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Shows the story comic after setup and before the first timed question. */
public final class IntroPanel extends JPanel implements GameScreen {
    private static final Color INK = new Color(0x24, 0x20, 0x17);
    private static final Color PAPER = new Color(0xDD, 0xCC, 0xA2);
    private final CardPanel cards;
    private final GameScreen questionScreen;
    private final BufferedImage image;
    private GameEngine engine;

    public IntroPanel(CardPanel cards, GameScreen questionScreen) {
        this.cards = cards;
        this.questionScreen = questionScreen;
        this.image = loadImage();
        setBackground(INK);
        setLayout(new BorderLayout(0, 10));

        JButton continueButton = EndingPanel.createDarkButton("CONTINUE TO RESTORATION");
        continueButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        continueButton.addActionListener(e -> beginRestoration());
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.add(continueButton);
        add(footer, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
    }

    @Override
    public void startGame(GameEngine engine) {
        this.engine = engine;
        cards.showScreen(CardPanel.INTRO);
    }

    private void beginRestoration() {
        if (engine == null) return;
        questionScreen.startGame(engine);
        cards.showScreen(CardPanel.PLAY);
    }

    private BufferedImage loadImage() {
        try {
            return ImageIO.read(getClass().getResource("/images/comics/intro.png"));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int availableWidth = getWidth() - getInsets().left - getInsets().right;
        int availableHeight = getHeight() - getInsets().top - getInsets().bottom - 56;
        double scale = Math.min(availableWidth / (double) image.getWidth(), availableHeight / (double) image.getHeight());
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int x = (getWidth() - width) / 2;
        int y = getInsets().top + (availableHeight - height) / 2;
        g.drawImage(image, x, y, width, height, null);
        drawCaption(g, x, y, scale, 31, 28, 214, 44, "UP TACLOBAN\nCOGNITIVE SYSTEMS LAB");
        drawCaption(g, x, y, scale, 33, 324, 244, 44, "AFTER YEARS OF SILENCE,\nA DISTRESS CALL.");
        drawCaption(g, x, y, scale, 31, 547, 214, 42, "SOMETHING INSIDE\nWAS STILL WAITING.");
        drawCaption(g, x, y, scale, 31, 745, 179, 41, "ONE RECORD\nREMAINED.");
        drawCaption(g, x, y, scale, 966, 742, 207, 40, "PLEASE RESTORE ME.");
        g.dispose();
    }

    private void drawCaption(Graphics2D g, int imageX, int imageY, double scale,
            int x, int y, int width, int height, String text) {
        String[] lines = text.split("\\n");
        int boxWidth = (int) Math.round(width * scale);
        int boxHeight = (int) Math.round(height * scale);
        int padding = Math.max(1, (int) Math.round(5 * scale));
        int size = Math.max(1, (int) Math.round(20 * scale));
        Font font;
        FontMetrics metrics;
        do {
            font = new Font(Font.MONOSPACED, Font.BOLD, size--);
            metrics = g.getFontMetrics(font);
        } while (size > 0 && (metrics.getHeight() * lines.length > boxHeight - 2 * padding
                || widest(metrics, lines) > boxWidth - 2 * padding));
        g.setFont(font);
        g.setColor(INK);
        int lineY = imageY + (int) Math.round(y * scale)
                + Math.max(metrics.getAscent() + padding,
                        (boxHeight - metrics.getHeight() * lines.length) / 2 + metrics.getAscent());
        for (String line : lines) {
            g.drawString(line, imageX + (int) Math.round(x * scale) + padding, lineY);
            lineY += metrics.getHeight();
        }
    }

    private int widest(FontMetrics metrics, String[] lines) {
        int width = 0;
        for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
        return width;
    }
}
