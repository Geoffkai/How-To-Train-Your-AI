package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class MainMenuPanel extends JPanel {

    private static final boolean DEBUG_SHOW_OUTLINES = false;

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

        playButton = createHotspotButton(() -> cardPanel.showScreen(CardPanel.SETUP));
        tutorialButton = createHotspotButton(() -> cardPanel.showScreen(CardPanel.TUTORIAL));
        settingsButton = createHotspotButton(() -> cardPanel.showScreen(CardPanel.SETTINGS));
        exitButton = createHotspotButton(() -> System.exit(0));

        add(playButton);
        add(tutorialButton);
        add(settingsButton);
        add(exitButton);
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

    private JButton createHotspotButton(Runnable onClick) {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (DEBUG_SHOW_OUTLINES) {
            button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            button.setBorderPainted(true);
        } else {
            button.setBorderPainted(false);
        }

        button.addActionListener(e -> onClick.run());
        return button;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Button dimensions to cover the brass circular plates
        int btnWidth = (int) (w * 0.100);
        int btnHeight = (int) (h * 0.190);

        // Vertical position aligned with the circles
        int btnY = (int) (h * 0.412);

        // Center point across the 4 circles
        int centerX = (int) (w * 0.512);

        // Spacing between buttons
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