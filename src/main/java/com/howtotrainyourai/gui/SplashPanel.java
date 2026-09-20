package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class SplashPanel extends JPanel {

    private static final String IMAGE_PATH = "src/main/resources/elements/splashscreen.png";
    private static final int AUTO_ADVANCE_MS = 3000;
    private static final int FADE_IN_MS = 500;
    private static final int FADE_OUT_MS = 500;

    // Breathing/pulse animation tuning
    private static final int ANIMATION_FPS = 30;
    private static final double PULSE_SPEED = 0.05;   // how fast it breathes
    private static final double PULSE_AMOUNT = 0.02;  // how much it scales (2%)

    private final CardPanel cardPanel;
    private Image splashImage;
    private Timer autoAdvanceTimer;
    private Timer animationTimer;
    private double pulsePhase = 0.0;
    private long startTimeMs = 0L;

    public SplashPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setBackground(Color.BLACK);
        loadImage();

        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goToMenu();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                goToMenu();
            }
        });
    }

    private void loadImage() {
        try {
            splashImage = ImageIO.read(new File(IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to load splash image: " + e.getMessage());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
        startTimeMs = System.currentTimeMillis();
        startAutoAdvance();
        startAnimation();
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        super.removeNotify();
    }

    private void startAutoAdvance() {
        if (autoAdvanceTimer != null && autoAdvanceTimer.isRunning()) return;
        autoAdvanceTimer = new Timer(AUTO_ADVANCE_MS, e -> goToMenu());
        autoAdvanceTimer.setRepeats(false);
        autoAdvanceTimer.start();
    }

    private void startAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) return;
        int delayMs = 1000 / ANIMATION_FPS;
        animationTimer = new Timer(delayMs, e -> {
            pulsePhase += PULSE_SPEED;
            repaint();
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private void goToMenu() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
        stopAnimation();
        cardPanel.showScreen(CardPanel.MENU);
    }

    /** Returns 0.0 (fully faded to black) to 1.0 (fully visible) based on elapsed time. */
    private double computeFadeAlpha() {
        long elapsed = System.currentTimeMillis() - startTimeMs;

        if (elapsed < FADE_IN_MS) {
            return clamp01((double) elapsed / FADE_IN_MS);
        }

        long fadeOutStart = AUTO_ADVANCE_MS - FADE_OUT_MS;
        if (elapsed > fadeOutStart) {
            double remaining = AUTO_ADVANCE_MS - elapsed;
            return clamp01(remaining / FADE_OUT_MS);
        }

        return 1.0;
    }

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (splashImage == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int panelW = getWidth();
        int panelH = getHeight();
        int imgW = splashImage.getWidth(this);
        int imgH = splashImage.getHeight(this);
        if (imgW <= 0 || imgH <= 0) return;

        double imgRatio = (double) imgW / imgH;
        double panelRatio = (double) panelW / panelH;

        int baseDrawW, baseDrawH;
        if (panelRatio > imgRatio) {
            baseDrawH = panelH;
            baseDrawW = (int) (baseDrawH * imgRatio);
        } else {
            baseDrawW = panelW;
            baseDrawH = (int) (baseDrawW / imgRatio);
        }

        // Breathing pulse: gentle sine wave scale around 1.0
        double pulseScale = 1.0 + PULSE_AMOUNT * Math.sin(pulsePhase);
        int drawW = (int) (baseDrawW * pulseScale);
        int drawH = (int) (baseDrawH * pulseScale);

        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        // Fade in/out: applied as image transparency over the black background
        double alpha = computeFadeAlpha();
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));

        g2.drawImage(splashImage, x, y, drawW, drawH, this);

        g2.setComposite(originalComposite);
    }
}