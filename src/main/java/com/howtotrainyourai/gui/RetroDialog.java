package com.howtotrainyourai.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RetroDialog extends JDialog {

    private boolean confirmed = false;
    private static JDialog dimmerOverlay = null;

    private RetroDialog(Window parent, String directiveId, String titleText, 
                        String messageText, String subtext, String confirmLabel, String cancelLabel) {
        super(parent, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(540, 310);
        setBackground(new Color(0, 0, 0, 0));

        if (parent != null) {
            Point loc = parent.getLocationOnScreen();
            int x = loc.x + (parent.getWidth() - getWidth()) / 2;
            int y = loc.y + (int) ((parent.getHeight() - getHeight()) * 0.40);
            setLocation(x, Math.max(loc.y + 20, y));
        } else {
            setLocationRelativeTo(null);
        }

        setContentPane(new DossierSheetPanel(directiveId, titleText, messageText, subtext, confirmLabel, cancelLabel));

        // Clean up dimmer when dialog closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                removeDimmer();
            }
        });
    }

    public static boolean showConfirm(Component parent, String directiveId, String title, 
                                      String message, String subtext, String confirmLabel, String cancelLabel) {
        Window win = SwingUtilities.getWindowAncestor(parent);
        showDimmer(win);

        RetroDialog dialog = new RetroDialog(win, directiveId, title, message, subtext, confirmLabel, cancelLabel);
        dialog.setVisible(true);
        removeDimmer();
        return dialog.confirmed;
    }

    // --- Screen Dimmer Backdrop ---
    private static void showDimmer(Window parent) {
        if (parent == null) return;
        dimmerOverlay = new JDialog(parent);
        dimmerOverlay.setUndecorated(true);
        dimmerOverlay.setBounds(parent.getBounds());
        dimmerOverlay.setBackground(new Color(0, 0, 0, 0));

        JPanel shade = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Deep amber-vignette cinematic scrim
                g2.setPaint(new RadialGradientPaint(
                    getWidth() / 2.0f, getHeight() / 2.0f,
                    Math.max(getWidth(), getHeight()) * 0.65f,
                    new float[]{0.0f, 0.7f, 1.0f},
                    new Color[]{
                        new Color(10, 12, 10, 140),
                        new Color(5, 7, 5, 190),
                        new Color(0, 0, 0, 220)
                    }
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        shade.setOpaque(false);
        dimmerOverlay.setContentPane(shade);
        dimmerOverlay.setVisible(true);
    }

    private static void removeDimmer() {
        if (dimmerOverlay != null) {
            dimmerOverlay.dispose();
            dimmerOverlay = null;
        }
    }

    // --- Vintage Laboratory Dossier Rendering ---
    private class DossierSheetPanel extends JPanel {
        private final String directiveId;
        private final String titleText;
        private final String messageText;
        private final String subtext;

        public DossierSheetPanel(String directiveId, String title, String message, 
                                 String subtext, String confirmLabel, String cancelLabel) {
            this.directiveId = directiveId;
            this.titleText = title;
            this.messageText = message;
            this.subtext = subtext;

            setLayout(null);
            setOpaque(false);

            JButton confirmBtn = createMechanicalButton(confirmLabel, true, () -> {
                confirmed = true;
                dispose();
            });
            JButton cancelBtn = createMechanicalButton(cancelLabel, false, () -> {
                confirmed = false;
                dispose();
            });

            confirmBtn.setBounds(50, 236, 210, 44);
            cancelBtn.setBounds(280, 236, 210, 44);

            add(confirmBtn);
            add(cancelBtn);
        }

        private JButton createMechanicalButton(String text, boolean isHazard, Runnable onClick) {
            JButton btn = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    boolean hover = getModel().isRollover();
                    boolean pressed = getModel().isArmed();
                    int w = getWidth();
                    int h = getHeight();
                    int offY = pressed ? 2 : 0;

                    // Heavy mechanical switch bevel
                    g2.setColor(new Color(0x11, 0x0E, 0x0A));
                    g2.fillRoundRect(0, 3, w, h - 3, 6, 6);

                    Color bg = isHazard
                        ? (pressed ? new Color(0x6E, 0x1A, 0x18) : (hover ? new Color(0x9E, 0x2A, 0x24) : new Color(0x82, 0x22, 0x1E)))
                        : (pressed ? new Color(0x3B, 0x33, 0x24) : (hover ? new Color(0x5E, 0x52, 0x38) : new Color(0x4C, 0x41, 0x2C)));

                    g2.setColor(bg);
                    g2.fillRoundRect(0, offY, w, h - 4, 6, 6);

                    // Brass rim
                    Color rim = isHazard
                        ? (hover ? new Color(0xF7, 0x94, 0x8D) : new Color(0xBF, 0x5A, 0x52))
                        : (hover ? new Color(0xFA, 0xDF, 0x9B) : new Color(0xC7, 0xAA, 0x6E));
                    g2.setColor(rim);
                    g2.setStroke(new BasicStroke(hover ? 2.2f : 1.3f));
                    g2.drawRoundRect(0, offY, w - 1, h - 5, 6, 6);

                    // Authentic typewriter font
                    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(getText())) / 2;
                    int ty = (h - 4 + fm.getAscent() - fm.getDescent()) / 2 + offY;

                    g2.setColor(hover ? Color.WHITE : new Color(0xF7, 0xEF, 0xDB));
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }
            };

            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setRolloverEnabled(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> onClick.run());
            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Heavy Wooden Clipboard Base with Drop Shadow
            g2.setColor(new Color(0x14, 0x0F, 0x0B, 180));
            g2.fillRoundRect(6, 10, w - 12, h - 14, 12, 12);

            g2.setColor(new Color(0x32, 0x22, 0x14)); // Hardboard wood
            g2.fillRoundRect(2, 6, w - 4, h - 10, 10, 10);
            g2.setColor(new Color(0x57, 0x3D, 0x25));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(2, 6, w - 5, h - 11, 10, 10);

            // 2. Aged Manila Document Sheet
            int pX = 14, pY = 22, pW = w - 28, pH = h - 34;
            g2.setColor(new Color(0xEE, 0xE4, 0xCB)); // Aged yellowed cardstock
            g2.fillRect(pX, pY, pW, pH);

            // Vintage carbon paper edge aging
            g2.setColor(new Color(0xD6, 0xC6, 0xA2));
            g2.drawRect(pX, pY, pW - 1, pH - 1);
            g2.setColor(new Color(0xBA, 0xA7, 0x7E));
            g2.drawRect(pX + 1, pY + 1, pW - 3, pH - 3);

            // 3. Brass / Steel Heavy Industrial Spring Clamp at Top
            int clipW = 140, clipH = 26;
            int clipX = (w - clipW) / 2;
            g2.setColor(new Color(0x2B, 0x27, 0x22));
            g2.fillRoundRect(clipX, 2, clipW, clipH, 6, 6);
            g2.setColor(new Color(0x9E, 0x86, 0x57)); // Brass edge
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(clipX, 2, clipW, clipH, 6, 6);

            // Clamp bolts
            g2.setColor(new Color(0xE3, 0xD0, 0x96));
            g2.fillOval(clipX + 14, 8, 9, 9);
            g2.fillOval(clipX + clipW - 23, 8, 9, 9);

           // 4. Stamped Confidential Header
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
            g2.setColor(new Color(0x75, 0x68, 0x53));
            g2.drawString("UPT CYBERNETICS // UNIT JPY-170 // " + directiveId, pX + 18, pY + 28);

            // RED RUBBER STAMP: Title
            Graphics2D gStamp = (Graphics2D) g2.create();
            gStamp.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            gStamp.setColor(new Color(0x96, 0x21, 0x1B));
            gStamp.drawString(titleText, pX + 18, pY + 54);

            // Stamped box outline around title
            FontMetrics tfm = gStamp.getFontMetrics();
            int titleWidth = tfm.stringWidth(titleText);
            gStamp.setStroke(new BasicStroke(1.5f));
            gStamp.drawRect(pX + 14, pY + 38, titleWidth + 8, 22);
            gStamp.dispose();

            // Divider rule
            g2.setColor(new Color(0xBE, 0xAE, 0x8E));
            g2.drawLine(pX + 18, pY + 68, pX + pW - 18, pY + 68);

            // 5. Authentic Typewriter Prompt
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g2.setColor(new Color(0x1F, 0x1B, 0x18));
            g2.drawString(messageText, pX + 18, pY + 112);

            // 6. Subtext / Synaptic Warning Note
            if (subtext != null && !subtext.isEmpty()) {
                g2.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 11));
                g2.setColor(new Color(0x6E, 0x51, 0x3E));
                g2.drawString(subtext, pX + 18, pY + 148);
            }

            // Punch card index dots near buttons
            g2.setColor(new Color(0xC7, 0xB6, 0x93));
            for (int dx = pX + 18; dx < pX + pW - 18; dx += 10) {
                g2.fillOval(dx, pY + 192, 3, 3);
            }

            g2.dispose();
        }
    }
}