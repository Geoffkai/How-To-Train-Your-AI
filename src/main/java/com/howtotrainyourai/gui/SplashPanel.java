package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class SplashPanel extends JPanel {

    private static final String EMPTY_FRAME_IMAGE_PATH = "src/main/resources/elements/background.png";
    private static final String FULL_SPLASH_IMAGE_PATH = "src/main/resources/elements/splashscreen.png";

    // Timeline (all in ms) - adds up to the total time on screen
    private static final int FADE_IN_MS = 500;      // empty frame fades in from black
    private static final int STAGE1_HOLD_MS = 800;  // empty frame sits alone
    private static final int CROSSFADE_MS = 700;    // empty frame -> full splash
    private static final int STAGE2_HOLD_MS = 1100;  // full splash sits alone
    private static final int FADE_OUT_MS = 500;     // full splash fades to black

    private static final int T0 = FADE_IN_MS;
    private static final int T1 = T0 + STAGE1_HOLD_MS;
    private static final int T2 = T1 + CROSSFADE_MS;
    private static final int T3 = T2 + STAGE2_HOLD_MS;
    private static final int T4 = T3 + FADE_OUT_MS;

    private final CardPanel cardPanel;
    private Image emptyFrameImage;
    private Image fullSplashImage;
    private Timer autoAdvanceTimer;
    private Timer animationTimer;
    private long startTimeMs = 0L;

    public SplashPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setBackground(Color.BLACK);
        loadImages();

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

    private void loadImages() {
        try {
            emptyFrameImage = ImageIO.read(new File(EMPTY_FRAME_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to load empty frame image: " + e.getMessage());
        }
        try {
            fullSplashImage = ImageIO.read(new File(FULL_SPLASH_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Failed to load full splash image: " + e.getMessage());
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
        autoAdvanceTimer = new Timer(T4, e -> goToMenu());
        autoAdvanceTimer.setRepeats(false);
        autoAdvanceTimer.start();
    }

    /** Just drives repaint() so the fade/crossfade timeline renders smoothly. No pulse/scale effects. */
    private void startAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) return;
        animationTimer = new Timer(1000 / 30, e -> repaint());
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

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    /** Empty frame alpha: fades in, holds, fades out during the crossfade. */
    private double computeEmptyFrameAlpha(long elapsed) {
        if (elapsed < T0) {
            return clamp01((double) elapsed / FADE_IN_MS);
        }
        if (elapsed < T1) {
            return 1.0;
        }
        if (elapsed < T2) {
            double progressed = elapsed - T1;
            return clamp01(1.0 - (progressed / CROSSFADE_MS));
        }
        return 0.0;
    }

    /** Full splash alpha: 0 until crossfade starts, ramps up, holds, then fades to black. */
    private double computeFullSplashAlpha(long elapsed) {
        if (elapsed < T1) {
            return 0.0;
        }
        if (elapsed < T2) {
            double progressed = elapsed - T1;
            return clamp01(progressed / CROSSFADE_MS);
        }
        if (elapsed < T3) {
            return 1.0;
        }
        if (elapsed < T4) {
            double progressed = elapsed - T3;
            return clamp01(1.0 - (progressed / FADE_OUT_MS));
        }
        return 0.0;
    }

    private void drawFitted(Graphics2D g2, Image img, int panelW, int panelH, double alpha) {
        if (img == null || alpha <= 0.0) return;

        int imgW = img.getWidth(this);
        int imgH = img.getHeight(this);
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

        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        g2.drawImage(img, x, y, drawW, drawH, this);
        g2.setComposite(original);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int panelW = getWidth();
        int panelH = getHeight();
        long elapsed = System.currentTimeMillis() - startTimeMs;

        double emptyFrameAlpha = computeEmptyFrameAlpha(elapsed);
        double fullSplashAlpha = computeFullSplashAlpha(elapsed);

        drawFitted(g2, emptyFrameImage, panelW, panelH, emptyFrameAlpha);
        drawFitted(g2, fullSplashImage, panelW, panelH, fullSplashAlpha);
    }
}