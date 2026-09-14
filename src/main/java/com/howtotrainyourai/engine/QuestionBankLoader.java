package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and parses the pipe-delimited question bank CSV (see CONTEXT.md §3)
 * from the classpath and turns each row into a Question. This class only
 * knows how to read + parse the file -- it has no opinion about which
 * questions end up in a session, module balance, or choice order. That
 * selection/shuffling logic lives in CsvQuestionSource.
 *
 * Expected column order (10 columns, delimiter "|"):
 *   id|module|bloom|question|choiceA|choiceB|choiceC|choiceD|correct|explanation
 *
 * A field may optionally be wrapped in double quotes (a leftover from the
 * source spreadsheet's CSV export, e.g. fields that used to contain a comma
 * or a literal quote character). Quotes are stripped and doubled quotes
 * ("") are unescaped into a single ". A quoted field is also allowed to
 * spill across more than one physical line in the file -- this happens in
 * the real data (e.g. question m01-04) -- continuation lines are folded
 * back into a single logical row before splitting on "|".
 */
public final class QuestionBankLoader {

    /** Default classpath location of the question bank. */
    public static final String DEFAULT_RESOURCE_PATH = "/data/Question_Bank_PDL.csv";

    private static final int COLUMN_COUNT = 10;

    private QuestionBankLoader() {
        // static utility class -- not meant to be instantiated
    }

    /** Loads every question from the default resource path. */
    public static List<Question> loadAll() {
        return loadAll(DEFAULT_RESOURCE_PATH);
    }

    /**
     * Loads every question from the given classpath resource.
     *
     * @param resourcePath a classpath-relative path, e.g. "/data/Question_Bank_PDL.csv"
     */
    public static List<Question> loadAll(String resourcePath) {
        try (InputStream in = QuestionBankLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Question bank resource not found on classpath: " + resourcePath);
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parse(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read question bank: " + resourcePath, e);
        }
    }

    private static List<Question> parse(BufferedReader reader) throws IOException {
        List<String> records = readLogicalRecords(reader);
        if (records.isEmpty()) {
            throw new IllegalStateException("Question bank file is empty.");
        }

        List<Question> questions = new ArrayList<>();
        // records.get(0) is the header row ("id|module|bloom|..."); skip it.
        for (int i = 1; i < records.size(); i++) {
            String record = records.get(i);
            if (record.isBlank()) {
                continue;
            }
            questions.add(parseRow(record, i + 1));
        }
        return questions;
    }

    /**
     * Reads the file line by line and folds any quoted field that spans
     * multiple physical lines back into one logical record, so each entry
     * in the returned list is exactly one CSV row ready to split on "|".
     */
    private static List<String> readLogicalRecords(BufferedReader reader) throws IOException {
        List<String> records = new ArrayList<>();
        StringBuilder current = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (current == null) {
                current = new StringBuilder(line);
            } else {
                // A stray line break landed inside a quoted field -- rejoin
                // with a single space so the question text stays readable.
                current.append(' ').append(line.trim());
            }
            if (isQuoteBalanced(current)) {
                records.add(current.toString());
                current = null;
            }
        }
        if (current != null) {
            // Trailing unbalanced quotes at EOF -- best effort, add as-is.
            records.add(current.toString());
        }
        return records;
    }

    private static boolean isQuoteBalanced(CharSequence s) {
        int quoteCount = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                quoteCount++;
            }
        }
        return quoteCount % 2 == 0;
    }

    private static Question parseRow(String record, int rowNumberForErrors) {
        String[] rawFields = record.split("\\|", -1);
        if (rawFields.length != COLUMN_COUNT) {
            throw new IllegalStateException(
                    "Expected " + COLUMN_COUNT + " columns but found " + rawFields.length
                            + " on data row " + rowNumberForErrors + ": " + record);
        }

        String id = unquote(rawFields[0]);
        String module = unquote(rawFields[1]);
        String bloom = unquote(rawFields[2]);
        String text = unquote(rawFields[3]);
        String choiceAText = unquote(rawFields[4]);
        String choiceBText = unquote(rawFields[5]);
        String choiceCText = unquote(rawFields[6]);
        String choiceDText = unquote(rawFields[7]);
        String correct = unquote(rawFields[8]).toLowerCase();
        String explanation = unquote(rawFields[9]);

        // Mutable list on purpose: CsvQuestionSource shuffles this list's
        // order in place before handing the Question off (see Question.java
        // and Choice.java's comments on why choiceId, not position, is the
        // source of truth for correctness checks).
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice("a", choiceAText));
        choices.add(new Choice("b", choiceBText));
        choices.add(new Choice("c", choiceCText));
        choices.add(new Choice("d", choiceDText));

        return new Question(id, module, bloom, text, explanation, choices, correct);
    }

    /** Strips one layer of wrapping quotes, if present, and unescapes "" into ". */
    private static String unquote(String field) {
        String f = field.strip();
        if (f.length() >= 2 && f.startsWith("\"") && f.endsWith("\"")) {
            f = f.substring(1, f.length() - 1);
        }
        f = f.replace("\"\"", "\"").strip();
        return normalizeTypographicPunctuation(f);
    }

    /**
     * Replaces "smart"/typographic punctuation (curly quotes, en/em dashes,
     * ellipsis) with their plain ASCII equivalents. The source spreadsheet
     * auto-substitutes these, and while they're perfectly valid UTF-8 (this
     * loader reads the file as UTF-8), printing them depends on the reading
     * program's console/output encoding actually supporting them -- which
     * isn't guaranteed (e.g. a Windows terminal on a legacy codepage will
     * silently print "?" instead). Normalizing here means every question's
     * text is safe to print anywhere, regardless of that.
     */
    private static String normalizeTypographicPunctuation(String s) {
        return s
                .replace('\u2018', '\'')   // ‘ left single quote
                .replace('\u2019', '\'')   // ’ right single quote / apostrophe
                .replace('\u201C', '"')    // “ left double quote
                .replace('\u201D', '"')    // ” right double quote
                .replace('\u2013', '-')    // – en dash
                .replace('\u2014', '-')    // — em dash
                .replace("\u2026", "...")  // … ellipsis
                .strip();
    }
}