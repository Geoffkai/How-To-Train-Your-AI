package com.howtotrainyourai.engine;

/** Immutable summary written to the training records when a session ends. */
public final class SessionResult {
    private final String trainerName;
    private final int finalScore;
    private final String highestCapability;
    private final Protocol protocol;
    private final String result;

    public SessionResult(String trainerName, int finalScore, String highestCapability,
            Protocol protocol, String result) {
        this.trainerName = trainerName;
        this.finalScore = finalScore;
        this.highestCapability = highestCapability;
        this.protocol = protocol;
        this.result = result;
    }

    public String getTrainerName() { return trainerName; }
    public int getFinalScore() { return finalScore; }
    public String getHighestCapability() { return highestCapability; }
    public Protocol getProtocol() { return protocol; }
    public String getResult() { return result; }

    String toRecordLine() {
        return String.join("|", clean(trainerName), Integer.toString(finalScore),
                clean(highestCapability), protocol.name(), result);
    }

    private static String clean(String value) {
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
