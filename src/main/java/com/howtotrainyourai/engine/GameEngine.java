package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.*;
import java.util.Set;

/**
 * Runs one game session: tracks the current question, scores answers, and
 * knows when the session ends. This is the single seam R3/R4's GUI code
 * builds against — it should never need GameEngineImpl (the real
 * implementation, Week 2) by name, only this interface.
 *
 * Real scoring/checkpoint/protocol logic lives in GameEngineImpl (Week 2).
 * This interface exists now purely so R3/R4 can start wiring screens against
 * it this week, per How_To_Train_Your_AI_Plan_v2.md's Week 1 handoff.
 */
public interface GameEngine {

    /**
     * Starts a new session: builds the 15-question set (via QuestionSource)
     * and resets score/checkpoint state for the chosen protocol.
     */
    void startSession(String trainerName, Protocol protocol);

    /**
     * The question the player is currently answering. Same Question object
     * until submitAnswer() is called and the engine advances.
     */
    Question currentQuestion();

    /**
     * Submits an answer to the current question and advances the session.
     * 
     * @param choiceId the letter ("a".."d") the player chose
     * @return what happened this turn — see TurnResult
     */
    TurnResult submitAnswer(String choiceId);

    /**
     * The protocol the current session was started with.
     */
    Protocol getProtocol();

    /**
     * Lifelines the player can still use this session. Starts as the
     * protocol's allowed set and shrinks as each one is used. Override is
     * never "used" by the player directly -- it's spent automatically the
     * first time they answer wrong (see TurnResult.isRetry()).
     */
    Set<Lifeline> getRemainingLifelines();

    /**
     * Uses Binary Choice or Predict on the current question. Each lifeline
     * works once per session.
     *
     * @throws IllegalStateException    if there's no active session, or the
     *                                  lifeline isn't available any more
     * @throws IllegalArgumentException for OVERRIDE, which triggers on its own
     */
    LifelineResult useLifeline(Lifeline lifeline);

    /**
     * Ends the session (whether from a game-over, a win, or the player
     * choosing "Return") and returns the final summary to record.
     */
    SessionResult endSession();
}
