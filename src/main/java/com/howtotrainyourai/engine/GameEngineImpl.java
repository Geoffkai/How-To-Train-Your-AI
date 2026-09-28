package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Question;
import java.util.List;
import java.util.Map;

/**
 * Real GameEngine implementation. Tracks one session's state and applies the
 * scoring/checkpoint/capability rules from CONTEXT.md §2.
 *
 * Depends on QuestionSource by INTERFACE, not on FakeQuestionSource or
 * CsvQuestionSource by name -- whoever constructs this decides which one to
 * pass in. That's what lets you test against FakeQuestionSource today and
 * a GUI wire up CsvQuestionSource tomorrow without this class changing.
 *
 * ASSUMPTION (revisit later): lifelines aren't wired up yet -- there's no
 * useLifeline()-style method on GameEngine's interface. For now, treat every
 * wrong answer as ending the session (as if the player always has zero
 * lifelines left). That matches end condition (a) in CONTEXT.md §2.5 exactly
 * once lifelines don't exist yet -- expand this once GameEngine grows a real
 * lifeline method.
 */
public class GameEngineImpl implements GameEngine {

    // question number (1-based) that completes a Bloom stage -> capability it restores
    private static final Map<Integer, String> CAPABILITY_UNLOCKS = Map.of(
            3, "Memory",
            5, "Understanding",
            8, "Application",
            10, "Analysis",
            13, "Evaluation",
            15, "Synthesize");

    private final QuestionSource questionSource;
    private int tokenTotal;
    private int questionIndex;
    private boolean isRunning;
    private Protocol protocol; // active ruleset for this session
    private List<Question> sessionQuestions; // the 15 questions, in order
    private int securedScore; // tokenTotal to roll back to on failure
    private int securedIndex; // questionIndex to roll back to on failure
    private String trainerName;

    public GameEngineImpl(QuestionSource questionSource) {
        this.questionSource = questionSource;
    }

    @Override
    public void startSession(String trainerName, Protocol protocol) {
        // Pure state reset -- no printing, no Scanner. This has to be safe
        // to call from a GUI button click, not just a terminal loop, so it
        // does nothing but build data and assign fields.
        this.trainerName = trainerName;
        this.protocol = protocol;
        this.sessionQuestions = questionSource.buildSession();
        this.questionIndex = 0;
        this.tokenTotal = 0;
        this.securedScore = 0;
        this.securedIndex = 0;
        this.isRunning = true;
    }

    @Override
    public Question currentQuestion() {
        requireActiveSession();
        return sessionQuestions.get(questionIndex);
    }

    @Override
    public TurnResult submitAnswer(String choiceId) {
        requireActiveSession();
        boolean capabilityUnlocked;
        boolean isGameOver;
        boolean isCorrect;
        String capabilityName;
        int tokensAwarded;

        Question currentQuestion = currentQuestion();
        int questionNumber = questionIndex + 1;

        if (currentQuestion.isCorrect(choiceId)) {
            isCorrect = true;

            tokensAwarded = ScoreLadder.tokensFor(protocol, questionNumber);
            tokenTotal += tokensAwarded;

            capabilityName = CAPABILITY_UNLOCKS.get(questionNumber);
            capabilityUnlocked = capabilityName != null;

            if (protocol.isCheckpoint(questionNumber)) {
                securedScore = tokenTotal;
                securedIndex = questionIndex;
            }

            isGameOver = questionNumber == ScoreLadder.TOTAL_QUESTIONS;
            questionIndex++;
        } else {
            isCorrect = false;
            tokensAwarded = 0;
            tokenTotal = securedScore;
            capabilityUnlocked = false;
            capabilityName = null;
            isGameOver = true;
        }

        // The session is dead the moment it's over -- both when the player
        // wins Q15 (questionIndex is now 15, past the end of the list) and
        // when a wrong answer ends the run (questionIndex never advanced).
        // Flipping the flag here is what stops a second click from
        // re-scoring the same question or walking off the end.
        if (isGameOver) {
            isRunning = false;
        }

        return new TurnResult(isCorrect, tokensAwarded, tokenTotal, capabilityUnlocked, capabilityName, isGameOver);
    }

    @Override
    public SessionResult endSession() {
        isRunning = false;
        return new SessionResult();
    }

    /**
     * Rejects any call that needs a live session -- before startSession(), or
     * after the session ended (game over, win, or "Return"). A GUI can't be
     * trusted to stop calling on its own: a double-clicked answer button or a
     * stale screen would otherwise crash or corrupt the score. Failing loudly
     * here beats silently re-scoring a finished game.
     */
    private void requireActiveSession() {
        if (!isRunning || sessionQuestions == null) {
            throw new IllegalStateException("no active session");
        }
    }
}
