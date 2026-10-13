package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;

public class SettingsPanel extends JPanel {

    // 1970s Mainframe Color Palette
    private static final Color CHALK_AMBER = new Color(0xF2, 0xEC, 0xD8);
    private static final Color PHOSPHOR_MUTED = new Color(0x7C, 0x7E, 0x5D);
    private static final Color ACTIVE_GOLD = new Color(0xC2, 0xA6, 0x6E);
    private static final Color CRT_PULSE = new Color(0x4A, 0x9E, 0x6B);
    private static final Color TERMINAL_PLATE_BG = new Color(0x0C, 0x0A, 0x09, 248);

    private static final Font TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 28);
    private static final Font SECTION_FONT = new Font(Font.MONOSPACED, Font.BOLD, 18);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);
    private static final Font LOG_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private final CardPanel cardPanel;
    private BufferedImage backgroundImage;
    private final Random random = new Random();

    // Ambient background flicker & ECG wave timers
    private final Timer fxTimer;
    private int bgFlickerAlpha = 20;
    private int waveOffset = 0;

    // Controls
    private JSlider musicSlider;
    private JSlider sfxSlider;
    private JButton viewRecordsBtn;
    private JButton backBtn;

    // Persisted volume values
    private static int musicVolume = 74;
    private static int sfxVolume = 58;

    // Highest record data
    private static final int HIGHEST_SCORE = 1000;
    private static final String TOP_OPERATOR = "MARK ASHER";
    private static final String TOP_PROTOCOL = "HIGH RISK (2x)";
    private static final String TOP_TIER = "SYNTHESIS [ONLINE]";

    public SettingsPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);
        setBackground(new Color(0x10, 0x0E, 0x0C));

        loadLabImage();
        buildUI();

        // Runs at ~12.5 FPS for a low-frequency analog hum flicker + animated wave
        fxTimer = new Timer(80, e -> {
            // Subtle, non-intrusive background brownout flicker
            bgFlickerAlpha = random.nextInt(35) + 10;
            waveOffset = (waveOffset + 1) % 40;
            repaint();
        });
        fxTimer.start();
    }

    private void loadLabImage() {
        String[] paths = {
            "/images/splashscreen.png",
            "/images/splashscreen.jpg"
        };
        for (String path : paths) {
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    backgroundImage = ImageIO.read(in);
                    if (backgroundImage != null) return;
                }
            } catch (Exception ignored) {}
        }
    }

    private void buildUI() {
        // ==========================================
        // 1. AUDIO CALIBRATION
        // ==========================================
        JLabel musicLbl = createLabel("MUSIC VOLUME:");
        musicLbl.setBounds(90, 160, 160, 25);
        add(musicLbl);

        musicSlider = createSlider(0, 100, musicVolume);
        musicSlider.setBounds(260, 160, 230, 25);
        add(musicSlider);

        JLabel musicVal = createValueLabel(musicVolume + "%");
        musicVal.setBounds(505, 160, 60, 25);
        musicSlider.addChangeListener(e -> {
            musicVolume = musicSlider.getValue();
            musicVal.setText(musicVolume + "%");
        });
        add(musicVal);

        JLabel sfxLbl = createLabel("SFX RELAY:");
        sfxLbl.setBounds(90, 210, 160, 25);
        add(sfxLbl);

        sfxSlider = createSlider(0, 100, sfxVolume);
        sfxSlider.setBounds(260, 210, 230, 25);
        add(sfxSlider);

        JLabel sfxVal = createValueLabel(sfxVolume + "%");
        sfxVal.setBounds(505, 210, 60, 25);
        sfxSlider.addChangeListener(e -> {
            sfxVolume = sfxSlider.getValue();
            sfxVal.setText(sfxVolume + "%");
        });
        add(sfxVal);

        // ==========================================
        // 2. DATA MANAGEMENT & ARCHIVE
        // ==========================================
        viewRecordsBtn = createButton(">> [ RETRIEVE TRAINING LOGS ]");
        viewRecordsBtn.setBounds(90, 315, 340, 30);
        viewRecordsBtn.addActionListener(e -> {
            SoundManager.playSound("/audio/flicker.wav", 100);
            JOptionPane.showMessageDialog(
                this,
                "UP TACLOBAN MAINFRAME ARCHIVE // SECTOR 1974\n" +
                "============================================\n" +
                "TOP OPERATOR    : " + TOP_OPERATOR + "\n" +
                "PEAK SCORE      : " + HIGHEST_SCORE + " TOKENS\n" +
                "REACHED STAGE   : " + TOP_TIER + "\n" +
                "ACTIVE PROTOCOL : " + TOP_PROTOCOL + "\n" +
                "============================================\n" +
                "STATUS: CORE RECOVERY VERIFIED",
                "TRAINING ARCHIVE",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        add(viewRecordsBtn);

        // ==========================================
        // RETURN BUTTON (Cleanly docked)
        // ==========================================
        backBtn = createButton("< [ BACK TO MAIN MENU ] >");
        backBtn.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        backBtn.setBounds(70, 520, 250, 35);
        backBtn.addActionListener(e -> {
            SoundManager.playSound("/audio/flicker.wav", 100);
            cardPanel.showScreen(CardPanel.MENU);
        });
        add(backBtn);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(MONO_FONT);
        lbl.setForeground(CHALK_AMBER);
        return lbl;
    }

    private JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(MONO_FONT);
        lbl.setForeground(ACTIVE_GOLD);
        return lbl;
    }

    private JSlider createSlider(int min, int max, int val) {
        JSlider sl = new JSlider(min, max, val);
        sl.setOpaque(false);
        sl.setForeground(CHALK_AMBER);
        sl.setFocusable(false);
        return sl;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MONO_FONT);
        btn.setForeground(PHOSPHOR_MUTED);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(CHALK_AMBER);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(PHOSPHOR_MUTED);
            }
        });
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Lab Room Background
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
        }

        // 2. Background Power Flicker Layer (Atmospheric room flicker)
        if (bgFlickerAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(255, bgFlickerAlpha)));
            g2.fillRect(0, 0, w, h);
        }

        // 3. Central Console Terminal Frame
        int frameX = 50;
        int frameY = 40;
        int frameW = w - 100;
        int frameH = h - 80;

        // Solid readable terminal backplate
        g2.setColor(TERMINAL_PLATE_BG);
        g2.fillRect(frameX, frameY, frameW, frameH);

        // Frame double border
        g2.setColor(CHALK_AMBER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(frameX, frameY, frameW, frameH);
        g2.drawRect(frameX + 4, frameY + 4, frameW - 8, frameH - 8);

        // Header Title
        g2.setFont(TITLE_FONT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("MAINFRAME CONFIGURATION", (w - fm.stringWidth("MAINFRAME CONFIGURATION")) / 2, frameY + 50);

        // Section Headers
        g2.setFont(SECTION_FONT);
        g2.drawString("1. AUDIO CALIBRATION", frameX + 40, frameY + 95);
        g2.drawString("2. DATA MANAGEMENT", frameX + 40, frameY + 250);

        // Log archive data box
        int boxX = frameX + 40;
        int boxY = frameY + 315;
        g2.setColor(new Color(0x15, 0x13, 0x11));
        g2.fillRect(boxX, boxY, 390, 80);
        g2.setColor(PHOSPHOR_MUTED);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(boxX, boxY, 390, 80);

        g2.setFont(LOG_FONT);
        g2.setColor(ACTIVE_GOLD);
        g2.drawString("PEAK SCORE : " + HIGHEST_SCORE + " TOKENS", boxX + 15, boxY + 25);
        g2.drawString("OPERATOR   : " + TOP_OPERATOR, boxX + 15, boxY + 45);
        g2.setColor(CHALK_AMBER);
        g2.drawString("FACULTY    : " + TOP_TIER, boxX + 15, boxY + 65);

        // 4. Animated CRT ECG Oscilloscope (Bottom-Right)
        int ecgW = 160;
        int ecgH = 65;
        int ecgX = frameX + frameW - ecgW - 25;
        int ecgY = frameY + frameH - ecgH - 25;

        g2.setColor(new Color(0x08, 0x0E, 0x0A));
        g2.fillRect(ecgX, ecgY, ecgW, ecgH);
        g2.setColor(CRT_PULSE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(ecgX, ecgY, ecgW, ecgH);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        g2.drawString("AI CORE PULSE", ecgX + 8, ecgY + 14);

        // Animated pulse wave
        int[] basePattern = {32, 32, 32, 30, 46, 12, 54, 30, 32, 32, 32, 32};
        int px = ecgX + 10;
        int py = ecgY + basePattern[(waveOffset) % basePattern.length];

        for (int i = 1; i < 12; i++) {
            int cx = ecgX + 10 + (i * 12);
            int cy = ecgY + basePattern[(i + waveOffset) % basePattern.length];
            g2.drawLine(px, py, cx, cy);
            px = cx;
            py = cy;
        }

        g2.dispose();
    }
}