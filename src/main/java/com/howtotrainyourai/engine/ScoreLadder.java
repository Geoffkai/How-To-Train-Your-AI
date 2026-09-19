package com.howtotrainyourai.engine;

/**
 * The token tables for each protocol -- how many tokens a correct answer is
 * worth, indexed by question number (index 0 = Q1, index 14 = Q15).
 *
 * Pure data, no logic here: GameEngineImpl decides which array to read from
 * (based on the active Protocol) and when.
 */
public final class ScoreLadder {

    private ScoreLadder() {
        // static utility class -- not meant to be instantiated
    }

    public static final int[] STANDARD = { 10, 15, 25, 35, 50, 70, 95, 125, 165, 215, 275, 350, 425, 500 };
    public static final int[] HIGH_RISK = { 20, 30, 50, 70, 100, 140, 190, 250, 330, 430, 550, 700, 850, 950, 1000 };
}
