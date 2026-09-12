package com.howtotrainyourai.engine;

/**
 * Throwaway console harness for Week 1. Plays all 15 questions from a
 * FakeQuestionSource via System.in/System.out — no GUI, no GameEngineImpl
 * (that doesn't exist until Week 2), no scoring. Just proves Question/
 * Choice/QuestionSource fit together end to end.
 */
public class TerminalGameLoop {

    public static void main(String[] args) {
        // TODO 1: Get your questions.
        // - new FakeQuestionSource(), then call .buildSession() on it to
        // get your List<Question>.

        // TODO 2: Open ONE Scanner over System.in for the whole game
        // (create it once, before the loop — not one per question).

        // TODO 3: Loop over the 15 questions in order. For each one:
        // a) Print the question's text (Question has a getter for this).
        // b) Print each of its 4 choices — you'll need to loop over
        // question.getChoices() and print each one's choiceId + text,
        // e.g. in the form "a) <text>".
        // c) Read the player's typed answer as a line of input.
        // d) Ask the question itself whether that answer was right:
        // question.isCorrect(theAnswerTheyTyped).
        // e) Print something like "Correct!" or "Wrong. The answer was X."
        // (question.getExplanation() is available if you want to show it).
        // f) Per the spec: if the answer was wrong, stop here — don't go on
        // to the next question. (No lifelines yet — that's Week 2.)

        // TODO 4: Once the loop is done (either you got through all 15, or
        // you stopped early on a wrong answer), print a short end-of-game
        // summary — e.g. how many questions were answered correctly before
        // stopping, or a "you cleared all 15!" message.

        // TODO 5: Close the Scanner.

        // Sanity check once you've filled this in: running this file's
        // main() directly (no Swing, nothing else running) should let you
        // type answers in the console and walk through all 15 fake
        // questions without an exception.
    }
}
