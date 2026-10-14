package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.QuestionSpeechService;
import com.howtotrainyourai.engine.RecordsStore;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class SettingsPanel extends JPanel {

    // 1970s Mainframe Laboratory CRT Color Palette
    private static final Color CHALK_AMBER = new Color(0xF2, 0xEC, 0xD8);
    private static final Color PHOSPHOR_MUTED = new Color(0x7C, 0x7E, 0x5D);
    private static final Color ACTIVE_GOLD = new Color(0xC2, 0xA6, 0x6E);
    private static final Color CRT_GREEN = new Color(0x55, 0xDE, 0x88);
    private static final Color WARNING_RED = new Color(0xE0, 0x5A, 0x4F);
    private static final Color TV_CHASSIS_BEZEL = new Color(0x24, 0x20, 0x1A);
    private static final Color TV_INNER_GLASS = new Color(0x0A, 0x0E, 0x0B, 246);

    private static final Font TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 24);
    private static final Font SECTION_FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private static final Font SMALL_MONO = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font LOG_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

    private final CardPanel cardPanel;
    private final AudioManager audioManager;
    private final QuestionSpeechService speechService;
    private BufferedImage backgroundImage;
    private Clip humClip;
    private final Random random = new Random();

    // Visual FX Timers (Lab room flicker + TV scanline sweep + Oscilloscope wave)
    private final Timer fxTimer;
    private int bgFlickerAlpha = 20;
    private int waveOffset = 0;
    private int scanlineScroll = 0;

    // Controls
    private JSlider musicSlider;
    private JSlider sfxSlider;
    private JSlider speechSlider;
    private JCheckBox speakQuestions;
    private JLabel musicLabel;
    private JLabel sfxLabel;
    private JLabel speechLabel;
    private JLabel musicValue;
    private JLabel sfxValue;
    private JLabel speechValue;
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

        // Automatically start/stop the electric hum when entering/leaving Settings
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                refreshRecords();
                startHumAudio();
                repaint();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                stopHumAudio();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });

        // Runs at ~15 FPS: delivers genuine CRT hum and vintage laboratory flickering
        fxTimer = new Timer(65, e -> {
            bgFlickerAlpha = random.nextInt(35) + 12;
            waveOffset = (waveOffset + 1) % 40;
            scanlineScroll = (scanlineScroll + 1) % 8;
            repaint();
        });
        fxTimer.start();
    }

    private void startHumAudio() {
        new Thread(() -> {
            try {
                stopHumAudio();
                humClip = SoundManager.loadClip("/audio/electrichum.wav");
                if (humClip != null) {
                    humClip.loop(Clip.LOOP_CONTINUOUSLY);
                    humClip.start();
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void stopHumAudio() {
        if (humClip != null) {
            try {
                if (humClip.isRunning()) {
                    humClip.stop();
                }
                if (humClip.isOpen()) {
                    humClip.close();
                }
            } catch (Exception ignored) {}
            humClip = null;
        }
    }

    private void loadLabImage() {
        String[] attempts = {
            "/images/splashscreen.png",
            "/images/splashscreen.jpg",
            "/images/menuscreen.png"
        };
        for (String path : attempts) {
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    backgroundImage = ImageIO.read(in);
                    if (backgroundImage != null) return;
                }
            } catch (Exception ignored) {}
        }

        String[] folderPaths = {
            "src/main/resources/images",
            "bin/images"
        };
        for (String folder : folderPaths) {
            File f = new File(folder, "splashscreen.png");
            if (f.exists()) {
                try {
                    backgroundImage = ImageIO.read(f);
                    if (backgroundImage != null) return;
                } catch (Exception ignored) {}
            }
        }
    }

    private void buildUI() {
        // ==========================================
        // 1. AUDIO CALIBRATION CONTROLS
        // ==========================================
        musicLabel = createLabel("MUSIC VOLUME:");
        musicSlider = createSlider(0, 100, audioManager == null ? 35 : Math.round(audioManager.getMusicVolume() * 100));
        musicValue = createValueLabel(musicSlider.getValue() + "%");
        musicSlider.addChangeListener(e -> {
            int val = musicSlider.getValue();
            musicValue.setText(val + "%");
            if (audioManager != null) audioManager.setMusicVolume(val / 100f);
        });

        sfxLabel = createLabel("SFX RELAY:");
        sfxSlider = createSlider(0, 100, audioManager == null ? 35 : Math.round(audioManager.getSfxVolume() * 100));
        sfxValue = createValueLabel(sfxSlider.getValue() + "%");
        sfxSlider.addChangeListener(e -> {
            int val = sfxSlider.getValue();
            sfxValue.setText(val + "%");
            if (audioManager != null) audioManager.setSfxVolume(val / 100f);
            SoundManager.setSfxVolume(val / 100f);
        });

        speechLabel = createLabel("SPEECH VOLUME:");
        speechSlider = createSlider(0, 100, speechService == null ? 100 : Math.round(speechService.getVolume() * 100));
        speechValue = createValueLabel(speechSlider.getValue() + "%");
        speechSlider.addChangeListener(e -> {
            int val = speechSlider.getValue();
            speechValue.setText(val + "%");
            if (speechService != null) speechService.setVolume(val / 100f);
        });

        speakQuestions = new JCheckBox("SPEAK QUESTIONS (TTS READOUT)", speechService == null || speechService.isEnabled());
        speakQuestions.setFont(SMALL_MONO);
        speakQuestions.setForeground(CHALK_AMBER);
        speakQuestions.setOpaque(false);
        speakQuestions.setFocusPainted(false);
        speakQuestions.addActionListener(e -> {
            if (speechService != null) speechService.setEnabled(speakQuestions.isSelected());
        });

        // ==========================================
        // 2. DATA MANAGEMENT (ARCHIVE)
        // ==========================================
        viewRecordsBtn = createButton(">> [ RETRIEVE TRAINING LOGS ]");
        viewRecordsBtn.addActionListener(e -> {
            SoundManager.playSound("/audio/flicker.wav", 100);
            refreshRecords();
            showRetroArchiveDialog();
            repaint();
        });

        // ==========================================
        // DOCKED RETURN BUTTON (No click sound on return)
        // ==========================================
        backBtn = createButton("<< [ BACK TO MAIN MENU ] >>");
        backBtn.setFont(MONO_FONT);
        backBtn.addActionListener(e -> {
            stopHumAudio();
            cardPanel.showScreen(CardPanel.MENU);
        });

        add(musicLabel); add(musicSlider); add(musicValue);
        add(sfxLabel); add(sfxSlider); add(sfxValue);
        add(speechLabel); add(speechSlider); add(speechValue);
        add(speakQuestions);
        add(viewRecordsBtn);
        add(backBtn);
    }

    /**
     * Spawns a custom 1974 CRT Magnetic Tape Archive Modal matching the Mainframe Console aesthetic.
     */
    private void showRetroArchiveDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        int dialogW = 820;
        int dialogH = 490;
        dialog.setSize(dialogW, dialogH);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                // Outer CRT chassis rim
                g2.setColor(TV_CHASSIS_BEZEL);
                g2.fillRoundRect(0, 0, w, h, 28, 28);

                // Inner CRT phosphor glass
                g2.setColor(new Color(0x08, 0x0C, 0x09, 252));
                g2.fillRoundRect(6, 6, w - 12, h - 12, 22, 22);

                // Double Brass Border
                g2.setColor(ACTIVE_GOLD);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(6, 6, w - 12, h - 12, 22, 22);
                g2.setColor(new Color(0x60, 0x4D, 0x2C, 170));
                g2.drawRoundRect(11, 11, w - 22, h - 22, 18, 18);

                // CRT Header Banner
                g2.setFont( new Font(Font.MONOSPACED, Font.BOLD, 19));
                g2.setColor(CHALK_AMBER);
                String header = "MAGNETIC TAPE ARCHIVE // RESTORATION BAY LOGS";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(header, (w - fm.stringWidth(header)) / 2, 42);

                // Subheader metadata
                g2.setFont(SMALL_MONO);
                g2.setColor(CRT_GREEN);
                String meta = String.format("TAPE DRIVE: 01-A  |  TOTAL SESSIONS RECORDED: %d  |  STATUS: MOUNTED", records.size());
                FontMetrics fmSub = g2.getFontMetrics();
                g2.drawString(meta, (w - fmSub.stringWidth(meta)) / 2, 64);

                // Divider line
                g2.setColor(PHOSPHOR_MUTED);
                g2.drawLine(30, 76, w - 30, 76);

                // Table Column Header Bar
                int tableX = 30;
                int tableY = 88;
                int tableW = w - 60;
                g2.setColor(new Color(0x14, 0x22, 0x18));
                g2.fillRect(tableX, tableY, tableW, 28);
                g2.setColor(ACTIVE_GOLD);
                g2.drawRect(tableX, tableY, tableW, 28);

                g2.setFont(SMALL_MONO);
                String colHeader = String.format(" %-4s | %-18s | %-11s | %-15s | %-10s | %-8s",
                        "LOG#", "OPERATOR ID", "PEAK TOKENS", "COGNITIVE STAGE", "PROTOCOL", "OUTCOME");
                g2.drawString(colHeader, tableX + 8, tableY + 18);

                // Subtle CRT Scanlines across modal
                g2.setColor(new Color(0, 0, 0, 28));
                for (int y = 8; y < h - 8; y += 4) {
                    g2.drawLine(8, y, w - 8, y);
                }

                g2.dispose();
            }
        };
        root.setOpaque(false);

        // Build custom row list inside scroll pane
        JPanel rowsContainer = new JPanel();
        rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
        rowsContainer.setBackground(new Color(0x06, 0x0A, 0x07));

        if (recordsError != null) {
            rowsContainer.add(createArchiveRowLabel("!! MAGNETIC TAPE READ ERROR: " + recordsError, WARNING_RED, false));
        } else if (records.isEmpty()) {
            rowsContainer.add(createArchiveRowLabel("   [ NO TRAINING SESSIONS RECORDED ON MAGNETIC TAPE ]", PHOSPHOR_MUTED, false));
        } else {
            int logNum = records.size();
            for (int i = records.size() - 1; i >= 0; i--) {
                String[] r = records.get(i);
                String op = r.length > 0 ? fit(r[0], 18) : "--";
                String sc = r.length > 1 ? fit(r[1], 6) + " TK" : "0 TK";
                String fac = r.length > 2 ? fit(r[2], 15) : "None";
                String mode = r.length > 3 ? fit(r[3], 10) : "STANDARD";
                String res = r.length > 4 ? fit(r[4], 8) : "--";

                String formattedRow = String.format(" #%03d | %-18s | %-11s | %-15s | %-10s | %-8s",
                        logNum--, op, sc, fac, mode, res);

                Color rowColor = CHALK_AMBER;
                if ("WIN".equalsIgnoreCase(res.trim())) {
                    rowColor = CRT_GREEN;
                } else if ("FAIL".equalsIgnoreCase(res.trim())) {
                    rowColor = WARNING_RED;
                }

                boolean evenRow = (i % 2 == 0);
                rowsContainer.add(createArchiveRowLabel(formattedRow, rowColor, evenRow));
            }
        }

        JScrollPane scrollPane = new JScrollPane(rowsContainer);
        scrollPane.setBounds(30, 118, dialogW - 60, dialogH - 185);
        scrollPane.setBorder(BorderFactory.createLineBorder(PHOSPHOR_MUTED, 1));
        scrollPane.getViewport().setBackground(new Color(0x06, 0x0A, 0x07));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ACTIVE_GOLD;
                this.trackColor = new Color(0x0E, 0x15, 0x10);
            }
        });
        root.add(scrollPane);

        // Close / Eject Tape Button
        JButton closeBtn = createButton("<< [ EJECT TAPE / RETURN TO CONFIGURATION ] >>");
        closeBtn.setHorizontalAlignment(SwingConstants.CENTER);
        closeBtn.setForeground(ACTIVE_GOLD);
        closeBtn.setBounds((dialogW - 440) / 2, dialogH - 52, 440, 34);
        closeBtn.addActionListener(e -> dialog.dispose());
        root.add(closeBtn);

        // Allow ESC key to close modal
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeArchive");
        root.getActionMap().put("closeArchive", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private JPanel createArchiveRowLabel(String text, Color textColor, boolean alternateBg) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setMaximumSize(new Dimension(760, 28));
        rowPanel.setPreferredSize(new Dimension(740, 28));
        rowPanel.setBackground(alternateBg ? new Color(0x0B, 0x13, 0x0E) : new Color(0x06, 0x0A, 0x07));

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lbl.setForeground(textColor);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        rowPanel.add(lbl, BorderLayout.CENTER);
        return rowPanel;
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
        sl.setForeground(ACTIVE_GOLD);
        sl.setFocusable(false);
        return sl;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MONO_FONT);
        btn.setForeground(PHOSPHOR_MUTED);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(CHALK_AMBER);
            }
            @Override
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

        // Maintain 16:9 aspect box dimensions for responsive sizing
        double targetAspect = 16.0 / 9.0;
        int drawW = w;
        int drawH = (int) (w / targetAspect);
        if (drawH > h) {
            drawH = h;
            drawW = (int) (h * targetAspect);
        }
        int offsetX = (w - drawW) / 2;
        int offsetY = (h - drawH) / 2;

        // CRT TV Frame Coordinate Bounds
        int tvX = offsetX + (int) (drawW * 0.05);
        int tvY = offsetY + (int) (drawH * 0.06);
        int tvW = (int) (drawW * 0.90);
        int tvH = (int) (drawH * 0.88);

        // Column 1: Audio calibration controls
        int col1X = tvX + (int) (tvW * 0.06);
        int labelWidth = 140;
        int sliderWidth = (int) (tvW * 0.28);
        int sliderX = col1X + labelWidth + 10;
        int valueX = sliderX + sliderWidth + 12;
        int rowHeight = 28;

        int row1Y = tvY + (int) (tvH * 0.22);
        int rowGap = (int) (tvH * 0.060);

        musicLabel.setBounds(col1X, row1Y, labelWidth, rowHeight);
        musicSlider.setBounds(sliderX, row1Y, sliderWidth, rowHeight);
        musicValue.setBounds(valueX, row1Y, 60, rowHeight);

        sfxLabel.setBounds(col1X, row1Y + rowGap, labelWidth, rowHeight);
        sfxSlider.setBounds(sliderX, row1Y + rowGap, sliderWidth, rowHeight);
        sfxValue.setBounds(valueX, row1Y + rowGap, 60, rowHeight);

        speechLabel.setBounds(col1X, row1Y + (rowGap * 2), labelWidth, rowHeight);
        speechSlider.setBounds(sliderX, row1Y + (rowGap * 2), sliderWidth, rowHeight);
        speechValue.setBounds(valueX, row1Y + (rowGap * 2), 60, rowHeight);

        speakQuestions.setBounds(col1X, row1Y + (int) (rowGap * 3.1), labelWidth + sliderWidth, rowHeight);

        // Section 2: Data Management button positioned cleanly below the header
        int dataSectionHeaderY = tvY + (int) (tvH * 0.50);
        viewRecordsBtn.setBounds(col1X, dataSectionHeaderY + 22, 330, 32);

        // Return button: docked inside the bottom of the TV frame
        backBtn.setBounds(col1X, tvY + tvH - (int) (tvH * 0.10), 320, 36);
    }

    public void shutdown() {
        fxTimer.stop();
        stopHumAudio();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Render Scaled Laboratory Background
        double targetAspect = 16.0 / 9.0;
        int drawW = w;
        int drawH = (int) (w / targetAspect);
        if (drawH > h) {
            drawH = h;
            drawW = (int) (h * targetAspect);
        }
        int offsetX = (w - drawW) / 2;
        int offsetY = (h - drawH) / 2;

        g2.setColor(new Color(0x06, 0x05, 0x04));
        g2.fillRect(0, 0, w, h);

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, offsetX, offsetY, drawW, drawH, this);
        }

        // 2. Power Brownout Layer (Subtle lab room lighting flicker)
        if (bgFlickerAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(255, bgFlickerAlpha)));
            g2.fillRect(0, 0, w, h);
        }

        // 3. Render Large Television CRT Monitor Chassis & Glass
        int tvX = offsetX + (int) (drawW * 0.05);
        int tvY = offsetY + (int) (drawH * 0.06);
        int tvW = (int) (drawW * 0.90);
        int tvH = (int) (drawH * 0.88);

        // Outer chassis bezel
        g2.setColor(TV_CHASSIS_BEZEL);
        g2.fillRoundRect(tvX - 6, tvY - 6, tvW + 12, tvH + 12, 34, 34);

        // Inner curved dark terminal glass
        g2.setColor(TV_INNER_GLASS);
        g2.fillRoundRect(tvX, tvY, tvW, tvH, 26, 26);

        // Dual brass border frame
        g2.setColor(ACTIVE_GOLD);
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawRoundRect(tvX, tvY, tvW, tvH, 26, 26);
        g2.setColor(new Color(0x60, 0x4D, 0x2C, 160));
        g2.drawRoundRect(tvX + 5, tvY + 5, tvW - 10, tvH - 10, 22, 22);

        // Horizontal scanlines on the TV glass
        g2.setColor(new Color(0, 0, 0, 30));
        for (int y = tvY + 6; y < tvY + tvH - 6; y += 4) {
            if ((y + scanlineScroll) % 8 == 0) {
                g2.drawLine(tvX + 6, y, tvX + tvW - 6, y);
            }
        }

        // TV Header Banner
        g2.setFont(TITLE_FONT);
        g2.setColor(CHALK_AMBER);
        FontMetrics fm = g2.getFontMetrics();
        String title = "MAINFRAME CONFIGURATION // CONSOLE 01";
        g2.drawString(title, tvX + (tvW - fm.stringWidth(title)) / 2, tvY + (int) (tvH * 0.10));

        // Thin divider line under header
        g2.setColor(PHOSPHOR_MUTED);
        g2.drawLine(tvX + 40, tvY + (int) (tvH * 0.125), tvX + tvW - 40, tvY + (int) (tvH * 0.125));

        // Section Headers
        g2.setFont(SECTION_FONT);
        g2.setColor(ACTIVE_GOLD);
        int col1X = tvX + (int) (tvW * 0.06);
        g2.drawString("1. AUDIO CALIBRATION", col1X, tvY + (int) (tvH * 0.19));
        g2.drawString("2. DATA MANAGEMENT", col1X, tvY + (int) (tvH * 0.50));

        // Archive summary box
        int boxX = col1X;
        int boxY = tvY + (int) (tvH * 0.61);
        int boxW = (int) (tvW * 0.42);
        int boxH = (int) (tvH * 0.19);

        g2.setColor(new Color(0x06, 0x0E, 0x08, 220));
        g2.fillRect(boxX, boxY, boxW, boxH);
        g2.setColor(PHOSPHOR_MUTED);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRect(boxX, boxY, boxW, boxH);

        g2.setFont(LOG_FONT);
        g2.setColor(ACTIVE_GOLD);
        String[] recordSummary = getBestRecord();
        g2.drawString(recordsError != null ? "ARCHIVE : READ ERROR" : recordSummary == null ? "ARCHIVE : NO TRAINING RECORDS" :
                "PEAK SCORE : " + recordSummary[1] + " TOKENS", boxX + 16, boxY + 24);
        g2.drawString(recordsError != null ? "OPERATOR   : --" : recordSummary == null ? "OPERATOR   : --" :
                "OPERATOR   : " + fit(recordSummary[0], 20), boxX + 16, boxY + 46);
        g2.setColor(CHALK_AMBER);
        g2.drawString(recordsError != null ? "FACULTY    : CHECK RECORD TAPE" : recordSummary == null ? "FACULTY    : --" :
                "FACULTY    : " + fit(recordSummary[2], 20), boxX + 16, boxY + 68);

        // 4. Oscilloscope Display Screen (Docked neatly on the right side of the TV)
        int oscW = (int) (tvW * 0.35);
        int oscH = (int) (tvH * 0.28);
        int oscX = tvX + tvW - oscW - (int) (tvW * 0.06);
        int oscY = tvY + (int) (tvH * 0.22);

        g2.setColor(new Color(0x04, 0x10, 0x07));
        g2.fillRoundRect(oscX, oscY, oscW, oscH, 14, 14);
        g2.setColor(CRT_GREEN);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(oscX, oscY, oscW, oscH, 14, 14);

        g2.setFont(SMALL_MONO);
        g2.drawString("AI CORE PULSE FREQUENCY", oscX + 14, oscY + 22);

        // Oscilloscope grid lines
        g2.setColor(new Color(0x18, 0x38, 0x22));
        g2.drawLine(oscX + 10, oscY + (oscH / 2), oscX + oscW - 10, oscY + (oscH / 2));
        g2.drawLine(oscX + (oscW / 2), oscY + 28, oscX + (oscW / 2), oscY + oscH - 10);

        // Animated ECG waveform
        g2.setColor(CRT_GREEN);
        g2.setStroke(new BasicStroke(2.2f));
        int[] basePattern = { 0, 0, 0, -4, 22, -30, 36, -8, 0, 0, 0, 0 };
        int step = (oscW - 24) / 12;
        int startPlotX = oscX + 12;
        int midPlotY = oscY + (oscH / 2);

        int px = startPlotX;
        int py = midPlotY + basePattern[waveOffset % basePattern.length];
        for (int i = 1; i < 12; i++) {
            int cx = startPlotX + (i * step);
            int cy = midPlotY + basePattern[(i + waveOffset) % basePattern.length];
            g2.drawLine(px, py, cx, cy);
            px = cx;
            py = cy;
        }

        // 5. Hardware Specification Dossier (Lower right side)
        int infoX = oscX;
        int infoY = oscY + oscH + (int) (tvH * 0.06);
        g2.setFont(SMALL_MONO);
        g2.setColor(PHOSPHOR_MUTED);
        g2.drawString("DEPARTMENT OF PHYSICAL SCIENCES & COMPUTING", infoX, infoY);
        g2.drawString("FACILITY SECTOR : 1974-ALPHA // MAIN TERMINAL", infoX, infoY + 20);
        g2.drawString("POWER LEVEL     : 120V AC NOMINAL // RELAYS SYNCED", infoX, infoY + 40);
        g2.drawString("SYSTEM STATUS   : STANDBY // CALIBRATION ENGAGED", infoX, infoY + 60);

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

    private static String fit(String value, int width) {
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= width ? clean : clean.substring(0, width);
    }
}