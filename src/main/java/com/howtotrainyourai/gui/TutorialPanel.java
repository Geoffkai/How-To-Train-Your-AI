package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class TutorialPanel extends JPanel {

    private BufferedImage backgroundImage;
    private final CardPanel cardPanel;
    private final JButton backButton;

    public TutorialPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);

        loadTutorialImage();

        backButton = createBackButton();
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        add(backButton);
    }

    private void loadTutorialImage() {
        String[] classpathAttempts = {
            "/images/tutorialscreen.png",
            "/tutorialscreen.png"
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
                        if (f.getName().equalsIgnoreCase("tutorialscreen.png")) {
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

    private JButton createBackButton() {
        JButton btn = new JButton("BACK");
        btn.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        btn.setForeground(new Color(0xED, 0xE3, 0xCD)); // Paper
        btn.setBackground(new Color(0xA9, 0x86, 0x3F)); // Brass
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(0x7C, 0x60, 0x27), 2));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Positioned at the bottom-right desk area, matching the Back button in PlayPanel
        int btnWidth = Math.max(90, (int) (w * 0.08));
        int btnHeight = Math.max(35, (int) (h * 0.05));
        int btnX = (int) (w * 0.88) - btnWidth;
        int btnY = (int) (h * 0.92) - btnHeight;

        backButton.setBounds(btnX, btnY, btnWidth, btnHeight);
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