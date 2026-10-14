package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class MainMenuPanel extends JPanel {

    private BufferedImage backgroundImage;
    private final CardPanel cardPanel;

    private final JButton playButton;
    private final JButton tutorialButton;
    private final JButton settingsButton;
    private final JButton exitButton;

    public MainMenuPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);

        loadMenuImage();

        playButton = createCircularHotspot(() -> cardPanel.showScreen(CardPanel.SETUP));
        tutorialButton = createCircularHotspot(() -> cardPanel.showScreen(CardPanel.TUTORIAL));
        settingsButton = createCircularHotspot(() -> cardPanel.showScreen(CardPanel.SETTINGS));

        exitButton = createCircularHotspot(() -> {
            boolean abort = RetroDialog.showConfirm(
                this,
                "CORE-DECOMMISSION-DIR-01",
                "[ DIRECTIVE: MANUAL SYSTEM SHUTDOWN ]",
                "Cut power to the neural learning core?",
                ">> NOTICE: Unsaved memory registers & synaptic weights will be wiped.",
                "CUT CORE POWER",
                "MAINTAIN RUN"
            );

            if (abort) {
                System.exit(0);
            }
        });

        add(playButton);
        add(tutorialButton);
        add(settingsButton);
        add(exitButton);
    }

    private JButton createCircularHotspot(Runnable onClick) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover() || getModel().isArmed()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int size = (int) (Math.min(w, h) * 0.78);
                    int x = (w - size) / 2;
                    int y = (h - size) / 2;
                    float radius = size / 2.0f;
                    float centerX = x + radius;
                    float centerY = y + radius;

                    int centerAlpha = getModel().isArmed() ? 160 : 100;
                    int midAlpha    = getModel().isArmed() ? 90  : 50;

                    RadialGradientPaint bulbGlow = new RadialGradientPaint(
                        centerX, centerY, radius,
                        new float[] { 0.0f, 0.55f, 1.0f },
                        new Color[] {
                            new Color(255, 245, 190, centerAlpha),
                            new Color(255, 205, 80, midAlpha),
                            new Color(255, 180, 50, 0)
                        }
                    );

                    g2.setPaint(bulbGlow);
                    g2.fillOval(x, y, size, size);
                    g2.dispose();
                }
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> onClick.run());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    private void loadMenuImage() {
        String[] classpathAttempts = {
            "/images/menuscreen.png",
            "/menuscreen.png"
        };

        for (String cp : classpathAttempts) {
            try (InputStream in = getClass().getResourceAsStream(cp)) {
                if (in != null) {
                    backgroundImage = ImageIO.read(in);
                    if (backgroundImage != null) return;
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
            File dir = new File(folder);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().equalsIgnoreCase("menuscreen.png")) {
                            try {
                                backgroundImage = ImageIO.read(f);
                                if (backgroundImage != null) return;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Preserve 16:9 bounds for button hitbox placement
        double targetAspect = 16.0 / 9.0;
        int drawW = w;
        int drawH = (int) (w / targetAspect);
        if (drawH > h) {
            drawH = h;
            drawW = (int) (h * targetAspect);
        }
        int offsetX = (w - drawW) / 2;
        int offsetY = (h - drawH) / 2;

        int btnSize = (int) (drawH * 0.205);
        int btnY = offsetY + (int) (drawH * 0.405);
        int centerX = offsetX + (int) (drawW * 0.512);
        int spacing = (int) (drawW * 0.117);

        playButton.setBounds(centerX - (int)(spacing * 1.5) - (btnSize / 2), btnY, btnSize, btnSize);
        tutorialButton.setBounds(centerX - (int)(spacing * 0.5) - (btnSize / 2), btnY, btnSize, btnSize);
        settingsButton.setBounds(centerX + (int)(spacing * 0.5) - (btnSize / 2), btnY, btnSize, btnSize);
        exitButton.setBounds(centerX + (int)(spacing * 1.5) - (btnSize / 2), btnY, btnSize, btnSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        // Letterbox / Pillarbox matte
        g.setColor(new Color(0x0C, 0x0A, 0x09));
        g.fillRect(0, 0, w, h);

        if (backgroundImage != null) {
            double targetAspect = 16.0 / 9.0;
            int drawW = w;
            int drawH = (int) (w / targetAspect);
            if (drawH > h) {
                drawH = h;
                drawW = (int) (h * targetAspect);
            }
            int offsetX = (w - drawW) / 2;
            int offsetY = (h - drawH) / 2;

            g.drawImage(backgroundImage, offsetX, offsetY, drawW, drawH, this);
        }
    }
}