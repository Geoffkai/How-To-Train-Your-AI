package com.howtotrainyourai.gui;

import java.awt.*;
import javax.swing.*;

public class CardPanel extends JPanel {

    public static final String MENU = "MENU";
    public static final String PLAY = "PLAY";
    public static final String SETTINGS = "SETTINGS";
    public static final String TUTORIAL = "TUTORIAL";
    public static final String SPLASH = "SPLASH";
    

    private final CardLayout cardLayout;

    public CardPanel() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
    }

    public void addScreen(String name, JPanel panel) {
        add(panel, name);
    }

    public void showScreen(String name) {
        cardLayout.show(this, name);
    }
}