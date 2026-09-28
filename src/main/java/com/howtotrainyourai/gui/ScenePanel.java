package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.FakeQuestionSource;
import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.Protocol;
import com.howtotrainyourai.engine.SessionResult;
import com.howtotrainyourai.engine.TurnResult;
import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

// blocking pass for the vintage ai lab scene, see
// ~/dev/Java/CMSC170/reference-photos/restoration-bay-fold-claude-table.html for the
// frozen mockup this tracks (not the earlier drafts -- restoration-bay-fold.html's
// top-right lifeline panel and chalkboard-ledge NEXT button are superseded). same seam
// as PlayPanel: ScenePanel(CardPanel), startGame(GameEngine). PlayPanel stays the
// working fallback until this is swapped in at Main.java after the oct 4 playthrough.
//
// flat shapes only, no gradients/textures. arm sweep is a later phase, not here.
public class ScenePanel extends JPanel {

    private static final int TOTAL_QUESTIONS = 15;
    private static final double LOGICAL_W = 1600;
    private static final double LOGICAL_H = 1000;

    // mockup css custom properties, restoration-bay-fold.html :root
    private static final Color PAPER = Color.decode("#ddcca2");
    private static final Color BRASS = Color.decode("#a3803a");
    private static final Color BRASS_LIT = Color.decode("#dcc07a");
    private static final Color BRASS_OX = Color.decode("#5f5a36");
    private static final Color WOOD_DARK = Color.decode("#2e2013");
    private static final Color TILE = Color.decode("#4f6660");
    private static final Color MINT = Color.decode("#6c887b");
    private static final Color STEEL = Color.decode("#7a8783");
    private static final Color STEEL_DARK = Color.decode("#343e3b");
    private static final Color SLATE = Color.decode("#2b3d33");
    private static final Color CHALK = Color.decode("#e2dccb");
    private static final Color CHALK_YELLOW = Color.decode("#e6d38c");
    private static final Color TAPE_GREEN = Color.decode("#4c6b4f");
    private static final Color TAPE_RED = Color.decode("#8c3b2e");
    private static final Color CEILING = Color.decode("#12201c");
    // mockup .bay/.cell background (#0a0706/#141210/#1b211f/#0f1312 are all close
    // near-blacks in the mockup; one well color covers all of them here)
    private static final Color SHELF_WELL = Color.decode("#0a0706");

    private static final Font FONT_LADDER = new Font(Font.MONOSPACED, Font.BOLD, 11);
    private static final Font FONT_PROMPT = new Font(Font.MONOSPACED, Font.BOLD, 20);
    private static final Font FONT_ANSWER = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final Font FONT_LABEL = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font FONT_LIFELINE = new Font(Font.MONOSPACED, Font.BOLD, 8);
    private static final Font FONT_PAPER = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    // logical (1600x1000) layout rects, pulled from the mockup's percentage positions
    private static final double CEILING_H = 60;
    private static final double CAB_L_X = -16, CAB_L_Y = 190, CAB_L_W = 176, CAB_L_H = 520;
    private static final double CAB_R_X = 1456, CAB_R_Y = 250, CAB_R_W = 160, CAB_R_H = 440;
    private static final double LAMPS_X = 240, LAMPS_Y = 52, LAMPS_W = 1312, LAMP_W = 144, LAMP_H = 18;
    private static final double STATION_X = 168, STATION_Y = 105, STATION_W = 1216, STATION_H = 630;
    private static final double LADDER_X = 224, LADDER_Y = 134, LADDER_W = 912, LADDER_H = 37, LADDER_GAP = 4.8;
    private static final double BOARD_X = 208, BOARD_Y = 205, BOARD_W = 1136, BOARD_H = 440;
    private static final double LEDGE_X = 195, LEDGE_Y = 673, LEDGE_W = 1162, LEDGE_H = 24;
    private static final double MONITOR_X = 1288, MONITOR_Y = 610, MONITOR_W = 272, MONITOR_H = 270;
    private static final double BACK_X = 20, BACK_Y = 20, BACK_W = 80, BACK_H = 30;

    // desk container: left:-4%/right:-4%/bottom:-6%/height:34% of the 1600x1000 canvas
    private static final double DESK_X = -64, DESK_Y = 720, DESK_W = 1728, DESK_H = 340;
    // keybank: left:31%/bottom:30% of the desk box (not 25%/40% -- that was the
    // superseded draft's position)
    private static final double KEYS_X = 472, KEYS_Y = 862, KEY_W = 115, KEY_H = 96, KEY_GAP = 21;
    // next probe: left:68.2cqw/bottom:6.4cqw of the desk box -- moved into the answer
    // row per the handoff's physical-reach fix, lands in the same row as the keys
    private static final double NEXT_X = 1027, NEXT_Y = 862, NEXT_W = 144, NEXT_H = 96;
    // decorative backing panel behind the keybank/next row: left:31.5cqw/bottom:5.3cqw
    // of the desk box, width 47.5cqw, height 8.2cqw
    private static final double DECK_X = 440, DECK_Y = 844, DECK_W = 760, DECK_H = 131;
    // raised rear shelf backing strip, full desk width: top:-.4cqw/height:8cqw of the desk box
    private static final double SHELFBAR_X = -64, SHELFBAR_Y = 714, SHELFBAR_W = 1728, SHELFBAR_H = 128;
    // the shelf platform itself: left:15cqw/top:-.4cqw/width:68cqw/height:8cqw of the desk box
    private static final double SHELF_X = 176, SHELF_Y = 714, SHELF_W = 1088, SHELF_H = 128;
    // lifeline bay module on the shelf: left:11.6cqw/top:1.75cqw/width:26.4cqw/height:6cqw of the shelf
    private static final double BAY_X = 362, BAY_Y = 742, BAY_W = 422, BAY_H = 96;
    // carrier plate inside the bay: left:.4cqw/top:.3cqw/width:22.8cqw/height:5.1cqw of the bay
    private static final double CARRIER_X = 368, CARRIER_Y = 747, CARRIER_W = 365, CARRIER_H = 82;
    // the three lifeline cells inside the carrier, shared top:1.65cqw/height:3.3cqw;
    // each cell's own left/width differs (bay module, not a uniform grid)
    private static final double LIFELINE_Y = 773, LIFELINE_H = 53;
    private static final double[] LIFELINE_X = { 382, 477, 590 }; // binary, predict, override
    private static final double[] LIFELINE_W = { 86, 106, 118 };
    // teletype housing on the shelf: left:40.6cqw/top:2.6cqw/width:12cqw/height:5cqw of the shelf
    private static final double TELETYPE_X = 826, TELETYPE_Y = 756, TELETYPE_W = 192, TELETYPE_H = 80;
    // the paper feed is a separate zone stacked above the housing (mockup's .paper:
    // bottom:calc(100% - .1cqw), max-height:9.5cqw), not an enlarged housing
    private static final double PAPER_H = 152;
    // switch on the shelf: left:54.6cqw/top:2.3cqw/width:11cqw/height:5.4cqw of the shelf
    private static final double SWITCH_X = 1050, SWITCH_Y = 751, SWITCH_W = 176, SWITCH_H = 86;
    // operator log + punch card, both direct children of the scene (not desk-nested):
    // log left:-1%/bottom:-6%/width:12%/height:21%; card left:12.5%/bottom:-1%/width:12%,
    // aspect 738:325
    private static final double LOG_X = -16, LOG_Y = 850, LOG_W = 192, LOG_H = 210;
    private static final double CARD_X = 200, CARD_Y = 925, CARD_W = 192, CARD_H = 85;

    private final CardPanel cardPanel;
    private final Color[] ladderOutcomes = new Color[TOTAL_QUESTIONS]; // null = not answered yet
    private final JButton[] lifelineButtons = new JButton[3];
    private final JButton[] keyButtons = new JButton[4];
    private final JButton nextButton;
    private final JButton switchButton;
    private final JButton backButton;
    private final List<String> paperLines = new ArrayList<>();
    private final List<Boolean> paperGood = new ArrayList<>();

    private GameEngine engine;
    private int questionIndex;
    private boolean sessionOver;
    // the board only pulls a fresh question on showQuestion() (start / Next click),
    // never mid-turn, so the just-answered prompt stays up until the player advances
    private Question displayedQuestion;
    private int displayedIndex;
    private double scale = 1;
    private double offsetX, offsetY;

    public ScenePanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);
        setBackground(WOOD_DARK);

        String[] lifelineLabels = { "BINARY", "PREDICT", "OVERRIDE" };
        for (int i = 0; i < lifelineButtons.length; i++) {
            JButton button = flatButton(lifelineLabels[i], FONT_LIFELINE, BRASS, PAPER);
            final int lifelineIndex = i;
            button.addActionListener(e -> pullLifeline(lifelineLabels[lifelineIndex]));
            lifelineButtons[i] = button;
            add(button);
        }

        String[] keyLabels = { "A", "B", "C", "D" };
        for (int i = 0; i < keyButtons.length; i++) {
            JButton button = flatButton(keyLabels[i], FONT_PROMPT, BRASS_LIT, WOOD_DARK);
            final int position = i;
            button.addActionListener(e -> submitAnswerAt(position));
            keyButtons[i] = button;
            add(button);
        }

        nextButton = flatButton("NEXT PROBE", FONT_LABEL, BRASS, PAPER);
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            if (sessionOver) {
                cardPanel.showScreen(CardPanel.MENU);
            } else {
                showQuestion();
            }
        });
        add(nextButton);

        switchButton = flatButton("TRAIN/SECURE", FONT_LABEL, BRASS, WOOD_DARK);
        switchButton.addActionListener(e -> pullSwitch());
        add(switchButton);

        backButton = flatButton("Back", FONT_LABEL, BRASS, PAPER);
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        add(backButton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });
    }

    private JButton flatButton(String text, Font font, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setMargin(new Insets(0, 2, 0, 2));
        return button;
    }

    // converts a logical (1600x1000) rect to screen pixels and positions c there
    private void place(Component c, double lx, double ly, double lw, double lh) {
        int x = (int) Math.round(offsetX + lx * scale);
        int y = (int) Math.round(offsetY + ly * scale);
        int w = (int) Math.round(lw * scale);
        int h = (int) Math.round(lh * scale);
        c.setBounds(x, y, w, h);
    }

    private void relayout() {
        scale = Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H);
        offsetX = (getWidth() - LOGICAL_W * scale) / 2;
        offsetY = (getHeight() - LOGICAL_H * scale) / 2;

        for (int i = 0; i < lifelineButtons.length; i++) {
            place(lifelineButtons[i], LIFELINE_X[i], LIFELINE_Y, LIFELINE_W[i], LIFELINE_H);
        }
        for (int i = 0; i < keyButtons.length; i++) {
            place(keyButtons[i], KEYS_X + i * (KEY_W + KEY_GAP), KEYS_Y, KEY_W, KEY_H);
        }
        place(nextButton, NEXT_X, NEXT_Y, NEXT_W, NEXT_H);
        place(switchButton, SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H);
        place(backButton, BACK_X, BACK_Y, BACK_W, BACK_H);
        repaint();
    }

    // starts rendering a session that's already been started on engine
    public void startGame(GameEngine engine) {
        this.engine = engine;
        this.questionIndex = 0;
        this.sessionOver = false;
        java.util.Arrays.fill(ladderOutcomes, null);
        paperLines.clear();
        paperGood.clear();
        print("BAY 04 LINK OPEN", true);
        print("AWAITING HUMAN VERIFIER", true);
        showQuestion();
    }

    private void showQuestion() {
        displayedQuestion = engine.currentQuestion();
        displayedIndex = questionIndex;
        for (JButton keyButton : keyButtons) {
            keyButton.setEnabled(true);
        }
        for (JButton lifelineButton : lifelineButtons) {
            lifelineButton.setEnabled(true);
        }
        switchButton.setEnabled(true);
        nextButton.setText("NEXT PROBE");
        nextButton.setEnabled(false);
        repaint();
    }

    private void pullLifeline(String label) {
        JButton source = null;
        for (JButton b : lifelineButtons) {
            if (b.getText().equals(label)) {
                source = b;
            }
        }
        if (sessionOver || source == null || !source.isEnabled()) {
            return;
        }
        source.setEnabled(false);
        print(label + ": NOT IN ENGINE YET", true);
    }

    private void pullSwitch() {
        if (sessionOver) {
            return;
        }
        sessionOver = true;
        for (JButton keyButton : keyButtons) {
            keyButton.setEnabled(false);
        }
        for (JButton lifelineButton : lifelineButtons) {
            lifelineButton.setEnabled(false);
        }
        switchButton.setEnabled(false);
        engine.endSession();
        print("BUNDLE WRITTEN TO TAPE", true);
        print("LINK CLOSED", true);
        nextButton.setText("BACK TO MENU");
        nextButton.setEnabled(true);
        repaint();
    }

    // submits whatever choice is actually shown at this display position right now
    // -- never a fixed letter. QuestionSource shuffles choice order per question
    // (CsvQuestionSource/CONTEXT.md 3), so the display position is not the choiceId;
    // same pattern PlayPanel.submitAnswerAt() already uses for the same reason.
    private void submitAnswerAt(int position) {
        String choiceId = displayedQuestion.getChoices().get(position).getChoiceId();
        for (JButton keyButton : keyButtons) {
            keyButton.setEnabled(false);
        }

        TurnResult result = engine.submitAnswer(choiceId);
        ladderOutcomes[questionIndex] = result.isCorrect() ? TAPE_GREEN : TAPE_RED;
        questionIndex++;
        sessionOver = result.isGameOver();

        if (result.isCorrect()) {
            print("P" + questionIndex + " VALIDATED +" + result.getTokensAwarded(), true);
            if (result.isCapabilityUnlocked()) {
                print(result.getCapabilityName().toUpperCase() + " RESTORED", true);
            }
        } else {
            print("P" + questionIndex + " REJECTED", false);
            print("UNCOMMITTED UNITS DISCARDED", false);
        }

        if (sessionOver) {
            for (JButton lifelineButton : lifelineButtons) {
                lifelineButton.setEnabled(false);
            }
            switchButton.setEnabled(false);
            nextButton.setText("BACK TO MENU");
        } else {
            nextButton.setText("NEXT PROBE");
        }
        nextButton.setEnabled(true);
        repaint();
    }

    private void print(String text, boolean good) {
        paperLines.add(text);
        paperGood.add(good);
        while (paperLines.size() > 5) {
            paperLines.remove(0);
            paperGood.remove(0);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        // background: the dead facility
        g2.setColor(TILE);
        g2.fillRect(0, 0, (int) LOGICAL_W, (int) LOGICAL_H);
        g2.setColor(CEILING);
        g2.fillRect(0, 0, (int) LOGICAL_W, (int) CEILING_H);
        g2.setColor(STEEL_DARK);
        double lampGap = (LAMPS_W - 6 * LAMP_W) / 5;
        for (int i = 0; i < 6; i++) {
            g2.fillRect((int) (LAMPS_X + i * (LAMP_W + lampGap)), (int) LAMPS_Y, (int) LAMP_W, (int) LAMP_H);
        }
        g2.setColor(MINT);
        g2.fillRect((int) CAB_L_X, (int) CAB_L_Y, (int) CAB_L_W, (int) CAB_L_H);
        g2.setColor(STEEL);
        g2.fillRect((int) CAB_R_X, (int) CAB_R_Y, (int) CAB_R_W, (int) CAB_R_H);

        // midground: the salvaged training station
        g2.setColor(PAPER);
        g2.fillRect((int) STATION_X, (int) STATION_Y, (int) STATION_W, (int) STATION_H);

        paintLadder(g2);

        g2.setColor(SLATE);
        g2.fillRect((int) BOARD_X, (int) BOARD_Y, (int) BOARD_W, (int) BOARD_H);
        paintBoardContent(g2);

        g2.setColor(WOOD_DARK);
        g2.fillRect((int) LEDGE_X, (int) LEDGE_Y, (int) LEDGE_W, (int) LEDGE_H);

        // foreground: operator's desk, faked perspective since java2d has no true tilt
        g2.setColor(WOOD_DARK);
        Polygon desk = new Polygon();
        desk.addPoint(140, 720);
        desk.addPoint(1460, 720);
        desk.addPoint(1664, 1000);
        desk.addPoint(-64, 1000);
        g2.fillPolygon(desk);

        // decorative backing panel behind the keybank/next row
        g2.setColor(WOOD_DARK);
        g2.fillRect((int) DECK_X, (int) DECK_Y, (int) DECK_W, (int) DECK_H);

        // raised rear shelf: a field-fitted bypass carrier for the lifelines, plus
        // the teletype and train/secure switch housing
        g2.setColor(BRASS);
        g2.fillRect((int) SHELFBAR_X, (int) SHELFBAR_Y, (int) SHELFBAR_W, (int) SHELFBAR_H);
        g2.setColor(WOOD_DARK);
        g2.fillRect((int) SHELF_X, (int) SHELF_Y, (int) SHELF_W, (int) SHELF_H);

        g2.setColor(SHELF_WELL);
        g2.fillRect((int) BAY_X, (int) BAY_Y, (int) BAY_W, (int) BAY_H);
        g2.setColor(STEEL);
        g2.fillRect((int) CARRIER_X, (int) CARRIER_Y, (int) CARRIER_W, (int) CARRIER_H);
        for (int i = 0; i < LIFELINE_X.length; i++) {
            g2.setColor(SHELF_WELL);
            g2.fillRect((int) LIFELINE_X[i] - 4, (int) LIFELINE_Y - 4, (int) LIFELINE_W[i] + 8, (int) LIFELINE_H + 8);
        }

        g2.setColor(BRASS_OX);
        g2.fillRect((int) TELETYPE_X, (int) TELETYPE_Y, (int) TELETYPE_W, (int) TELETYPE_H);
        paintPaper(g2);
        g2.setColor(CHALK);
        g2.setFont(FONT_LADDER);
        g2.drawString("RECORD", (int) (TELETYPE_X + TELETYPE_W / 2 - 20), (int) (TELETYPE_Y + TELETYPE_H - 6));

        paintCard(g2);
        paintLog(g2);

        g2.setColor(BRASS);
        g2.fillRoundRect((int) MONITOR_X, (int) MONITOR_Y, (int) MONITOR_W, (int) MONITOR_H, 24, 24);
        g2.setColor(new Color(0x0e, 0x1d, 0x14));
        g2.fillRoundRect((int) MONITOR_X + 14, (int) MONITOR_Y + 14, (int) MONITOR_W - 28, (int) MONITOR_H - 28, 14, 14);

        g2.dispose();
    }

    // low-detail props, both direct children of the scene (not desk-nested); the
    // punch card's digit-grid and the log's full flavor text stay flat/no-texture
    // here, same line the project already holds until a later texture pass
    private void paintCard(Graphics2D g2) {
        g2.setColor(PAPER);
        g2.fillRoundRect((int) CARD_X, (int) CARD_Y, (int) CARD_W, (int) CARD_H, 10, 10);
    }

    private void paintLog(Graphics2D g2) {
        g2.setColor(PAPER);
        g2.fillRect((int) LOG_X, (int) LOG_Y, (int) LOG_W, (int) LOG_H);
        g2.setColor(WOOD_DARK);
        g2.setFont(FONT_LADDER);
        String[] lines = {
                "OPERATOR LOG 042", "14 OCT 1963", "Relay power stable.",
                "Core still requests", "a human verifier.", "Do not leave the", "link unattended." };
        int ly = (int) LOG_Y + 20;
        for (String line : lines) {
            g2.drawString(line, (int) LOG_X + 10, ly);
            ly += 16;
        }
    }

    private void paintLadder(Graphics2D g2) {
        double rungW = (LADDER_W - (TOTAL_QUESTIONS - 1) * LADDER_GAP) / TOTAL_QUESTIONS;
        for (int i = 0; i < TOTAL_QUESTIONS; i++) {
            Color outcome = ladderOutcomes[i];
            Color fill = outcome != null ? outcome : i == questionIndex ? BRASS_LIT : CHALK_YELLOW.darker();
            double x = LADDER_X + i * (rungW + LADDER_GAP);
            g2.setColor(fill);
            g2.fillRect((int) x, (int) LADDER_Y, (int) rungW, (int) LADDER_H);
        }
    }

    private void paintBoardContent(Graphics2D g2) {
        Question question = displayedQuestion;
        if (question == null) {
            return;
        }
        g2.setColor(CHALK);
        g2.setFont(FONT_LABEL);
        g2.drawString(question.getModule().toUpperCase() + " / " + question.getBloom().toUpperCase(),
                (int) BOARD_X + 24, (int) BOARD_Y + 30);
        g2.drawString("PROBE " + String.format("%02d", displayedIndex + 1) + " / " + TOTAL_QUESTIONS,
                (int) (BOARD_X + BOARD_W) - 180, (int) BOARD_Y + 30);

        g2.setFont(FONT_PROMPT);
        drawWrapped(g2, question.getText(), (int) BOARD_X + 24, (int) BOARD_Y + 70, (int) BOARD_W - 48, 26);

        g2.setFont(FONT_ANSWER);
        List<Choice> choices = question.getChoices();
        int rowY = (int) BOARD_Y + 200;
        for (int i = 0; i < choices.size(); i++) {
            g2.setColor(CHALK_YELLOW);
            String letter = "ABCD".charAt(i) + ". ";
            g2.drawString(letter, (int) BOARD_X + 24 + (i % 2) * (int) (BOARD_W / 2), rowY + (i / 2) * 50);
            g2.setColor(CHALK);
            g2.drawString(choices.get(i).getText(), (int) BOARD_X + 24 + 30 + (i % 2) * (int) (BOARD_W / 2),
                    rowY + (i / 2) * 50);
        }
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int curY = y;
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                g2.setColor(CHALK);
                g2.drawString(line.toString(), x, curY);
                curY += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            g2.setColor(CHALK);
            g2.drawString(line.toString(), x, curY);
        }
    }

    // the printed strip feeds up out of the housing's top edge into its own taller
    // zone above it (mockup's .paper), not inside the compact housing itself
    private void paintPaper(Graphics2D g2) {
        g2.setFont(FONT_PAPER);
        int y = (int) TELETYPE_Y - 8;
        for (int i = paperLines.size() - 1; i >= 0; i--) {
            g2.setColor(paperGood.get(i) ? TAPE_GREEN : TAPE_RED);
            g2.drawString(paperLines.get(i), (int) TELETYPE_X + 10, y);
            y -= 14;
        }
    }

    // manual visual check only, click through with a stub GameEngine, same as PlayPanel's.
    public static void main(String[] args) throws Exception {
        // aqua (macos) ignores custom JButton colors/borders otherwise
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        List<Question> questions = new FakeQuestionSource().buildSession();

        GameEngine demoEngine = new GameEngine() {
            int index = 0;
            int total = 0;

            @Override
            public void startSession(String trainerName, Protocol protocol) {
                index = 0;
                total = 0;
            }

            @Override
            public Question currentQuestion() {
                return questions.get(index);
            }

            @Override
            public TurnResult submitAnswer(String choiceId) {
                Question question = questions.get(index);
                boolean correct = question.isCorrect(choiceId);
                int tokens = correct ? (index + 1) * 10 : 0;
                total += tokens;
                index++;
                boolean gameOver = !correct || index >= questions.size();
                boolean capabilityUnlocked = correct
                        && (index == 3 || index == 5 || index == 8 || index == 10 || index == 13 || index == 15);
                return new TurnResult(correct, tokens, total, capabilityUnlocked,
                        capabilityUnlocked ? "Capability " + index : null, gameOver);
            }

            @Override
            public SessionResult endSession() {
                return new SessionResult();
            }
        };

        JFrame frame = new JFrame("ScenePanel demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ScenePanel panel = new ScenePanel(new CardPanel());
        frame.add(panel);
        frame.setSize(1600, 1000);
        frame.setVisible(true);
        panel.startGame(demoEngine);
    }
}
