package com.howtotrainyourai.model;

import java.util.List;

/**
 * One multiple-choice question, loaded from a row in an MCQ/*.csv file
 * (see CONTEXT.md §3 for the exact pipe-delimited column layout).
 *
 * A Question is just data + one helper to check an answer — it does not
 * know about scoring, sessions, or the GUI. Those live in GameEngine.
 */
public class Question {

    // Unique question id, e.g. "w03-q001" — matches the "id" column in the CSV.
    private final String id;

    // Which module/week this question belongs to, e.g. "week03".
    // Used by QuestionSource to pick an equal number of questions per module.
    private final String module;

    // Bloom's Taxonomy level, one of:
    // Remember, Understand, Apply, Analyze, Evaluate, Synthesis
    // Used to keep questions ordered from easiest to hardest in a session.
    private final String bloom;

    // The question text shown on screen.
    private final String text;

    // Shown to the player after they answer, right or wrong.
    private final String explanation;

    // The 4 answer choices. QuestionSource shuffles the ORDER of this list
    // before handing the Question to the GUI — the list here is expected to
    // already be shuffled by the time anyone but the loader sees it.
    private final List<Choice> choices;

    // The choiceId ("a".."d") of the correct choice, from the CSV's
    // "correct" column. This is the ORIGINAL letter — same rule as
    // Choice.choiceId — so shuffling the choices list never breaks scoring.
    private final String correctChoiceId;

    public Question(String id, String module, String bloom, String text,
            String explanation, List<Choice> choices, String correctChoiceId) {
        this.id = id;
        this.module = module;
        this.bloom = bloom;
        this.text = text;
        this.explanation = explanation;
        this.choices = choices;
        this.correctChoiceId = correctChoiceId;
    }

    public String getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public String getBloom() {
        return bloom;
    }

    public String getText() {
        return text;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    /**
     * True if choiceId (the letter the player clicked/answered) matches the
     * correct choice for this question. Compares against the ORIGINAL letter,
     * so it works correctly no matter how the choices were shuffled on screen.
     */
    public boolean isCorrect(String choiceId) {
        return correctChoiceId.equalsIgnoreCase(choiceId);
    }
}
