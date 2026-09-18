package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Question;
import java.util.List;

/**
 * Produces the 15 questions for one game session. Nothing else — no
 * tracking of the current question or submitted answers; that's
 * GameEngine's job, not this one's.
 *
 * Implementations (see CONTEXT.md for the full contract):
 *  - FakeQuestionSource (Week 1, this repo) — hardcoded dummy data, used to
 *    prove GameEngine/TerminalGameLoop work before real data exists.
 *  - The real implementation (R2, Week 3) — reads MCQ/*.csv (pipe-delimited,
 *    see CONTEXT.md §3), picks an equal number of questions per module
 *    (Assignment Guide #1 item 1d), and shuffles each question's choices.
 */
public interface QuestionSource {

    /**
     * @return exactly 15 Questions, ordered by ascending Bloom stage
     *         (Remember, Understand, Apply, Analyze, Evaluate, Synthesis —
     *         see CONTEXT.md §2.1 for the exact question-count-per-stage
     *         breakdown). Each Question's choices must already be shuffled;
     *         callers never reorder them. A different session (different
     *         questions and/or different choice order) is expected on every
     *         call, per Assignment Guide #1 items 1b/2.
     */
    List<Question> buildSession();
}
