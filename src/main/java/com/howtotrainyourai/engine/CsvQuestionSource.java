package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Real QuestionSource implementation (R2). Loads the full question bank via
 * QuestionBankLoader once, then on every buildSession() call:
 *
 *  1) Groups the bank by Bloom stage, then by module within each stage.
 *  2) For each stage, picks the required number of questions one at a time,
 *     choosing UNIFORMLY among whichever modules still have unused
 *     questions left for that stage. This means a module with more total
 *     questions is never more likely to get picked than a module with
 *     fewer -- every module gets an equal chance on every single pick
 *     (Assignment Guide #1 item 1d).
 *  3) Shuffles each selected question's choices in place.
 *
 * Ordering of the returned list follows ascending Bloom stage, per
 * QuestionSource's contract.
 */
public class CsvQuestionSource implements QuestionSource {

    /** Bloom stage -> how many questions from that stage go into one session. Must sum to 15. */
    private static final Map<String, Integer> QUESTIONS_PER_STAGE = new LinkedHashMap<>();
    static {
        QUESTIONS_PER_STAGE.put("Remember", 3);
        QUESTIONS_PER_STAGE.put("Understand", 2);
        QUESTIONS_PER_STAGE.put("Apply", 3);
        QUESTIONS_PER_STAGE.put("Analyze", 2);
        QUESTIONS_PER_STAGE.put("Evaluate", 3);
        QUESTIONS_PER_STAGE.put("Synthesis", 2);
    }

    private final List<Question> bank;
    private final Random random;

    public CsvQuestionSource() {
        this(QuestionBankLoader.loadAll(), new Random());
    }

    public CsvQuestionSource(String resourcePath) {
        this(QuestionBankLoader.loadAll(resourcePath), new Random());
    }

    /** Package-visible so tests can inject a fixed bank and a seeded Random. */
    CsvQuestionSource(List<Question> bank, Random random) {
        this.bank = bank;
        this.random = random;
    }

    @Override
    public List<Question> buildSession() {
        Map<String, Map<String, List<Question>>> byStageThenModule = groupByStageThenModule(bank);

        List<Question> session = new ArrayList<>();
        for (Map.Entry<String, Integer> stageEntry : QUESTIONS_PER_STAGE.entrySet()) {
            String stage = stageEntry.getKey();
            int countNeeded = stageEntry.getValue();

            Map<String, List<Question>> byModule =
                    byStageThenModule.getOrDefault(stage, Map.of());
            session.addAll(pickEquallyAcrossModules(stage, byModule, countNeeded));
        }

        shuffleChoicesInPlace(session);
        return session;
    }

    private Map<String, Map<String, List<Question>>> groupByStageThenModule(List<Question> questions) {
        Map<String, Map<String, List<Question>>> result = new LinkedHashMap<>();
        for (Question q : questions) {
            result.computeIfAbsent(q.getBloom(), k -> new LinkedHashMap<>())
                  .computeIfAbsent(q.getModule(), k -> new ArrayList<>())
                  .add(q);
        }
        return result;
    }

    /**
     * Picks `countNeeded` questions from a single Bloom stage. On every pick,
     * every module that still has at least one unused question for this
     * stage is equally likely to be chosen -- regardless of how many
     * questions that module has in total for this stage.
     */
    private List<Question> pickEquallyAcrossModules(
            String stage, Map<String, List<Question>> byModule, int countNeeded) {

        // Work on shuffled copies so we can pop questions off without
        // touching the shared bank, and so "pick from this module" itself
        // returns a random question rather than always the same one.
        Map<String, List<Question>> remaining = new LinkedHashMap<>();
        for (Map.Entry<String, List<Question>> e : byModule.entrySet()) {
            List<Question> copy = new ArrayList<>(e.getValue());
            Collections.shuffle(copy, random);
            remaining.put(e.getKey(), copy);
        }

        List<Question> picked = new ArrayList<>();
        for (int i = 0; i < countNeeded; i++) {
            List<String> availableModules = new ArrayList<>();
            for (Map.Entry<String, List<Question>> e : remaining.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    availableModules.add(e.getKey());
                }
            }
            if (availableModules.isEmpty()) {
                throw new IllegalStateException(
                        "Not enough \"" + stage + "\" questions in the bank to build a session "
                                + "(needed " + countNeeded + ", only found " + picked.size() + ").");
            }

            String chosenModule = availableModules.get(random.nextInt(availableModules.size()));
            List<Question> pool = remaining.get(chosenModule);
            Question chosen = pool.remove(pool.size() - 1); // pool is pre-shuffled, so "last" is random
            picked.add(chosen);
        }
        return picked;
    }

    private void shuffleChoicesInPlace(List<Question> questions) {
        for (Question q : questions) {
            Collections.shuffle(q.getChoices(), random);
        }
    }
}
