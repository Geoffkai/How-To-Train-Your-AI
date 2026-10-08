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
        
        // Placeholder exit action for now (we will attach the dialog next)
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
                    // Target the inner face of the button disc
                    int size = (int) (Math.min(w, h) * 0.78);
                    int x = (w - size) / 2;
                    int y = (h - size) / 2;
                    float radius = size / 2.0f;
                    float centerX = x + radius;
                    float centerY = y + radius;

                    // Center is bright vintage gold-white, fading smoothly to transparent near the rim
                    int centerAlpha = getModel().isArmed() ? 160 : 100;
                    int midAlpha    = getModel().isArmed() ? 90  : 50;

                    RadialGradientPaint bulbGlow = new RadialGradientPaint(
                        centerX, centerY, radius,
                        new float[] { 0.0f, 0.55f, 1.0f },
                        new Color[] {
                            new Color(255, 245, 190, centerAlpha), // Warm bulb center
                            new Color(255, 205, 80, midAlpha),     // Amber diffused cap
                            new Color(255, 180, 50, 0)             // Feathered edge into brass
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

        // Repaint immediately on mouse enter/exit so the glow turns on/off instantly
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

        int btnWidth = (int) (w * 0.100);
        int btnHeight = (int) (h * 0.190);
        int btnY = (int) (h * 0.412);
        int centerX = (int) (w * 0.512);
        int spacing = (int) (w * 0.117);

        playButton.setBounds(centerX - (int)(spacing * 1.5) - (btnWidth / 2), btnY, btnWidth, btnHeight);
        tutorialButton.setBounds(centerX - (int)(spacing * 0.5) - (btnWidth / 2), btnY, btnWidth, btnHeight);
        settingsButton.setBounds(centerX + (int)(spacing * 0.5) - (btnWidth / 2), btnY, btnWidth, btnHeight);
        exitButton.setBounds(centerX + (int)(spacing * 1.5) - (btnWidth / 2), btnY, btnWidth, btnHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(0x14, 0x12, 0x0E));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}