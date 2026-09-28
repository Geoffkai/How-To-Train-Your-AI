package com.howtotrainyourai.engine;

/**
 * The token tables for each protocol -- how many tokens a correct answer is
 * worth, by question number.
 *
 * The arrays are private so nothing outside this class can overwrite a
 * ladder value at runtime; callers read through tokensFor() instead.
 * GameEngineImpl decides which protocol and question to ask about, and when.
 */
public final class ScoreLadder {

    public static final int TOTAL_QUESTIONS = 15;

    private static final int[] STANDARD = { 10, 15, 25, 35, 50, 70, 95, 125, 165, 215, 275, 350, 425, 475, 500 };
    private static final int[] HIGH_RISK = { 20, 30, 50, 70, 100, 140, 190, 250, 330, 430, 550, 700, 850, 950, 1000 };

    private ScoreLadder() {
        // static utility class -- not meant to be instantiated
    }

    /**
     * @param protocol       the session's protocol
     * @param questionNumber 1-based question number (1..15)
     * @return tokens a correct answer to that question is worth
     */
    public static int tokensFor(Protocol protocol, int questionNumber) {
        if (questionNumber < 1 || questionNumber > TOTAL_QUESTIONS) {
            throw new IllegalArgumentException("questionNumber must be 1-" + TOTAL_QUESTIONS + ": " + questionNumber);
        }
        int[] ladder = protocol == Protocol.HIGH_RISK ? HIGH_RISK : STANDARD;
        return ladder[questionNumber - 1];
    }
}
