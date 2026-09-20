package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Question;
import java.util.HashMap;
import java.util.List;

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
        return sessionQuestions.get(questionIndex);
    }

    @Override
    public TurnResult submitAnswer(String choiceId) {
        HashMap<Integer, String> capabilityMap = new HashMap<>();
        capabilityMap.put(3, "Memory");
        capabilityMap.put(5, "Understanding");
        capabilityMap.put(8, "Application");
        capabilityMap.put(10, "Analysis");
        capabilityMap.put(13, "Evaluation");
        capabilityMap.put(15, "Synthesize");
        boolean capabilityUnlocked;
        boolean isGameOver;
        boolean isCorrect;
        String capabilityName;
        int tokensAwarded;

        // TODO 4: this is the core of the engine. Work through it in this
        // order -- write each piece, and consider testing it before moving
        // to the next:
        Question currentQuestion = currentQuestion();

        if (currentQuestion.isCorrect(choiceId)) {
            isCorrect = true;

            tokensAwarded = protocol == Protocol.STANDARD ? ScoreLadder.STANDARD[questionIndex]
                    : ScoreLadder.HIGH_RISK[questionIndex];
            tokenTotal += tokensAwarded;

            if (capabilityMap.containsKey(questionIndex + 1)) {
                capabilityUnlocked = true;
                capabilityName = capabilityMap.get(questionIndex + 1);
            } else {
                capabilityUnlocked = false;
                capabilityName = null;
            }

            for (int index : protocol.getCheckpointQuestion()) {
                if ((questionIndex + 1) == index) {
                    securedScore = tokenTotal;
                    securedIndex = questionIndex;
                    break;
                }
            }

            isGameOver = (questionIndex + 1) == 15;
            questionIndex++;
        } else {
            isCorrect = false;
            tokensAwarded = 0;
            tokenTotal = securedScore;
            capabilityUnlocked = false;
            capabilityName = null;
            isGameOver = true;
        }
        return new TurnResult(isCorrect, tokensAwarded, tokenTotal, capabilityUnlocked, capabilityName, isGameOver);
    }

    @Override
    public SessionResult endSession() {
        isRunning = false;
        return new SessionResult();
    }
}
