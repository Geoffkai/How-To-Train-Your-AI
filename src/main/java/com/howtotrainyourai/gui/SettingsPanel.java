package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SettingsPanel extends JPanel {

    private static final String BG_IMAGE_PATH = "src/main/resources/elements/background.png";
    private static final String BACK_BUTTON_IMAGE_PATH = "src/main/resources/elements/back.png";

    private Image bgImage;

    public SettingsPanel(CardPanel cardPanel) {
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        loadBackground();

        // Center Title Label
        JLabel titleLabel = new JLabel("SETTINGS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 54));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.CENTER);

        // Bottom right container for back button
        JButton backButton = createBackButton(cardPanel);
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 40, 40));
        bottomPanel.setOpaque(false);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadBackground() {
        try {
            bgImage = ImageIO.read(new File(BG_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to load background image: " + e.getMessage());
        }
    }

    private JButton createBackButton(CardPanel cardPanel) {
        JButton backButton = new JButton();

        try {
            BufferedImage fullImg = ImageIO.read(new File(BACK_BUTTON_IMAGE_PATH));
            if (fullImg != null) {
                // Autocrop transparent borders from the large canvas
                BufferedImage cropped = autoCrop(fullImg);

                // Scale cropped graphic to standard button dimensions
                int targetWidth = 220;
                int targetHeight = 75;
                Image scaled = cropped.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                backButton.setIcon(new ImageIcon(scaled));
            }
        } catch (IOException e) {
            System.err.println("Could not load back.png: " + e.getMessage());
            backButton.setText("◀ BACK");
            backButton.setFont(new Font("SansSerif", Font.BOLD, 22));
            backButton.setForeground(Color.CYAN);
        }

        backButton.setOpaque(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));

        return backButton;
    }

    /**
     * Trims transparent pixels surrounding the button artwork
     */
    private BufferedImage autoCrop(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        int top = 0, bottom = height - 1, left = 0, right = width - 1;

        // Scan from top
        topLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (((src.getRGB(x, y) >> 24) & 0xFF) != 0) {
                    top = y;
                    break topLoop;
                }
            }
        }

        // Scan from bottom
        bottomLoop:
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (((src.getRGB(x, y) >> 24) & 0xFF) != 0) {
                    bottom = y;
                    break bottomLoop;
                }
            }
        }

        // Scan from left
        leftLoop:
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (((src.getRGB(x, y) >> 24) & 0xFF) != 0) {
                    left = x;
                    break leftLoop;
                }
            }
        }

        // Scan from right
        rightLoop:
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                if (((src.getRGB(x, y) >> 24) & 0xFF) != 0) {
                    right = x;
                    break rightLoop;
                }
            }
        }

        int cropW = Math.max(1, right - left + 1);
        int cropH = Math.max(1, bottom - top + 1);

        return src.getSubimage(left, top, cropW, cropH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (bgImage == null) return;

        int panelW = getWidth();
        int panelH = getHeight();
        int imgW = bgImage.getWidth(this);
        int imgH = bgImage.getHeight(this);
        if (imgW <= 0 || imgH <= 0) return;

        double imgRatio = (double) imgW / imgH;
        double panelRatio = (double) panelW / panelH;
        int drawW, drawH;
        if (panelRatio > imgRatio) {
            drawH = panelH;
            drawW = (int) (drawH * imgRatio);
        } else {
            drawW = panelW;
            drawH = (int) (drawW / imgRatio);
        }
        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;
        g2.drawImage(bgImage, x, y, drawW, drawH, this);
    }
}