package com.howtotrainyourai.gui;

import com.howtotrainyourai.core.GameEngineFactory;
import com.howtotrainyourai.engine.Protocol;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Headless self-check for timed reveal and click-to-skip behavior. */
public final class ScenePanelSweepCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        ScenePanel[] scene = new ScenePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            scene[0] = new ScenePanel(new CardPanel());
            scene[0].setSize(1600, 1000);
            var engine = GameEngineFactory.createDefault();
            engine.startSession("sweep-check", Protocol.STANDARD);
            scene[0].startGame(engine);
        });

        try {
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> {
                if (progress(scene[0]) <= 0 || progress(scene[0]) >= 1) {
                    throw new AssertionError("sweep did not advance");
                }
                JButton answer = null;
                for (Component component : scene[0].getComponents()) {
                    if (component instanceof JButton button && "A".equals(button.getText())) {
                        answer = button;
                        break;
                    }
                }
                if (answer == null) throw new AssertionError("answer key A not found");
                answer.dispatchEvent(new MouseEvent(answer, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
                if (progress(scene[0]) != 1) throw new AssertionError("mouse input did not skip sweep");
                try {
                    var showQuestion = ScenePanel.class.getDeclaredMethod("showQuestion");
                    showQuestion.setAccessible(true);
                    showQuestion.invoke(scene[0]);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
            Thread.sleep(1600);
            SwingUtilities.invokeAndWait(() -> {
                if (progress(scene[0]) != 1) throw new AssertionError("sweep did not finish on its timer");
            });
            System.out.println("scene panel sweep check passed");
        } finally {
            SwingUtilities.invokeAndWait(() -> stopTimers(scene[0]));
        }
    }

    private static double progress(ScenePanel scene) {
        try {
            Field field = ScenePanel.class.getDeclaredField("questionSweepProgress");
            field.setAccessible(true);
            return field.getDouble(scene);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void stopTimers(ScenePanel scene) {
        for (String name : new String[] { "questionSweepTimer", "monitorTimer", "heartRateTimer",
                "idleTimer", "paperFeedTimer", "countdownTimer", "endingTimer" }) {
            try {
                Field field = ScenePanel.class.getDeclaredField(name);
                field.setAccessible(true);
                Timer timer = (Timer) field.get(scene);
                if (timer != null) timer.stop();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }
}
