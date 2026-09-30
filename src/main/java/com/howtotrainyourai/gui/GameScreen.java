package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.GameEngine;

public interface GameScreen {
    // any screen SetupPanel can hand a live engine to
    void startGame(GameEngine engine);
}
