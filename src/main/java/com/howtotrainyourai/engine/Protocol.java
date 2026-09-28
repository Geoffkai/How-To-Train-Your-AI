package com.howtotrainyourai.engine;

import java.util.List;

/**
 * Which restoration protocol the player picked for this session, and the
 * rules that differ between them (see CONTEXT.md §2.2):
 * - which lifelines the player gets (not just how many -- High Risk allows
 * Binary Choice + Predict, never Override, per the table)
 * - which question number(s) are checkpoints
 * - whether there's a per-question timer
 *
 * (Score multiplier isn't a field here -- ScoreLadder's High Risk table
 * already has the x2 baked into its numbers, so there's nothing for Protocol
 * to apply.)
 *
 * Every field is immutable (List.of), so no caller can change a protocol's
 * rules at runtime -- an enum constant is shared by every session.
 */
public enum Protocol {
    STANDARD(List.of(Lifeline.BINARY_CHOICE, Lifeline.PREDICT, Lifeline.OVERRIDE),
            List.of(5, 10), false),
    HIGH_RISK(List.of(Lifeline.BINARY_CHOICE, Lifeline.PREDICT), List.of(5), true);

    private final List<Lifeline> allowedLifelines;
    private final List<Integer> checkpointQuestions;
    private final boolean hasTimer;

    Protocol(List<Lifeline> allowedLifelines, List<Integer> checkpointQuestions, boolean hasTimer) {
        this.allowedLifelines = allowedLifelines;
        this.checkpointQuestions = checkpointQuestions;
        this.hasTimer = hasTimer;
    }

    public List<Lifeline> getAllowedLifelines() {
        return allowedLifelines;
    }

    /** True if answering this question number (1-based) correctly secures the score. */
    public boolean isCheckpoint(int questionNumber) {
        return checkpointQuestions.contains(questionNumber);
    }

    public boolean hasTimer() {
        return hasTimer;
    }
}
