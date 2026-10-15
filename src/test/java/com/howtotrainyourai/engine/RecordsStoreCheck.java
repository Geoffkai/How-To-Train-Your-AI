package com.howtotrainyourai.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class RecordsStoreCheck {
    public static void main(String[] args) throws Exception {
        Path home = Path.of("/users/operator");
        assert RecordsStore.resolveFile("Windows 11", home.toString(), Map.of("APPDATA", "C:/Users/operator/AppData/Roaming"))
                .equals(Path.of("C:/Users/operator/AppData/Roaming/HowToTrainYourAI/TrainingRecords.csv"));
        assert RecordsStore.resolveFile("Mac OS X", home.toString(), Map.of())
                .equals(home.resolve("Library/Application Support/HowToTrainYourAI/TrainingRecords.csv"));
        assert RecordsStore.resolveFile("Linux", home.toString(), Map.of())
                .equals(home.resolve(".local/share/HowToTrainYourAI/TrainingRecords.csv"));
        assert RecordsStore.resolveFile("Linux", home.toString(), Map.of("XDG_DATA_HOME", "/data"))
                .equals(Path.of("/data/HowToTrainYourAI/TrainingRecords.csv"));

        Path dir = Files.createTempDirectory("records-check");
        try {
            Path file = dir.resolve("profile/TrainingRecords.csv");
            RecordsStore.append(file, new SessionResult("LOW", 25, "MEMORY", Protocol.STANDARD, "FAIL"));
            RecordsStore.append(file, new SessionResult("HIGH", 100, "SYNTHESIS", Protocol.HIGH_RISK, "WIN"));
            assert RecordsStore.readAll(file).size() == 2;
            assert RecordsStore.topScores(file, 1).get(0)[0].equals("HIGH");
            assert Files.readAllLines(file).size() == 3;
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("RecordsStoreCheck passed");
    }
}
