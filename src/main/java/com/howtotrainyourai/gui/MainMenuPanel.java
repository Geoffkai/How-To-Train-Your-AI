package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class MainMenuPanel extends JPanel {

    private static final String BG_IMAGE_PATH = "src/main/resources/elements/main_menu.png";

    // DEBUG_ALIGN: set to true temporarily to see button outlines while you
    // fine-tune the percentages below to match your image exactly.
    private static final boolean DEBUG_ALIGN = false;

    private Image bgImage;
    private JButton playButton, tutorialButton, settingsButton, exitButton;

    // Align buttons to the background image
    private static final double[] PLAY_BOUNDS     = {0.235, 0.345, 0.545, 0.125};
    private static final double[] TUTORIAL_BOUNDS = {0.235, 0.478, 0.545, 0.125};
    private static final double[] SETTINGS_BOUNDS = {0.235, 0.610, 0.545, 0.125};
    private static final double[] EXIT_BOUNDS     = {0.235, 0.742, 0.545, 0.125};

    public MainMenuPanel(CardPanel cardPanel) {
        setLayout(null); 
        setBackground(Color.BLACK);
        loadImage();

        playButton = createInvisibleButton();
        tutorialButton = createInvisibleButton();
        settingsButton = createInvisibleButton();
        exitButton = createInvisibleButton();

        playButton.addActionListener(e -> cardPanel.showScreen(CardPanel.PLAY));
        tutorialButton.addActionListener(e -> cardPanel.showScreen(CardPanel.TUTORIAL));
        settingsButton.addActionListener(e -> cardPanel.showScreen(CardPanel.SETTINGS));
        exitButton.addActionListener(e -> System.exit(0));

        add(playButton);
        add(tutorialButton);
        add(settingsButton);
        add(exitButton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
            }
        });
    }

    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setContentAreaFilled(false);
        button.setBorderPainted(DEBUG_ALIGN);
        if (DEBUG_ALIGN) {
            button.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void loadImage() {
        try {
            bgImage = ImageIO.read(new File(BG_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to load main menu image: " + e.getMessage());
        }
    }

    private int[] getImageDrawBounds() {
        int panelW = getWidth();
        int panelH = getHeight();
        if (bgImage == null || panelW == 0 || panelH == 0) {
            return new int[]{0, 0, panelW, panelH};
        }

        int imgW = bgImage.getWidth(this);
        int imgH = bgImage.getHeight(this);
        if (imgW <= 0 || imgH <= 0) {
            return new int[]{0, 0, panelW, panelH};
        }

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

        return new int[]{x, y, drawW, drawH};
    }

    private void layoutButtons() {
        int[] bounds = getImageDrawBounds();
        int imgX = bounds[0], imgY = bounds[1], imgW = bounds[2], imgH = bounds[3];

        placeButton(playButton, PLAY_BOUNDS, imgX, imgY, imgW, imgH);
        placeButton(tutorialButton, TUTORIAL_BOUNDS, imgX, imgY, imgW, imgH);
        placeButton(settingsButton, SETTINGS_BOUNDS, imgX, imgY, imgW, imgH);
        placeButton(exitButton, EXIT_BOUNDS, imgX, imgY, imgW, imgH);
    }

    private void placeButton(JButton button, double[] pct, int imgX, int imgY, int imgW, int imgH) {
        int bx = imgX + (int) (pct[0] * imgW);
        int by = imgY + (int) (pct[1] * imgH);
        int bw = (int) (pct[2] * imgW);
        int bh = (int) (pct[3] * imgH);
        button.setBounds(bx, by, bw, bh);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int[] bounds = getImageDrawBounds();
        g2.drawImage(bgImage, bounds[0], bounds[1], bounds[2], bounds[3], this);

        if (playButton.getWidth() == 0) {
            layoutButtons();
        }
    }
}