package com.howtotrainyourai.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Reads and appends the game's local training-record file. */
public final class RecordsStore {
    private static final Path FILE = Path.of("TrainingRecords.csv");
    private static final String HEADER = "trainer|score|highest capability|protocol|result";

    private RecordsStore() {}

    public static void append(SessionResult result) throws IOException {
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
}
