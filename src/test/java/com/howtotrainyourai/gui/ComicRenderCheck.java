package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.Protocol;
import com.howtotrainyourai.engine.SessionResult;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Renders the actual Swing screens at three sizes for caption/layout inspection. */
public final class ComicRenderCheck {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                CardPanel cards = new CardPanel();
                IntroPanel intro = new IntroPanel(cards, engine -> { });
                EndingPanel details = new EndingPanel(cards);
                EndingComicPanel ending = (EndingComicPanel) cards.getComponent(0);
                cards.addScreen(CardPanel.INTRO, intro);
                cards.addScreen(CardPanel.ENDING, details);
                for (int[] size : new int[][] {{1280, 800}, {960, 600}, {1920, 1080}}) {
                    render(intro, output, "01-intro", size);
                    String[] outcomes = {"WIN", "WIN", "FAIL", "RETURN"};
                    String[] names = {"02-standard-win", "03-corrupt-ai", "04-failure", "05-return"};
                    for (int i = 0; i < outcomes.length; i++) {
                        details.show(new SessionResult("CHECK", 500, "SYNTHESIS",
                                i == 1 ? Protocol.HIGH_RISK : Protocol.STANDARD, outcomes[i]),
                                new boolean[] {true, true, true, true, true, true}, true);
                        render(ending, output, names[i], size);
                    }
                    cards.showScreen(CardPanel.ENDING);
                    render(details, output, "06-session-details", size);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("ComicRenderCheck passed: " + output);
    }

    private static void render(JPanel panel, Path output, String name, int[] size) throws Exception {
        panel.setSize(size[0], size[1]);
        layout(panel);
        BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        assert image.getRGB(size[0] / 2, size[1] / 2) != image.getRGB(0, 0)
                : "Missing comic image: " + name;
        ImageIO.write(image, "png", output.resolve(name + "-" + size[0] + "x" + size[1] + ".png").toFile());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) layout(child);
        }
    }
}
