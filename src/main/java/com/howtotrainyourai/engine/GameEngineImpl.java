package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Real GameEngine implementation. Tracks one session's state and applies the
 * scoring/checkpoint/capability rules from CONTEXT.md 2.
 *
 * Depends on QuestionSource by INTERFACE, not on FakeQuestionSource or
 * CsvQuestionSource by name -- whoever constructs this decides which one to
 * pass in. That's what lets you test against FakeQuestionSource today and
 * a GUI wire up CsvQuestionSource tomorrow without this class changing.
 *
 * Lifelines (CONTEXT.md §2.4): Binary Choice and Predict are aids the
 * player asks for BEFORE answering, via useLifeline(). Override is the only
 * one that can rescue a wrong answer, so it isn't asked for -- the first
 * wrong answer while it's still available spends it automatically and the
 * player retries the same question. A wrong answer with Override gone (or
 * never allowed, as on High Risk) ends the session: end condition (a) in
 * CONTEXT.md §2.5.
 */
public class GameEngineImpl implements GameEngine {

    // Predict names the right answer this often (plan v2's Week 4 tuning note)
    private static final double PREDICT_ACCURACY = 0.7;

    // question number (1-based) that completes a Bloom stage -> capability it
    // restores
    private static final Map<Integer, String> CAPABILITY_UNLOCKS = Map.of(
            3, "Memory",
            5, "Understanding",
            8, "Application",
            10, "Analysis",
            13, "Evaluation",
            15, "Synthesize");

    private final QuestionSource questionSource;
    private final Random random;
    private int tokenTotal;
    private int questionIndex;
    private boolean isRunning;
    private Protocol protocol; // active ruleset for this session
    private List<Question> sessionQuestions; // the 15 questions, in order
    private int securedScore; // tokenTotal to roll back to on failure
    private int securedIndex; // questionIndex to roll back to on failure
    private String trainerName;
    private final Set<Lifeline> remainingLifelines = EnumSet.noneOf(Lifeline.class);

    public GameEngineImpl(QuestionSource questionSource) {
        this(questionSource, new Random());
    }

    /** Lets a test pass a seeded Random so lifeline picks are repeatable. */
    public GameEngineImpl(QuestionSource questionSource, Random random) {
        this.questionSource = questionSource;
        this.random = random;
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
        this.remainingLifelines.clear();
        this.remainingLifelines.addAll(protocol.getAllowedLifelines());
        this.isRunning = true;
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public Set<Lifeline> getRemainingLifelines() {
        return Collections.unmodifiableSet(EnumSet.copyOf(remainingLifelines));
    }

    @Override
    public LifelineResult useLifeline(Lifeline lifeline) {
        requireActiveSession();
        if (lifeline == Lifeline.OVERRIDE) {
            throw new IllegalArgumentException("Override triggers automatically on a wrong answer");
        }
        if (!remainingLifelines.remove(lifeline)) {
            throw new IllegalStateException(lifeline + " is not available");
        }

        Question question = currentQuestion();
        List<String> wrongIds = new ArrayList<>();
        String correctId = null;
        for (Choice choice : question.getChoices()) {
            if (question.isCorrect(choice.getChoiceId())) {
                correctId = choice.getChoiceId();
            } else {
                wrongIds.add(choice.getChoiceId());
            }
        }
        Collections.shuffle(wrongIds, random);

        if (lifeline == Lifeline.BINARY_CHOICE) {
            return LifelineResult.binaryChoice(wrongIds.subList(0, 2));
        }

        // PREDICT: right ~70% of the time, and the stated confidence runs
        // higher when it's right, so players can learn to read the number.
        if (random.nextDouble() < PREDICT_ACCURACY) {
            return LifelineResult.predict(correctId, 65 + random.nextInt(31)); // 65-95
        }
        return LifelineResult.predict(wrongIds.get(0), 35 + random.nextInt(36)); // 35-70
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
        } else if (remainingLifelines.remove(Lifeline.OVERRIDE)) {
            // Override absorbs this miss: nothing lost, nothing advanced,
            // the same question stays current for one more pick.
            return new TurnResult(false, 0, tokenTotal, false, null, false, true);
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
