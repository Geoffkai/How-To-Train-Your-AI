package com.howtotrainyourai.gui;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public class CardPanel extends JPanel {

    public static final String SPLASH = "SPLASH";
    public static final String MENU = "MENU";
    public static final String SETUP = "SETUP";
    public static final String INTRO = "INTRO";
    public static final String PLAY = "PLAY";
    public static final String ENDING = "ENDING";
    public static final String ENDING_COMIC = "ENDING_COMIC";
    public static final String SETTINGS = "SETTINGS";
    public static final String TUTORIAL = "TUTORIAL";

    private final CardLayout cardLayout;
    private Consumer<String> screenListener;

    public CardPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
    }

    public void addScreen(String name, JPanel panel) {
        add(panel, name);
    }

    /** Called with the screen name every time a screen is shown (used to pick the music). */
    public void setScreenListener(Consumer<String> listener) {
        this.screenListener = listener;
    }

    public void showScreen(String name) {
        cardLayout.show(this, name);
        if (screenListener != null) {
            screenListener.accept(name);
        }
    }
}
