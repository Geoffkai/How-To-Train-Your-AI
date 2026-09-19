package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Question;
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

    // TODO 1: add the fields you need to track one session's state. At
    // minimum you'll need something for:
    //  - the active Protocol (set in startSession)
    //  - the 15 questions for this session (set in startSession)
    //  - which question index the player is currently on
    //  - the running token total
    //  - the SECURED score and SECURED question index -- i.e. what to roll
    //    back to on failure (see CONTEXT.md §2.2: "checkpoints secure the
    //    token score and capability progress reached so far")
    //  - whether the session has already ended
    //
    // Think about what a brand-new engine (before startSession is ever
    // called) should have in these fields, since currentQuestion() /
    // submitAnswer() need to behave sanely if called too early.

    public GameEngineImpl(QuestionSource questionSource) {
        this.questionSource = questionSource;
    }

    @Override
    public void startSession(String trainerName, Protocol protocol) {
        // TODO 2: build the session via questionSource.buildSession() and
        // reset ALL of the state fields from TODO 1 to their starting
        // values for a fresh game (index 0, running total 0, nothing
        // secured yet, not over).
    }

    @Override
    public Question currentQuestion() {
        // TODO 3: return the question at the current index.
        return null; // TODO: replace with the real question
    }

    @Override
    public TurnResult submitAnswer(String choiceId) {
        // TODO 4: this is the core of the engine. Work through it in this
        // order -- write each piece, and consider testing it before moving
        // to the next:
        //
        //  a) Ask the current question whether choiceId is correct
        //     (Question.isCorrect(choiceId)).
        //
        //  b) tokensAwarded: 0 if wrong. If correct, look up the value from
        //     ScoreLadder.STANDARD or ScoreLadder.HIGH_RISK (whichever
        //     matches the active Protocol) at the current question index.
        //
        //  c) Update the running total by tokensAwarded.
        //
        //  d) capabilityUnlocked / capabilityName: true only when this
        //     question's number (index + 1) is one of 3, 5, 8, 10, 13, 15
        //     (CONTEXT.md §2.1) -- only check this if the answer was
        //     correct, since a wrong answer on e.g. Q3 shouldn't unlock
        //     Memory.
        //
        //  e) Checkpoint securing: if the answer was correct AND this
        //     question's number is one of the active Protocol's checkpoint
        //     numbers, "secure" the current running total and index into
        //     your secured-score/secured-index fields.
        //
        //  f) gameOver: true if the answer was wrong (see the class-level
        //     ASSUMPTION above), OR if the answer was correct and this was
        //     the last question (Q15).
        //
        //  g) On a wrong answer specifically: the running total the GUI
        //     sees should roll back to whatever was last secured -- not
        //     stay at whatever it was before this wrong answer. Re-read
        //     CONTEXT.md §2.2's rollback rule if that sentence doesn't
        //     click yet.
        //
        //  h) Only advance to the next question index if the session isn't
        //     over -- a wrong answer or a Q15 win shouldn't move past the
        //     end of the list.
        //
        //  i) Build and return the TurnResult with everything computed
        //     above.
        return null; // TODO: replace with the real TurnResult
    }

    @Override
    public SessionResult endSession() {
        // TODO 5: mark the session over. SessionResult itself is still an
        // empty stub (that's Week 4 work per R1_Engine_Weekly_Plan.md) --
        // for now just return `new SessionResult()` so this compiles and
        // the "Return early" end condition has somewhere to go.
        return null; // TODO: replace with a real SessionResult
    }
}
