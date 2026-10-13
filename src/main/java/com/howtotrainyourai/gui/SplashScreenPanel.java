package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SplashScreenPanel extends JPanel {

    private static final Color PHOSPHOR_GREEN = new Color(0x73, 0xF5, 0xA3);
    private static final Color PHOSPHOR_BLOOM = new Color(0x28, 0x73, 0x47, 160);
    private static final Color CHALK_LINE = new Color(0xF2, 0xEC, 0xD8);
    private static final Color CHALK_BLOOM = new Color(0x7C, 0x60, 0x27, 140);
    private static final Color SUBTEXT_GREEN = new Color(0x52, 0xB8, 0x7D);

    private final CardPanel cardPanel;
    private BufferedImage backgroundImage;
    private final Timer sequenceTimer;
    private final Random random = new Random();

    private int darknessAlpha = 245;
    private boolean powerStabilized = false;
    private boolean humTriggered = false;
    private int ticks = 0;

    private Clip humClip;

    private final List<String> renderedLines = new ArrayList<>();
    private final String[] bootScript = {
        "[ 120V POWER GRID STABILIZED ]",
        "[ CORE RELAY ONLINE // WEIGHTS MOUNTED ]",
        " ",
        "========================================",
        "HOW TO TRAIN YOUR AI",
        "ANALOG SUPERVISION LAB // VER. 1974",
        "========================================",
        " ",
        ">> SYSTEM OPERATIONAL <<"
    };
    private int scriptIndex = 0;

    public SplashScreenPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setBackground(new Color(0x14, 0x12, 0x0E));
        setFocusable(true);

        loadLabImage();

        // 1. Preload the steady hum in memory
        new Thread(() -> {
            humClip = SoundManager.loadClip("/audio/electrichum.wav");
        }).start();

        // 2. Prolonged flicker sound (runs ~4.5 seconds)
        SoundManager.playSound("/audio/flicker.wav", 4500);

        sequenceTimer = new Timer(50, e -> updateSequence());
        sequenceTimer.start();
    }

    private void loadLabImage() {
        String[] classpathAttempts = {
            "/images/splashscreen.png",
        };

        for (String cp : classpathAttempts) {
            try (InputStream in = getClass().getResourceAsStream(cp)) {
                if (in != null) {
                    backgroundImage = ImageIO.read(in);
                    if (backgroundImage != null) {
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        String[] folderPaths = {
            "bin/images",
            "src/main/resources/images",
            "C:\\Users\\ASUS\\OneDrive\\Desktop\\How-To-Train-Your-AI\\bin\\images",
            "C:\\Users\\ASUS\\OneDrive\\Desktop\\How-To-Train-Your-AI\\src\\main\\resources\\images"
        };

        for (String folder : folderPaths) {
            java.io.File dir = new java.io.File(folder);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] files = dir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.getName().toLowerCase().startsWith("splashscreen")) {
                            try {
                                backgroundImage = ImageIO.read(f);
                                if (backgroundImage != null) {
                                    return;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    private void updateSequence() {
        ticks++;

        // 1. Kick in electrichum at tick 65 (~3.25s) so it catches before flicker ends
        if (ticks >= 65 && !humTriggered) {
            humTriggered = true;
            SoundManager.playPreloaded(humClip, -1);
        }

        // 2. Flicker lights sequence (~3.5 seconds)
        if (!powerStabilized) {
            if (ticks < 20) {
                darknessAlpha = random.nextInt(35) + 220;
            } else if (ticks < 40) {
                darknessAlpha = (ticks % 3 == 0) ? random.nextInt(40) + 70 : random.nextInt(30) + 210;
            } else if (ticks < 55) {
                darknessAlpha = random.nextInt(60) + 110;
            } else if (ticks < 65) {
                darknessAlpha = (ticks % 2 == 0) ? random.nextInt(30) + 40 : random.nextInt(60) + 140;
            } else if (ticks < 70) {
                darknessAlpha = random.nextInt(30) + 20;
            } else {
                darknessAlpha = 0;
                powerStabilized = true;
            }
        } else {
            // Reveal text lines smoothly (every 5 ticks = 250ms)
            if (ticks % 5 == 0 && scriptIndex < bootScript.length) {
                renderedLines.add(bootScript[scriptIndex]);
                scriptIndex++;
            }

            // 3. Transition to menu at tick 135 (~6.75s) right as the audio track finishes naturally
            if (ticks >= 135) {
                advanceToMenu();
            }
        }

        repaint();
    }
    
    private void advanceToMenu() {
        sequenceTimer.stop();
        if (humClip != null && humClip.isOpen()) {
            humClip.stop();
            humClip.close();
        }
        cardPanel.showScreen(CardPanel.MENU);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Scaled lab scene
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
        }

        // 2. Power flicker overlay
        if (darknessAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(255, darknessAlpha)));
            g2.fillRect(0, 0, w, h);
        }

        // 3. Render centered chalkboard title card
        if (powerStabilized) {
            int boardCenterX = (w / 2) + 5;
            int boardTop = (int) (h * 0.370); 
            int lineHeight = Math.max(16, (int) (h * 0.026));

            Font normalFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, (int) (h * 0.017)));
            Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(14, (int) (h * 0.021)));

            for (int i = 0; i < renderedLines.size(); i++) {
                String line = renderedLines.get(i);
                if (line.trim().isEmpty()) continue;

                int yPos = boardTop + (i * lineHeight);
                boolean isTitle = line.equals("HOW TO TRAIN YOUR AI");
                boolean isDivider = line.contains("===");

                g2.setFont(isTitle ? titleFont : normalFont);
                FontMetrics fm = g2.getFontMetrics();
                int xPos = boardCenterX - (fm.stringWidth(line) / 2);

                Color mainColor = (isTitle || isDivider) ? CHALK_LINE : (line.startsWith(">>") ? PHOSPHOR_GREEN : SUBTEXT_GREEN);
                Color bloomColor = (isTitle || isDivider) ? CHALK_BLOOM : PHOSPHOR_BLOOM;

                // Soft phosphor glow pass
                g2.setColor(bloomColor);
                g2.drawString(line, xPos - 1, yPos);
                g2.drawString(line, xPos + 1, yPos);
                g2.drawString(line, xPos, yPos - 1);
                g2.drawString(line, xPos, yPos + 1);

                // Main sharp pass
                g2.setColor(mainColor);
                g2.drawString(line, xPos, yPos);
            }
        }

        g2.dispose();
    }
}