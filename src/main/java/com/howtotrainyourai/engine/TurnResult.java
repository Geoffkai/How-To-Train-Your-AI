package com.howtotrainyourai.engine;

/**
 * What happened after one submitted answer. Returned by
 * GameEngine.submitAnswer(choiceId) — this is how the GUI (or
 * TerminalGameLoop) finds out whether to show a green/red banner, award
 * tokens, flip a capability online, or end the session.
 *
 * This is data only, same as Question/Choice — no logic lives here. The
 * logic that decides these values (checkpoints, ladders, protocols) is
 * GameEngineImpl's job, built in Week 2.
 */
public class TurnResult {

    // Whether the submitted choiceId was the correct one.
    private final boolean correct;

    // Tokens gained THIS turn (0 if wrong). Comes from ScoreLadder (Week 2).
    private final int tokensAwarded;

    // Total tokens after this turn — what the GUI's token counter displays.
    private final int runningTotal;

    // True only on the turn that completes a Bloom stage (Q3/5/8/10/13/15 —
    // see CONTEXT.md §2.1). False on every other turn.
    private final boolean capabilityUnlocked;

    // The capability name unlocked THIS turn (e.g. "Memory"), or null if
    // capabilityUnlocked is false. Only meaningful together with the flag
    // above — don't read this without checking capabilityUnlocked first.
    private final String capabilityName;

    // True if this turn ended the session automatically: either the wrong
    // answer with no lifeline left, or a correct answer to Q15 (a win).
    // Does NOT cover the player choosing "Return" — that's a direct call to
    // GameEngine.endSession(), not something submitAnswer() reports.
    private final boolean gameOver;

    /**
     * @param correct            was the submitted choice right?
     * @param tokensAwarded      tokens gained this turn
     * @param runningTotal       total tokens after this turn
     * @param capabilityUnlocked did this turn complete a Bloom stage?
     * @param capabilityName     capability unlocked this turn, or null
     * @param gameOver           did this turn end the session (fail or win)?
     */
    public TurnResult(boolean correct, int tokensAwarded, int runningTotal,
            boolean capabilityUnlocked, String capabilityName, boolean gameOver) {
        this.correct = correct;
        this.tokensAwarded = tokensAwarded;
        this.runningTotal = runningTotal;
        this.capabilityUnlocked = capabilityUnlocked;
        this.capabilityName = capabilityName;
        this.gameOver = gameOver;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getTokensAwarded() {
        return tokensAwarded;
    }

    public int getRunningTotal() {
        return runningTotal;
    }

    public boolean isCapabilityUnlocked() {
        return capabilityUnlocked;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
