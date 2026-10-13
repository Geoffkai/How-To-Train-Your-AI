package com.howtotrainyourai.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads and appends the game's local training-record file.
 *
 * The file lives in the player's %APPDATA%\HowToTrainYourAI folder, so it
 * survives no matter which folder the game is launched from. If APPDATA isn't
 * set (non-Windows machine), it falls back to the user's home directory
 * instead of crashing.
 */
public final class RecordsStore {
    private static final String FOLDER_NAME = "HowToTrainYourAI";
    private static final String FILE_NAME = "TrainingRecords.csv";
    private static final String HEADER = "trainer|score|highest capability|protocol|result";
    private static final Path FILE = resolveFile();

    private RecordsStore() {}

    private static Path resolveFile() {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null && !appData.isBlank())
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"));
        return base.resolve(FOLDER_NAME).resolve(FILE_NAME);
    }

    /** Where the records file is (or will be) stored. Handy for debugging. */
    public static Path location() {
        return FILE;
    }

    public static void append(SessionResult result) throws IOException {
        // First write on a fresh machine: the folder doesn't exist yet.
        Files.createDirectories(FILE.getParent());
        boolean needsHeader = !Files.exists(FILE) || Files.size(FILE) == 0;
        try (var writer = Files.newBufferedWriter(FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needsHeader) {
                writer.write(HEADER);
                writer.newLine();
            }
            writer.write(result.toRecordLine());
            writer.newLine();
        }
    }

    public static List<String[]> readAll() throws IOException {
        if (!Files.exists(FILE)) return List.of();
        List<String[]> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(FILE);
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = lines.get(i).split("\\|", -1);
            if (fields.length == 5) records.add(fields);
        }
        return List.copyOf(records);
    }

    /**
     * The best sessions on file, highest score first. Each element has the
     * same 5 columns as readAll(): trainer, score, highest capability,
     * protocol, result. Rows whose score isn't a number are skipped. Ties
     * keep their file order, so the earlier run ranks higher.
     *
     * @param limit how many to return at most (e.g. 10 for a top-10 board)
     */
    public static List<String[]> topScores(int limit) throws IOException {
        List<String[]> ranked = new ArrayList<>();
        for (String[] record : readAll()) {
            try {
                Integer.parseInt(record[1].trim());
                ranked.add(record);
            } catch (NumberFormatException ignored) {
                // corrupt row -- leave it off the board
            }
        }
        ranked.sort(Comparator.comparingInt((String[] r) -> Integer.parseInt(r[1].trim())).reversed());
        return List.copyOf(ranked.subList(0, Math.min(limit, ranked.size())));
    }
}
