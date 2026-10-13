package com.howtotrainyourai.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.swing.SwingUtilities;

/** Offscreen check for proportional scene fitting at narrow and wide window sizes. */
public final class ScenePanelResizeCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        ScenePanel[] panel = new ScenePanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new ScenePanel(new CardPanel());
                checkSize(panel[0], 1024, 768, 0, 64, 0.64);
                checkSize(panel[0], 1920, 800, 320, 0, 0.8);
            });
        } finally {
            if (panel[0] != null) SwingUtilities.invokeAndWait(() -> panel[0].shutdown());
        }
        System.out.println("scene panel resize check passed");
    }

    private static void checkSize(ScenePanel panel, int width, int height,
            double expectedX, double expectedY, double expectedScale) {
        panel.setSize(width, height);
        try {
            var relayout = ScenePanel.class.getDeclaredMethod("relayout");
            relayout.setAccessible(true);
            relayout.invoke(panel);
            checkClose("scale", expectedScale, get(panel, "scale"));
            checkClose("x offset", expectedX, get(panel, "offsetX"));
            checkClose("y offset", expectedY, get(panel, "offsetY"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        for (Component component : panel.getComponents()) {
            check(component.getX() >= 0 && component.getY() >= 0
                    && component.getX() + component.getWidth() <= width
                    && component.getY() + component.getHeight() <= height,
                    "control outside resized scene: " + component);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        check(image.getRGB(8, 8) == new Color(0x4f, 0x66, 0x60).getRGB(),
                "wall backdrop should fill the resized scene margins");
        if (expectedY > 0) {
            check(image.getRGB(8, height - 8) == new Color(0x2e, 0x20, 0x13).getRGB(),
                    "wood backdrop should fill the lower scene margin");
        }
    }

    private static double get(ScenePanel panel, String name) throws ReflectiveOperationException {
        Field field = ScenePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(panel);
    }

    private static void checkClose(String name, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
