package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.QuestionSpeechService;
import com.howtotrainyourai.engine.RecordsStore;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
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
    private final AudioManager audioManager;
    private final QuestionSpeechService speechService;
    private BufferedImage backgroundImage;
    private final Random random = new Random();

    // Ambient background flicker & ECG wave timers
    private final Timer fxTimer;
    private int bgFlickerAlpha = 20;
    private int waveOffset = 0;

    // Controls
    private JSlider musicSlider;
    private JSlider sfxSlider;
    private JSlider speechSlider;
    private JCheckBox speakQuestions;
    private JLabel musicValue;
    private JLabel sfxValue;
    private JLabel speechValue;
    private JLabel musicLabel;
    private JLabel sfxLabel;
    private JLabel speechLabel;
    private JButton viewRecordsBtn;
    private JButton backBtn;

    private List<String[]> records = List.of();
    private String recordsError;
    public SettingsPanel(CardPanel cardPanel) {
        this(cardPanel, null, null);
    }

    public SettingsPanel(CardPanel cardPanel, AudioManager audioManager, QuestionSpeechService speechService) {
        this.cardPanel = cardPanel;
        this.audioManager = audioManager;
        this.speechService = speechService;
        setLayout(null);
        setBackground(new Color(0x10, 0x0E, 0x0C));

        loadLabImage();
        refreshRecords();
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
        musicLabel = createLabel("MUSIC VOLUME:");
        musicSlider = createSlider(0, 100,
                audioManager == null ? 70 : Math.round(audioManager.getMusicVolume() * 100));
        add(musicSlider);
        add(musicLabel);
        musicValue = createValueLabel(musicSlider.getValue() + "%");
        musicSlider.addChangeListener(e -> {
            int value = musicSlider.getValue();
            musicValue.setText(value + "%");
            if (audioManager != null) audioManager.setMusicVolume(value / 100f);
        });
        add(musicValue);

        sfxLabel = createLabel("SFX RELAY:");
        sfxSlider = createSlider(0, 100,
                audioManager == null ? 70 : Math.round(audioManager.getSfxVolume() * 100));
        add(sfxSlider);
        add(sfxLabel);
        sfxValue = createValueLabel(sfxSlider.getValue() + "%");
        sfxSlider.addChangeListener(e -> {
            int value = sfxSlider.getValue();
            sfxValue.setText(value + "%");
            if (audioManager != null) audioManager.setSfxVolume(value / 100f);
        });
        add(sfxValue);

        speechLabel = createLabel("SPEECH VOLUME:");
        speechSlider = createSlider(0, 100,
                speechService == null ? 100 : Math.round(speechService.getVolume() * 100));
        add(speechSlider);
        add(speechLabel);
        speechValue = createValueLabel(speechSlider.getValue() + "%");
        speechSlider.addChangeListener(e -> {
            int value = speechSlider.getValue();
            speechValue.setText(value + "%");
            if (speechService != null) speechService.setVolume(value / 100f);
        });
        add(speechValue);

        speakQuestions = new JCheckBox("SPEAK QUESTIONS", speechService == null || speechService.isEnabled());
        speakQuestions.setFont(MONO_FONT);
        speakQuestions.setForeground(CHALK_AMBER);
        speakQuestions.setOpaque(false);
        speakQuestions.setFocusPainted(false);
        speakQuestions.addActionListener(e -> {
            if (speechService != null) speechService.setEnabled(speakQuestions.isSelected());
        });
        add(speakQuestions);

        // ==========================================
        // 2. DATA MANAGEMENT & ARCHIVE
        // ==========================================
        viewRecordsBtn = createButton(">> [ RETRIEVE TRAINING LOGS ]");
        viewRecordsBtn.setBounds(90, 315, 340, 30);
        viewRecordsBtn.addActionListener(e -> {
            SoundManager.playSound("/audio/flicker.wav", 100);
            refreshRecords();
            JTextArea archive = new JTextArea(formatRecords());
            archive.setEditable(false);
            archive.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            archive.setForeground(CHALK_AMBER);
            archive.setBackground(new Color(0x08, 0x0E, 0x0A));
            archive.setCaretPosition(0);
            JScrollPane scroll = new JScrollPane(archive);
            scroll.setPreferredSize(new Dimension(620, 320));
            JOptionPane.showMessageDialog(this, scroll, "RESTORATION TAPE // ARCHIVE",
                    JOptionPane.PLAIN_MESSAGE);
            repaint();
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
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int labelX = 90;
        int labelWidth = 160;
        int sliderX = 260;
        int sliderWidth = Math.min(230, Math.max(150, w - 610));
        int valueX = sliderX + sliderWidth + 15;
        int rowHeight = 26;
        int[] rowY = { 150, 182, 214 };
        JLabel[] labels = { musicLabel, sfxLabel, speechLabel };
        JSlider[] sliders = { musicSlider, sfxSlider, speechSlider };
        JLabel[] values = { musicValue, sfxValue, speechValue };
        for (int i = 0; i < sliders.length; i++) {
            labels[i].setBounds(labelX, rowY[i], labelWidth, rowHeight);
            sliders[i].setBounds(sliderX, rowY[i], sliderWidth, rowHeight);
            values[i].setBounds(valueX, rowY[i], 60, rowHeight);
        }
        speakQuestions.setBounds(labelX, 250, labelWidth + sliderWidth, rowHeight);
        viewRecordsBtn.setBounds(90, 315, 340, 30);
        backBtn.setBounds(70, h - 80, 260, 35);
    }

    public void shutdown() {
        fxTimer.stop();
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
        String[] recordSummary = getBestRecord();
        g2.drawString(recordsError != null ? "ARCHIVE : READ ERROR" : recordSummary == null ? "ARCHIVE : NO TRAINING RECORDS" :
                "PEAK SCORE : " + recordSummary[1] + " TOKENS", boxX + 15, boxY + 25);
        g2.drawString(recordsError != null ? "OPERATOR   : --" : recordSummary == null ? "OPERATOR   : --" :
                "OPERATOR   : " + fit(recordSummary[0], 23), boxX + 15, boxY + 45);
        g2.setColor(CHALK_AMBER);
        g2.drawString(recordsError != null ? "FACULTY    : CHECK TRAINING TAPE" : recordSummary == null ? "FACULTY    : --" :
                "FACULTY    : " + fit(recordSummary[2], 23), boxX + 15, boxY + 65);

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

    private void refreshRecords() {
        try {
            records = RecordsStore.readAll();
            recordsError = null;
        } catch (IOException e) {
            records = List.of();
            recordsError = e.getMessage();
        }
    }

    private String[] getBestRecord() {
        String[] best = null;
        int bestScore = Integer.MIN_VALUE;
        for (String[] record : records) {
            try {
                int score = Integer.parseInt(record[1]);
                if (score > bestScore) {
                    best = record;
                    bestScore = score;
                }
            } catch (NumberFormatException ignored) {}
        }
        return best;
    }

    private String formatRecords() {
        StringBuilder text = new StringBuilder("UP TACLOBAN RESTORATION BAY // SESSION TAPE\n")
                .append("==============================================================\n");
        if (recordsError != null) return text.append("TAPE READ ERROR: ").append(recordsError).append('\n').toString();
        if (records.isEmpty()) return text.append("NO TRAINING RECORDS ON FILE\n").toString();
        int first = Math.max(0, records.size() - 20);
        for (int i = records.size() - 1; i >= first; i--) {
            String[] record = records.get(i);
            text.append(String.format("%s | %s tokens | %s | %s | %s%n",
                    fit(record[0], 18), fit(record[1], 5), fit(record[2], 16),
                    fit(record[3], 9), fit(record[4], 6)));
        }
        if (first > 0) text.append("... earlier sessions remain on tape\n");
        return text.toString();
    }

    private static String fit(String value, int width) {
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= width ? clean : clean.substring(0, width);
    }
}
