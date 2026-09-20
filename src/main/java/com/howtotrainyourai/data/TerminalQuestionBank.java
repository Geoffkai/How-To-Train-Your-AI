package com.howtotrainyourai.data;

import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone manual test harness -- run this INSTEAD of Main to sanity-check
 * QuestionBankLoader + CsvQuestionSource before wiring them into GameEngine.
 *
 * For every question in a session it shows:
 * - the question text
 * - the (already-shuffled) answer choices, labeled A-D
 * - after you answer: the correct answer (always, for debugging/testing)
 * - if you got it wrong: the explanation
 *
 * At the end it prints your score out of 15.
 */
public class TerminalQuestionBank {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        List<Question> session;
        try {
            session = new CsvQuestionSource().buildSession();
        } catch (RuntimeException e) {
            System.out.println("Failed to build a question session: " + e.getMessage());
            return;
        }

        int score = 0;
        for (int i = 0; i < session.size(); i++) {
            Question question = session.get(i);
            System.out.println();
            System.out.println("=".repeat(70));
            System.out.println("Question " + (i + 1) + " of " + session.size()
                    + "  [Category: " + displayCategory(question.getBloom()) + "]");
            System.out.println("=".repeat(70));
            System.out.println(question.getText());
            System.out.println();

            List<Choice> choices = question.getChoices();
            for (int c = 0; c < choices.size(); c++) {
                System.out.println(label(c) + ") " + choices.get(c).getText());
            }

            int pickedIndex = promptForAnswer(scanner, choices.size());
            Choice pickedChoice = choices.get(pickedIndex);
            boolean correct = question.isCorrect(pickedChoice.getChoiceId());

            int correctIndex = indexOfCorrectChoice(choices, question);

            System.out.println();
            System.out.println("Your answer:    " + label(pickedIndex) + ") " + pickedChoice.getText());
            System.out.println("Correct answer: " + label(correctIndex) + ") "
                    + choices.get(correctIndex).getText() + "   [shown for testing]");

            if (correct) {
                System.out.println("Result: CORRECT");
                score++;
            } else {
                System.out.println("Result: INCORRECT");
                System.out.println("Explanation: " + question.getExplanation());
            }
        }

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("Final score: " + score + " / " + session.size());
        System.out.println("=".repeat(70));

        scanner.close();
    }

    /** Keeps asking until the user types a valid letter (A-D) or number (1-4). */
    private static int promptForAnswer(Scanner scanner, int choiceCount) {
        while (true) {
            System.out.print("Your answer (A-" + label(choiceCount - 1) + "): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                continue;
            }

            // Accept a letter like "B" ...
            char letter = input.charAt(0);
            int fromLetter = letter - 'A';
            if (fromLetter >= 0 && fromLetter < choiceCount) {
                return fromLetter;
            }

            // ... or a number like "2".
            try {
                int fromNumber = Integer.parseInt(input) - 1;
                if (fromNumber >= 0 && fromNumber < choiceCount) {
                    return fromNumber;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the retry message below
            }

            System.out.println("Please enter a letter A-" + label(choiceCount - 1)
                    + " or a number 1-" + choiceCount + ".");
        }
    }

    private static int indexOfCorrectChoice(List<Choice> choices, Question question) {
        for (int i = 0; i < choices.size(); i++) {
            if (question.isCorrect(choices.get(i).getChoiceId())) {
                return i;
            }
        }
        // Should never happen if the data is well-formed.
        throw new IllegalStateException("No choice in this question matches its correct answer.");
    }

    private static String label(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /** Friendlier display names for the raw Bloom stage names from the CSV. */
    private static String displayCategory(String bloom) {
        switch (bloom) {
            case "Remember":
                return "Memory";
            case "Understand":
                return "Understanding";
            case "Apply":
                return "Application";
            case "Analyze":
                return "Analysis";
            case "Evaluate":
                return "Evaluation";
            case "Synthesis":
                return "Synthesize";
            default:
                return bloom;
        }
    }
}
