package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class SplashPanel extends JPanel {

    private static final String EMPTY_FRAME_IMAGE_PATH = "src/main/resources/elements/background.png";
    private static final String FULL_SPLASH_IMAGE_PATH = "src/main/resources/elements/splashscreen.png";

    private static final int FADE_IN_MS = 500;
    private static final int STAGE1_HOLD_MS = 800;
    private static final int WIPE_MS = 700;
    private static final int STAGE2_HOLD_MS = 1000;
    private static final int FADE_OUT_MS = 500;

    private static final int T0 = FADE_IN_MS;
    private static final int T1 = T0 + STAGE1_HOLD_MS;
    private static final int T2 = T1 + WIPE_MS;
    private static final int T3 = T2 + STAGE2_HOLD_MS;
    private static final int T4 = T3 + FADE_OUT_MS; // total duration, also the auto-advance point

    private static final int WIPE_SLANT_PX = 140;

    // Sparkle trail along the wipe edge
    private static final int SPARKLE_COUNT = 16;
    private static final Color SPARKLE_COLOR = new Color(180, 235, 255);

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

    private double computeEmptyFrameAlpha(long elapsed) {
        if (elapsed < T0) {
            return clamp01((double) elapsed / FADE_IN_MS);
        }
        return 1.0;
    }

    private double computeFullSplashAlpha(long elapsed) {
        if (elapsed < T1) {
            return 0.0;
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

    private double computeWipeProgress(long elapsed) {
        if (elapsed < T1) return 0.0;
        if (elapsed >= T2) return 1.0;
        return clamp01((double) (elapsed - T1) / WIPE_MS);
    }

    private void drawFitted(Graphics2D g2, Image img, int panelW, int panelH, double alpha) {
        if (img == null || alpha <= 0.0) return;

        int[] b = fitBounds(img, panelW, panelH);
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        g2.drawImage(img, b[0], b[1], b[2], b[3], this);
        g2.setComposite(original);
    }

    private int[] fitBounds(Image img, int panelW, int panelH) {
        int imgW = img.getWidth(this);
        int imgH = img.getHeight(this);
        if (imgW <= 0 || imgH <= 0) return new int[]{0, 0, panelW, panelH};

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

    private Path2D buildWipeClip(int panelW, int panelH, double progress) {
        double frontX = progress * (panelW + WIPE_SLANT_PX) - WIPE_SLANT_PX;

        double topX = frontX + (WIPE_SLANT_PX / 2.0);
        double bottomX = frontX - (WIPE_SLANT_PX / 2.0);

        Path2D.Double path = new Path2D.Double();
        path.moveTo(-WIPE_SLANT_PX, 0);
        path.lineTo(topX, 0);
        path.lineTo(bottomX, panelH);
        path.lineTo(-WIPE_SLANT_PX, panelH);
        path.closePath();
        return path;
    }

    /** X position of the wipe's diagonal front edge at a given y (0..panelH). */
    private double frontEdgeXAt(int panelW, int panelH, double progress, double y) {
        double frontX = progress * (panelW + WIPE_SLANT_PX) - WIPE_SLANT_PX;
        double topX = frontX + (WIPE_SLANT_PX / 2.0);
        double bottomX = frontX - (WIPE_SLANT_PX / 2.0);
        double t = panelH == 0 ? 0 : y / panelH;
        return topX + (bottomX - topX) * t;
    }

    
    private void drawWipeSparkles(Graphics2D g2, int panelW, int panelH, double progress, long elapsed) {
        if (progress <= 0.0 || progress >= 1.0) return;

        double edgeFade = Math.min(1.0, Math.min(progress * 6.0, (1.0 - progress) * 6.0));
        if (edgeFade <= 0.0) return;

        Composite originalComposite = g2.getComposite();

        for (int i = 0; i < SPARKLE_COUNT; i++) {
            Random rnd = new Random(1000L + i); // stable per-particle properties across frames
            double tAlongLine = rnd.nextDouble();
            double jitter = (rnd.nextDouble() - 0.5) * 26.0;
            double phase = rnd.nextDouble() * Math.PI * 2.0;
            double speed = 0.006 + rnd.nextDouble() * 0.006;
            double baseSize = 3.0 + rnd.nextDouble() * 5.0;

            double y = tAlongLine * panelH;
            double x = frontEdgeXAt(panelW, panelH, progress, y) + jitter;

            double twinkle = 0.35 + 0.65 * Math.abs(Math.sin(elapsed * speed + phase));
            double alpha = clamp01(twinkle * edgeFade);
            if (alpha <= 0.02) continue;

            double size = baseSize * (0.7 + 0.5 * twinkle);

            // Soft glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    Point2D_(x, y), (float) (size * 2.4),
                    new float[]{0f, 1f},
                    new Color[]{
                            withAlpha(SPARKLE_COLOR, alpha * 0.9),
                            withAlpha(SPARKLE_COLOR, 0f)
                    }
            );
            g2.setPaint(glow);
            g2.fillOval((int) (x - size * 2.4), (int) (y - size * 2.4), (int) (size * 4.8), (int) (size * 4.8));

            // Bright core
            g2.setColor(withAlpha(Color.WHITE, alpha));
            g2.fillOval((int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size);
        }

        g2.setComposite(originalComposite);
    }

    private Color withAlpha(Color c, double alpha) {
        int a = (int) Math.round(clamp01(alpha) * 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // Tiny local helper so we don't need an extra import just for Point2D.Double
    private static java.awt.geom.Point2D Point2D_(double x, double y) {
        return new java.awt.geom.Point2D.Double(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();
        long elapsed = System.currentTimeMillis() - startTimeMs;

        double emptyFrameAlpha = computeEmptyFrameAlpha(elapsed);
        double fullSplashAlpha = computeFullSplashAlpha(elapsed);
        double wipeProgress = computeWipeProgress(elapsed);

        drawFitted(g2, emptyFrameImage, panelW, panelH, emptyFrameAlpha);

        if (wipeProgress <= 0.0) {
            return;
        }

        if (wipeProgress >= 1.0) {
            drawFitted(g2, fullSplashImage, panelW, panelH, fullSplashAlpha);
            return;
        }

        Shape originalClip = g2.getClip();
        Path2D wipeClip = buildWipeClip(panelW, panelH, wipeProgress);
        g2.setClip(wipeClip);
        drawFitted(g2, fullSplashImage, panelW, panelH, 1.0);
        g2.setClip(originalClip);

        // Sparkles drawn unclipped so the glow can bleed slightly across the edge
        drawWipeSparkles(g2, panelW, panelH, wipeProgress, elapsed);
    }
}