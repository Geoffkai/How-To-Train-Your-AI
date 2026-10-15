package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.Protocol;
import com.howtotrainyourai.engine.SessionResult;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;

public final class EndingPanelCheck {
    public static void main(String[] args) {
        CardPanel cards = new CardPanel();
        JPanel menu = new JPanel();
        EndingComicPanel ending = new EndingComicPanel(cards);
        cards.addScreen(CardPanel.MENU, menu);
        cards.addScreen(CardPanel.ENDING_COMIC, ending);

        check(cards, menu, ending, new SessionResult("A", 500, "SYNTHESIS", Protocol.STANDARD, "WIN"),
                "WIN", "FINAL TOKEN TOTAL: 500");
        check(cards, menu, ending, new SessionResult("B", 1000, "SYNTHESIS", Protocol.HIGH_RISK, "WIN"),
                "CORRUPT AI", "FINAL TOKEN TOTAL: 1000");
        check(cards, menu, ending, new SessionResult("C", 50, "MEMORY", Protocol.STANDARD, "FAIL"),
                "FAIL", "FINAL TOKEN TOTAL: 50");
        check(cards, menu, ending, new SessionResult("D", 25, "MEMORY", Protocol.STANDARD, "RETURN"),
                "RETURN", "FINAL TOKEN TOTAL: 25");
        cards.remove(ending);
        JPanel setup = new JPanel();
        cards.addScreen(CardPanel.SETUP, setup);
        EndingPanel details = new EndingPanel(cards);
        cards.addScreen(CardPanel.ENDING, details);
        details.show(new SessionResult("E", 50, "MEMORY", Protocol.STANDARD, "FAIL"),
                new boolean[] {true, false, false, false, false, false}, false);
        EndingComicPanel integrated = null;
        for (Component component : cards.getComponents()) {
            if (component instanceof EndingComicPanel panel && panel.isVisible()) integrated = panel;
        }
        assert integrated != null : "Actual session flow must open the comic";
        assert hasText(integrated, "FINAL TOKEN TOTAL: 50 / RECORD NOT FILED");
        button(integrated, "SESSION DETAILS").doClick();
        assert details.isVisible();
        assert containsText(details, "NOT&nbsp;FILED:");
        assert containsText(details, "ONLINE");
        button(details, "[ VIEW COMIC ]").doClick();
        assert integrated.isVisible();
        button(integrated, "SESSION DETAILS").doClick();
        button(details, "[ RETRY ]").doClick();
        assert setup.isVisible();
        System.out.println("EndingPanelCheck passed");
    }

    private static void check(CardPanel cards, JPanel menu, EndingComicPanel ending, SessionResult result,
            String heading, String summary) {
        ending.showResult(result);
        assert ending.isVisible() && !menu.isVisible();
        assert hasText(ending, heading);
        assert hasText(ending, summary);
        cards.showScreen(CardPanel.MENU);
        assert menu.isVisible();
    }

    private static boolean hasText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return true;
            if (component instanceof Container child && hasText(child, text)) return true;
        }
        return false;
    }

    private static boolean containsText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText().contains(text)) return true;
            if (component instanceof Container child && containsText(child, text)) return true;
        }
        return false;
    }

    private static JButton button(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container child) {
                JButton found = button(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
