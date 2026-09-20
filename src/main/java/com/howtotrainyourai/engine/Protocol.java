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
 * (Score multiplier isn't a field here -- ScoreLadder.HIGH_RISK already has
 * the x2 baked into its numbers, so there's nothing for Protocol to apply.)
 */
public enum Protocol {
    STANDARD(List.of(Lifeline.BINARY_CHOICE, Lifeline.PREDICT, Lifeline.OVERRIDE),
            new int[] { 5, 10 }, false),
    HIGH_RISK(List.of(Lifeline.BINARY_CHOICE, Lifeline.PREDICT), new int[] { 5 }, true);

    private final List<Lifeline> allowedLifelines;
    private final int[] checkpointQuestion;
    private final boolean hasTimer;

    Protocol(List<Lifeline> allowedLifelines, int[] checkpointQuestion, boolean hasTimer) {
        this.allowedLifelines = allowedLifelines;
        this.checkpointQuestion = checkpointQuestion;
        this.hasTimer = hasTimer;
    }

    public List<Lifeline> getAllowedLifelines() {
        return allowedLifelines;
    }

    public int[] getCheckpointQuestion() {
        return checkpointQuestion;
    }

    public boolean hasTimer() {
        return hasTimer;
    }
}
