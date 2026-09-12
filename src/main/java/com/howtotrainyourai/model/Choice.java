package com.howtotrainyourai.model;

import java.util.Objects;

/**
 * One answer option for a Question.
 *
 * choiceId is deliberately the ORIGINAL letter from the CSV column
 * (choiceA/B/C/D -> "a"/"b"/"c"/"d"), not the position the choice happens
 * to display in on screen. QuestionSource shuffles the order Choices are
 * shown in, but choiceId never changes — that's what lets
 * Question.isCorrect(choiceId) keep working no matter how the choices
 * were reordered for display.
 */
public class Choice {

    private final String choiceId;
    private final String text;

    /**
     * @param choiceId the original letter ("a".."d") — never the display position
     * @param text     the choice text shown on screen
     */
    public Choice(String choiceId, String text) {
        this.choiceId = Objects.requireNonNull(choiceId, "choiceId");
        this.text = Objects.requireNonNull(text, "text");
    }

    public String getChoiceId() {
        return choiceId;
    }

    public String getText() {
        return text;
    }
}
