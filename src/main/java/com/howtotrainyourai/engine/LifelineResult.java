package com.howtotrainyourai.engine;

import java.util.List;

/**
 * What a lifeline revealed. Returned by GameEngine.useLifeline(lifeline) --
 * the GUI reads whichever fields belong to the lifeline it just used.
 *
 * Data only, same as TurnResult -- the logic that fills these in lives in
 * GameEngineImpl.
 */
public class LifelineResult {

    private final Lifeline lifeline;

    // BINARY_CHOICE: the two WRONG choiceIds to hide. Empty for other lifelines.
    private final List<String> removedChoiceIds;

    // PREDICT: the choiceId the AI suggests (not guaranteed correct), or null.
    private final String predictedChoiceId;

    // PREDICT: how sure the AI claims to be, 0-100. 0 for other lifelines.
    private final int confidencePercent;

    private LifelineResult(Lifeline lifeline, List<String> removedChoiceIds,
            String predictedChoiceId, int confidencePercent) {
        this.lifeline = lifeline;
        this.removedChoiceIds = List.copyOf(removedChoiceIds);
        this.predictedChoiceId = predictedChoiceId;
        this.confidencePercent = confidencePercent;
    }

    static LifelineResult binaryChoice(List<String> removedChoiceIds) {
        return new LifelineResult(Lifeline.BINARY_CHOICE, removedChoiceIds, null, 0);
    }

    static LifelineResult predict(String predictedChoiceId, int confidencePercent) {
        return new LifelineResult(Lifeline.PREDICT, List.of(), predictedChoiceId, confidencePercent);
    }

    public Lifeline getLifeline() {
        return lifeline;
    }

    public List<String> getRemovedChoiceIds() {
        return removedChoiceIds;
    }

    public String getPredictedChoiceId() {
        return predictedChoiceId;
    }

    public int getConfidencePercent() {
        return confidencePercent;
    }
}
