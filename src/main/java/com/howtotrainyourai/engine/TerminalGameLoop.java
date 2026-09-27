package com.howtotrainyourai.engine;

import com.howtotrainyourai.data.CsvQuestionSource;
import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.util.List;
import java.util.Scanner;

/**
 * The real terminal harness -- drives GameEngineImpl end to end (not raw
 * Question.isCorrect() checks like the old Week 1 version did). This is
 * "the one thing to run for full core logic" -- no GUI, no Swing, just
 * this engine and a console.
 *
 * Display letters (A-D) come from a choice's POSITION in the shuffled list,
 * never from its choiceId -- choiceId is the original CSV letter and stays
 * hidden from the player, since showing it would leak which slot a choice
 * originally came from. The player picks a position; this class maps that
 * position back to the Choice and hands its choiceId to the engine. A GUI
 * does the identical mapping: button #2 clicked -> choices.get(1) ->
 * getChoiceId() -> submitAnswer(...).
 *
 * Flip USE_FAKE_DATA to true for a controlled test run -- e.g. forcing a
 * wrong answer at a specific question number, to verify checkpoint
 * rollback (see R1_Engine_Weekly_Plan.md's Week 2 "Done when": force-fail
 * at Q4, Q6, Q11, confirm the score falls back to the right checkpoint
 * each time). FakeQuestionSource gives fixed, known questions in a fixed
 * order, so you know exactly which question number you're answering.
 * Leave it false for normal play against the real question bank.
 */
public class TerminalGameLoop {

    private static final boolean USE_FAKE_DATA = false;
    private static final int TOTAL_QUESTIONS = 15;

    public static void main(String[] args) {
        QuestionSource questionSource = USE_FAKE_DATA
                ? new FakeQuestionSource()
                : new CsvQuestionSource();

        GameEngine engine = new GameEngineImpl(questionSource);
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Trainer Name: ");
        String trainerName = scanner.nextLine().trim();

        Protocol protocol = promptForProtocol(scanner);

        try {
            engine.startSession(trainerName, protocol);
        } catch (RuntimeException e) {
            System.out.println("Failed to build a question session: " + e.getMessage());
            scanner.close();
            return;
        }

        TurnResult result = null;
        int questionNumber = 1;
        boolean isRunning = true;

        while (isRunning) {
            Question question = engine.currentQuestion();
            List<Choice> choices = question.getChoices();

            System.out.println();
            System.out.println("=".repeat(70));
            System.out.println("Question " + questionNumber + " of " + TOTAL_QUESTIONS
                    + "  [Category: " + displayCategory(question.getBloom()) + "]");
            System.out.println("=".repeat(70));
            System.out.println(question.getText());
            System.out.println();

            for (int i = 0; i < choices.size(); i++) {
                System.out.println(label(i) + ") " + choices.get(i).getText());
            }

            int pickedIndex = promptForAnswer(scanner, choices.size());
            Choice picked = choices.get(pickedIndex);

            result = engine.submitAnswer(picked.getChoiceId());

            System.out.println();
            System.out.println("Your answer: " + label(pickedIndex) + ") " + picked.getText());

            if (result.isCorrect()) {
                System.out.println("Result: CORRECT   +" + result.getTokensAwarded()
                        + " tokens   (total: " + result.getRunningTotal() + ")");
            } else {
                System.out.println("Result: INCORRECT");
                System.out.println("Explanation: " + question.getExplanation());
                System.out.println("Score falls back to: " + result.getRunningTotal());
            }

            if (result.isCapabilityUnlocked()) {
                System.out.println(">> CAPABILITY RESTORED: " + result.getCapabilityName());
            }

            if (result.isGameOver()) {
                isRunning = false;
            } else {
                questionNumber++;
            }
        }

        engine.endSession();

        System.out.println();
        System.out.println("=".repeat(70));
        if (result.isCorrect()) {
            System.out.println("AI CORE: FULLY ACTIVATED -- " + trainerName
                    + " cleared all " + TOTAL_QUESTIONS + " questions.");
        } else {
            System.out.println("RESTORATION FAILED at question " + questionNumber + ".");
        }
        System.out.println("Protocol: " + protocol + "   Final score: " + result.getRunningTotal());
        System.out.println("=".repeat(70));

        scanner.close();
    }

    /** Keeps asking until the player picks Standard or High Risk. */
    private static Protocol promptForProtocol(Scanner scanner) {
        while (true) {
            System.out.println();
            System.out.println("Choose a restoration protocol:");
            System.out.println("    1) Standard    (3 lifelines, checkpoints at Q5 and Q10)");
            System.out.println("    2) High Risk   (2 lifelines, checkpoint at Q5, double tokens)");
            System.out.print("Your choice (1-2): ");

            String input = scanner.nextLine().trim();
            if (input.equals("1")) {
                return Protocol.STANDARD;
            }
            if (input.equals("2")) {
                return Protocol.HIGH_RISK;
            }
            System.out.println("Please enter 1 or 2.");
        }
    }

    /**
     * Keeps asking until the player types a valid letter (A-D) or number
     * (1-4). Returns the zero-based POSITION they picked, not a choiceId --
     * the caller maps that position back to the actual Choice.
     */
    private static int promptForAnswer(Scanner scanner, int choiceCount) {
        while (true) {
            System.out.print("Your answer (A-" + label(choiceCount - 1) + "): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                continue;
            }

            int fromLetter = input.charAt(0) - 'A';
            if (fromLetter >= 0 && fromLetter < choiceCount) {
                return fromLetter;
            }

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

    

    

    