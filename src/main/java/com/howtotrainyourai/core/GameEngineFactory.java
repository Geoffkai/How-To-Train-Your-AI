package com.howtotrainyourai.core;

import com.howtotrainyourai.data.CsvQuestionSource;
import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.GameEngineImpl;

/**
 * Builds a ready-to-use GameEngine backed by the real question bank.
 *
 * This is the single place that decides WHICH QuestionSource the game runs on.
 * Screens call createDefault() and never mention CsvQuestionSource, so the
 * GUI has no compile-time dependency on the data layer at all.
 *
 * Lives in `core` rather than `engine` on purpose: `data` already imports
 * `engine.QuestionSource`, so putting this in `engine` would make the two
 * packages import each other. `core` sits above both and can depend on each.
 */
public final class GameEngineFactory {

    private GameEngineFactory() {
        // static factory -- not meant to be instantiated
    }

    /**
     * A new engine reading the real CSV question bank. The bank is loaded here,
     * so this throws if it's missing or malformed -- callers should catch
     * RuntimeException and tell the player rather than dying on a dialog-less
     * stack trace.
     */
    public static GameEngine createDefault() {
        return new GameEngineImpl(new CsvQuestionSource());
    }
}
