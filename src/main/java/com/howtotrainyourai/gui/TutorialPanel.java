package com.howtotrainyourai.gui;

import javax.imageio.ImageIO;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Random;

public class TutorialPanel extends JPanel {

    // Grim 1970s Analog Laboratory Palette
    private static final Color CHALK_AMBER = new Color(0xF4, 0xEF, 0xDD);
    private static final Color TERMINAL_GREEN = new Color(0x65, 0xDE, 0x92);
    private static final Color WARNING_RED = new Color(0xDF, 0x4D, 0x42);
    private static final Color GOLD_ACCENT = new Color(0xD8, 0xB6, 0x68);
    private static final Color MUTED_GRAY = new Color(0x8C, 0x88, 0x7B);
    private static final Color SOLID_PLATE = new Color(0x0A, 0x09, 0x08, 244);

    private final CardPanel cardPanel;
    private BufferedImage backgroundImage;
    private Clip storyClip;
    private Clip humClip;
    private final Random random = new Random();

    // Two Distinct States
    private boolean isVoiceoverActive = true;
    private int currentManualPage = 0;

    // Visual FX Timers (Lab room power flicker + CRT scanline scroll + static ticker)
    private final Timer fxTimer;
    private int glitchTicker = 0;
    private int bgFlickerAlpha = 20;
    private int scanlineScroll = 0;

    // Responsive Buttons
    private JButton skipTransmissionBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton backBtn;

    // Phase 2: Operator Manual Pages
    private final String[][][] manualPages = {
        // Page 1: Disaster Background & Role
        {
            {"[ SECTION 01 // CATASTROPHE BRIEFING ]", "TITLE"},
            {"", "SPACE"},
            {"DATE: OCTOBER 1974 // UP TACLOBAN ANALOG COMPUTATION LAB", "GOLD"},
            {"A catastrophic core-voltage surge compromised the analog mainframe.", "BODY"},
            {"The AI unit survived, but its 6 cognitive capabilities were knocked OFFLINE.", "BODY"},
            {"", "SPACE"},
            {"YOUR ROLE AS LEAD OPERATOR:", "GREEN"},
            {"You must train the machine by feeding calibrated questions into the desk console.", "BODY"},
            {"Similar to Who Wants to Be a Millionaire, correctly verifying logic restores", "BODY"},
            {"the AI's internal weights and ascends the Bloom's Taxonomy cognitive ladder.", "BODY"},
            {"", "SPACE"},
            {"Complete all 15 calibration queries to trigger the core awakening sequence.", "ALERT"}
        },
        // Page 2: Bloom's Taxonomy Ladder
        {
            {"[ SECTION 02 // 15-STAGE TAXONOMY LADDER ]", "TITLE"},
            {"", "SPACE"},
            {"Reconstruction follows increasing levels of cognitive complexity:", "BODY"},
            {"  [Q01 - Q03] REMEMBER   -> Restores Core Memory Banks (10 to 25 Tokens)", "GREEN"},
            {"  [Q04 - Q05] UNDERSTAND -> Re-establishes Conceptual Understanding", "GREEN"},
            {"  [Q06 - Q08] APPLY      -> Connects Practical AI Algorithms", "GREEN"},
            {"  [Q09 - Q10] ANALYZE    -> Deconstructs Problem Architectures", "GREEN"},
            {"  [Q11 - Q13] EVALUATE   -> Calibrates Heuristic Critical Judgments", "GREEN"},
            {"  [Q14 - Q15] SYNTHESIS  -> FULL SYSTEM AWAKENING & TRUTH REVEAL", "ALERT"},
            {"", "SPACE"},
            {"Stage 15 unlocks the AI's suppressed memory of what caused the 1974 catastrophe.", "GOLD"}
        },
        // Page 3: Protocols & Lifelines
        {
            {"[ SECTION 03 // PROTOCOLS & ANALOG LIFELINES ]", "TITLE"},
            {"", "SPACE"},
            {"CHOOSE YOUR SUPERVISION PROTOCOL:", "GOLD"},
            {"  STANDARD  : 3 Lifelines | Checkpoints at Q5 & Q10 | Secured token score", "BODY"},
            {"  HIGH RISK : 2 Lifelines | Timer active | 2x Token multiplier | Secret Ending", "ALERT"},
            {"", "SPACE"},
            {"SINGLE-USE ANALOG RELAY LIFELINES:", "GREEN"},
            {"  * BINARY CHOICE : Shunts noisy relays; eliminates 2 wrong answers.", "BODY"},
            {"  * PREDICT       : Queries an early heuristic guess (CAN HALLUCINATE!).", "BODY"},
            {"  * OVERRIDE      : Safety breaker (Standard only) that pardons 1 wrong pick.", "BODY"},
            {"", "SPACE"},
            {"RETURN BUTTON: Click Return anytime during a run to bank your tokens safely.", "GOLD"}
        }
    };

    public TutorialPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);
        setBackground(new Color(0x0E, 0x0C, 0x0B));

        loadLabImage();
        buildUI();

        // Listen for when this panel is shown or hidden
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                stopHumAudio();
                isVoiceoverActive = true;
                currentManualPage = 0;
                relayoutButtons();
                playStoryAudio();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                stopStoryAudio();
                stopHumAudio();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });

        // Animation loop for lab room power flicker, scanlines, & tracking voiceover end
        fxTimer = new Timer(60, e -> {
            glitchTicker++;
            scanlineScroll = (scanlineScroll + 1) % 8;

            // Simulate vintage laboratory fluorescent/power brownout ("on and off" pulse)
            if (random.nextInt(10) == 0) {
                bgFlickerAlpha = random.nextInt(75) + 45; // Brief deeper brownout dip
            } else {
                bgFlickerAlpha = random.nextInt(32) + 10; // Normal subtle hum flicker
            }

            if (isVoiceoverActive && storyClip != null && !storyClip.isRunning() && storyClip.getMicrosecondPosition() > 0) {
                finishTransmission();
            }
            repaint();
        });
        fxTimer.start();
    }

    private void loadLabImage() {
        String[] paths = {
            "/images/splashscreen.png",
            "/images/splashscreen.jpg",
            "/images/menuscreen.png"
        };
        for (String p : paths) {
            try (InputStream in = getClass().getResourceAsStream(p)) {
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

    private void playStoryAudio() {
        new Thread(() -> {
            try {
                stopStoryAudio();
                storyClip = SoundManager.loadClip("/audio/story.wav");
                if (storyClip != null) {
                    storyClip.start();
                } else {
                    finishTransmission();
                }
            } catch (Exception e) {
                finishTransmission();
            }
        }).start();
    }

    private void stopStoryAudio() {
        if (storyClip != null) {
            try {
                if (storyClip.isRunning()) {
                    storyClip.stop();
                }
                if (storyClip.isOpen()) {
                    storyClip.close();
                }
            } catch (Exception ignored) {}
            storyClip = null;
        }
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

    public void shutdown() {
        fxTimer.stop();
        stopStoryAudio();
        stopHumAudio();
    }

    private void finishTransmission() {
        stopStoryAudio();
        if (isVoiceoverActive) {
            isVoiceoverActive = false;
            startHumAudio(); // Start looping electrichum.wav once inside the Facility Manual
        }
        SwingUtilities.invokeLater(() -> {
            relayoutButtons();
            repaint();
        });
    }

    private void buildUI() {
        skipTransmissionBtn = createRetroButton("[ >> SKIP TRANSMISSION & ENTER MANUAL << ]", WARNING_RED, SwingConstants.RIGHT);
        skipTransmissionBtn.addActionListener(e -> {
            SoundManager.playSound("/audio/flicker.wav", 120);
            finishTransmission();
        });
        add(skipTransmissionBtn);

        prevBtn = createRetroButton("< [ PREV PAGE ]", MUTED_GRAY, SwingConstants.RIGHT);
        prevBtn.addActionListener(e -> {
            if (currentManualPage > 0) {
                currentManualPage--;
                SoundManager.playSound("/audio/flicker.wav", 80);
                relayoutButtons();
                repaint();
            }
        });
        add(prevBtn);

        nextBtn = createRetroButton("[ NEXT PAGE ] >", GOLD_ACCENT, SwingConstants.RIGHT);
        nextBtn.addActionListener(e -> {
            if (currentManualPage < manualPages.length - 1) {
                currentManualPage++;
                SoundManager.playSound("/audio/flicker.wav", 80);
                relayoutButtons();
                repaint();
            }
        });
        add(nextBtn);

        backBtn = createRetroButton("<< [ RETURN TO CONSOLE ]", CHALK_AMBER, SwingConstants.LEFT);
        backBtn.addActionListener(e -> {
            stopStoryAudio();
            stopHumAudio();
            cardPanel.showScreen(CardPanel.MENU);
        });
        add(backBtn);
    }

    private void relayoutButtons() {
        int w = getWidth() > 0 ? getWidth() : 1280;
        int h = getHeight() > 0 ? getHeight() : 720;

        int frameX = 50;
        int frameY = 35;
        int frameW = w - 100;
        int frameH = h - 70;
        int btnY = frameY + frameH - 52;

        if (isVoiceoverActive) {
            skipTransmissionBtn.setVisible(true);
            prevBtn.setVisible(false);
            nextBtn.setVisible(false);
            backBtn.setVisible(true);

            backBtn.setBounds(frameX + 30, btnY, 300, 36);
            skipTransmissionBtn.setBounds(frameX + frameW - 480, btnY, 450, 36);
        } else {
            skipTransmissionBtn.setVisible(false);
            backBtn.setVisible(true);
            prevBtn.setVisible(currentManualPage > 0);
            nextBtn.setVisible(currentManualPage < manualPages.length - 1);

            backBtn.setBounds(frameX + 30, btnY, 300, 36);
            prevBtn.setBounds(frameX + frameW - 450, btnY, 200, 36);
            nextBtn.setBounds(frameX + frameW - 230, btnY, 200, 36);
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        relayoutButtons();
    }

    private JButton createRetroButton(String text, Color baseColor, int alignment) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        btn.setForeground(baseColor);
        btn.setHorizontalAlignment(alignment);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(baseColor);
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

        // 1. Lab Background (16:9 aspect preserved)
        g2.setColor(new Color(0x08, 0x07, 0x06));
        g2.fillRect(0, 0, w, h);

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
            g2.drawImage(backgroundImage, offsetX, offsetY, drawW, drawH, this);
        }

        // 2. Laboratory Room "On and Off" Power Flicker Overlay
        if (bgFlickerAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(255, bgFlickerAlpha)));
            g2.fillRect(0, 0, w, h);
        }

        int frameX = 50;
        int frameY = 35;
        int frameW = w - 100;
        int frameH = h - 70;

        // 3. High-contrast Slate Terminal Plate
        g2.setColor(SOLID_PLATE);
        g2.fillRect(frameX, frameY, frameW, frameH);

        // Double Border
        g2.setColor(isVoiceoverActive ? WARNING_RED : CHALK_AMBER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(frameX, frameY, frameW, frameH);
        g2.drawRect(frameX + 4, frameY + 4, frameW - 8, frameH - 8);

        if (isVoiceoverActive) {
            renderEmergencyTransmission(g2, frameX, frameY, frameW, frameH);
        } else {
            renderCleanManualPage(g2, frameX, frameY, frameW, frameH);
        }

        g2.dispose();
    }

    private void renderEmergencyTransmission(Graphics2D g2, int fx, int fy, int fw, int fh) {
        g2.setColor(new Color(255, 60, 60, 15 + (random.nextInt(20))));
        for (int y = fy; y < fy + fh; y += 4) {
            if ((y + glitchTicker) % 8 == 0) {
                g2.drawLine(fx, y, fx + fw, y);
            }
        }

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g2.setColor(WARNING_RED);
        g2.drawString("! INCOMING DISTRESS TRANSMISSION [AUDIO PLAYBACK] !", fx + 35, fy + 45);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g2.setColor(MUTED_GRAY);
        g2.drawString("CHANNEL: 1974-VHF // SIGNAL INTEGRITY: 41% // STATUS: DECRYPTING...", fx + 35, fy + 72);

        int waveBoxX = fx + 35;
        int waveBoxY = fy + 95;
        int waveBoxW = fw - 70;
        int waveBoxH = 65;

        g2.setColor(new Color(0x18, 0x08, 0x08));
        g2.fillRect(waveBoxX, waveBoxY, waveBoxW, waveBoxH);
        g2.setColor(WARNING_RED);
        g2.drawRect(waveBoxX, waveBoxY, waveBoxW, waveBoxH);

        g2.setStroke(new BasicStroke(1.5f));
        int prevPx = waveBoxX + 10;
        int prevPy = waveBoxY + (waveBoxH / 2);
        for (int x = waveBoxX + 15; x < waveBoxX + waveBoxW - 10; x += 12) {
            int spike = (random.nextInt(30) - 15);
            int currPy = waveBoxY + (waveBoxH / 2) + spike;
            g2.drawLine(prevPx, prevPy, x, currPy);
            prevPx = x;
            prevPy = currPy;
        }

        int textY = waveBoxY + waveBoxH + 45;
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        g2.setColor(CHALK_AMBER);
        g2.drawString(">> AUDIO LOG 'story.wav' STREAMING INTO OPERATOR CONSOLE...", fx + 35, textY);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        g2.setColor(GOLD_ACCENT);
        g2.drawString("Listen carefully to the voice dispatch to understand the facility disaster.", fx + 35, textY + 30);
        g2.drawString("Once playback concludes—or if you hit SKIP—the full operator manual", fx + 35, textY + 55);
        g2.drawString("and system rules will decode cleanly below.", fx + 35, textY + 80);

        g2.setColor(WARNING_RED);
        g2.drawString("CRITICAL: PREPARE TO RESTORE CORRUPTED COGNITIVE CAPABILITIES.", fx + 35, textY + 125);
    }

    private void renderCleanManualPage(Graphics2D g2, int fx, int fy, int fw, int fh) {
        // Subtle animated CRT scanlines across the manual terminal
        g2.setColor(new Color(0, 0, 0, 32));
        for (int y = fy + 6; y < fy + fh - 6; y += 4) {
            if ((y + scanlineScroll) % 8 == 0) {
                g2.drawLine(fx + 6, y, fx + fw - 6, y);
            }
        }

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g2.setColor(MUTED_GRAY);
        g2.drawString("FACILITY MANUAL // UP TACLOBAN AI CORE", fx + 30, fy + 32);

        String pageTracker = String.format("PAGE %d OF %d", currentManualPage + 1, manualPages.length);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(pageTracker, fx + fw - fm.stringWidth(pageTracker) - 30, fy + 32);

        g2.setColor(MUTED_GRAY);
        g2.drawLine(fx + 30, fy + 42, fx + fw - 30, fy + 42);

        String[][] lines = manualPages[currentManualPage];
        int startY = fy + 75;
        int lineHeight = 24;

        Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 18);
        Font bodyFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);

        for (int i = 0; i < lines.length; i++) {
            String txt = lines[i][0];
            String type = lines[i][1];
            int yPos = startY + (i * lineHeight);

            if (type.equals("SPACE")) continue;

            if (type.equals("TITLE")) {
                g2.setFont(titleFont);
                g2.setColor(CHALK_AMBER);
            } else if (type.equals("GREEN")) {
                g2.setFont(bodyFont);
                g2.setColor(TERMINAL_GREEN);
            } else if (type.equals("GOLD")) {
                g2.setFont(bodyFont);
                g2.setColor(GOLD_ACCENT);
            } else if (type.equals("ALERT")) {
                g2.setFont(bodyFont);
                g2.setColor(WARNING_RED);
            } else {
                g2.setFont(bodyFont);
                g2.setColor(CHALK_AMBER);
            }

            g2.drawString(txt, fx + 30, yPos);
        }
    }
}