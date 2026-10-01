package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.Lifeline;
import com.howtotrainyourai.engine.LifelineResult;
import com.howtotrainyourai.engine.TurnResult;
import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// blocking pass for the vintage ai lab scene, see
// ~/dev/Java/CMSC170/reference-photos/restoration-bay-fold-claude-table.html for the
// frozen mockup this tracks (not the earlier drafts -- restoration-bay-fold.html's
// top-right lifeline panel and chalkboard-ledge NEXT button are superseded). same seam
// as PlayPanel: ScenePanel(CardPanel), startGame(GameEngine). PlayPanel stays the
// working fallback until this is swapped in at Main.java after the oct 4 playthrough.
//
// flat shapes only, no gradients/textures. arm sweep is a later phase, not here.
public class ScenePanel extends JPanel implements GameScreen {

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
    // mockup .lmark.failed / .lmark.latched literal colors (not css vars there either)
    private static final Color MARKER_FAIL = Color.decode("#c97a62");
    private static final Color MARKER_LATCH = Color.decode("#f4e6a8");
    private static final Color COLD_LIGHT = Color.decode("#cfeee2");

    // capability unlock order, matches GameEngineImpl.CAPABILITY_UNLOCKS (Q3/5/8/10/13/15)
    private static final String[] CAPABILITY_NAMES = {
            "MEMORY", "UNDERSTANDING", "APPLICATION", "ANALYSIS", "EVALUATION", "SYNTHESIZE" };

    // lifelineButtons[i] <-> this enum, same order as the mockup's BINARY/PREDICT/
    // OVERRIDE cells. OVERRIDE is display-only, same as PlayPanel.LIFELINE_ORDER --
    // useLifeline(OVERRIDE) throws by design, the engine spends it on its own.
    private static final Lifeline[] LIFELINE_ORDER = {
            Lifeline.BINARY_CHOICE, Lifeline.PREDICT, Lifeline.OVERRIDE };

    // mockup --phosphor / --phosphor-warn, plus the dvd-bounce TINTS array
    private static final Color PHOSPHOR = Color.decode("#86e8a2");
    private static final Color PHOSPHOR_WARN = Color.decode("#e9a263");
    private static final Color[] IDLE_TINTS = {
            PHOSPHOR, Color.decode("#e9c46a"), Color.decode("#9fe3f0"), PHOSPHOR_WARN };
    private static final double FACE_W = 70, FACE_H = 46;
    private static final int IDLE_MS = 12000;
    private static final int MONITOR_TICK_MS = 50;
    private static final int HEART_RATE_TICK_MS = 900;
    private static final double BLINK_CYCLE_MS = 4600;

    // face-led identity ported from PlayPanel.AiMonitorPanel, extended with the
    // mockup's state/blink/idle-bounce behavior rather than redesigned toward its
    // separate instrument-like look (see corpus "Fold pass - user decisions")
    private enum MonitorState { LISTENING, HAPPY, ALERT, CLOSED, IDLE }

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
    private static final double MARKER_H = 18, MARKER_GAP = 6;
    private static final int MARKER_ANIM_MS = 450;
    private static final int MARKER_ROLLBACK_MS = 900;
    private static final double BOARD_X = 208, BOARD_Y = 205, BOARD_W = 1136, BOARD_H = 440;
    private static final double LEDGE_X = 195, LEDGE_Y = 673, LEDGE_W = 1162, LEDGE_H = 24;
    private static final int LINE_HEIGHT = 14;
    private static final int PAPER_FEED_MS = 180;
    private static final double MONITOR_X = 1288, MONITOR_Y = 610, MONITOR_W = 272, MONITOR_H = 270;
    private static final double BACK_X = 20, BACK_Y = 20, BACK_W = 80, BACK_H = 30;

    // desk container: left:-4%/right:-4%/bottom:-6%/height:34% of the 1600x1000 canvas
    private static final double DESK_X = -64, DESK_Y = 720, DESK_W = 1728, DESK_H = 340;
    // keybank: left:31%/bottom:30% of the desk box (not 25%/40% -- that was the
    // superseded draft's position)
    private static final double KEYS_X = 472, KEYS_Y = 862, KEY_W = 115, KEY_H = 96, KEY_GAP = 21;
    private static final double KEY_DEPRESS = 10;
    // next probe: left:68.2cqw/bottom:6.4cqw of the desk box -- moved into the answer
    // row per the handoff's physical-reach fix, lands in the same row as the keys
    private static final double NEXT_X = 1027, NEXT_Y = 862, NEXT_W = 144, NEXT_H = 96;
    // decorative backing panel behind the keybank/next row: left:31.5cqw/bottom:5.3cqw
    // of the desk box, width 47.5cqw, height 8.2cqw
    private static final double DECK_X = 440, DECK_Y = 844, DECK_W = 760, DECK_H = 131;
    // raised rear shelf backing strip, full desk width: top:-.4cqw/height:8cqw of the
    // desk box. it's two layers, not one solid block: a thin brass top lip
    // (.shelfbar .top, 1.6cqw) over a wood-dark facade (.shelfbar .facade) that
    // blends into the desk -- filling the whole strip brass left a stray block
    // showing above the operator log where the mockup shows nothing
    private static final double SHELFBAR_X = -64, SHELFBAR_Y = 714, SHELFBAR_W = 1728, SHELFBAR_H = 128;
    private static final double SHELFBAR_LIP_H = 26;
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
    // mockup rotate() transforms on elements NOT nested under .desk (so unrelated to
    // the taper field below): .monitor 3deg, .log -6deg, .card 8deg
    private static final double MONITOR_ROTATE_DEG = 3, LOG_ROTATE_DEG = -6, CARD_ROTATE_DEG = 8;

    // approximates .desk's perspective(60cqw) rotateX(17deg) (java2d has no true 3d
    // transform) by reusing the desk polygon's own established taper below: narrower
    // at the top (y=720, width 1320), wider at the bottom (y=1000, width 1728), both
    // centered on x=800. every desk-row shape/button interpolates against this same
    // field so the whole assembly tapers consistently, not just the background.
    private static final double DESK_TOP_Y = 720, DESK_BOTTOM_Y = 1000;
    private static final double DESK_TOP_W = 1320, DESK_BOTTOM_W = 1728;
    private static final double DESK_CENTER_X = 800;

    private double deskWidthAt(double y) {
        double t = (y - DESK_TOP_Y) / (DESK_BOTTOM_Y - DESK_TOP_Y);
        return DESK_TOP_W + t * (DESK_BOTTOM_W - DESK_TOP_W);
    }

    // an untapered desk-relative rect (x,y,w,h) -- as already computed from the
    // mockup's raw css percentages against the full DESK_X/DESK_W box -- to the 4
    // screen corners of the trapezoid it occupies once the desk's taper is applied.
    // {topLeftX, topRightX, bottomLeftX, bottomRightX}; y's are unchanged (x+w, y+h).
    private double[] taperedQuad(double x, double y, double w, double h) {
        double leftFrac = (x - DESK_X) / DESK_W;
        double rightFrac = (x + w - DESK_X) / DESK_W;
        double topWidth = deskWidthAt(y);
        double bottomWidth = deskWidthAt(y + h);
        double topLeftX = DESK_CENTER_X - topWidth / 2 + leftFrac * topWidth;
        double topRightX = DESK_CENTER_X - topWidth / 2 + rightFrac * topWidth;
        double bottomLeftX = DESK_CENTER_X - bottomWidth / 2 + leftFrac * bottomWidth;
        double bottomRightX = DESK_CENTER_X - bottomWidth / 2 + rightFrac * bottomWidth;
        return new double[] { topLeftX, topRightX, bottomLeftX, bottomRightX };
    }

    private Polygon taperedPolygon(double x, double y, double w, double h) {
        double[] quad = taperedQuad(x, y, w, h);
        Polygon p = new Polygon();
        p.addPoint((int) quad[0], (int) y);
        p.addPoint((int) quad[1], (int) y);
        p.addPoint((int) quad[3], (int) (y + h));
        p.addPoint((int) quad[2], (int) (y + h));
        return p;
    }

    // wraps a shape that isn't a .desk child (monitor/log/card) in a plain 2d
    // rotation around its own center -- unrelated to the taper field above
    private void paintRotated(Graphics2D g2, double cx, double cy, double degrees, Consumer<Graphics2D> paint) {
        Graphics2D g2r = (Graphics2D) g2.create();
        g2r.rotate(Math.toRadians(degrees), cx, cy);
        paint.accept(g2r);
        g2r.dispose();
    }

    private final CardPanel cardPanel;
    private final Color[] ladderOutcomes = new Color[TOTAL_QUESTIONS]; // null = not answered yet
    private final TaperedButton[] lifelineButtons = new TaperedButton[3];
    private final TaperedButton[] keyButtons = new TaperedButton[4];
    private final TaperedButton nextButton;
    private final SwitchButton switchButton;
    private final JButton backButton;
    private final List<String> paperLines = new ArrayList<>();
    private final List<Boolean> paperGood = new ArrayList<>();
    private Timer paperFeedTimer;
    private double paperFeedT = 1;

    private GameEngine engine;
    private int questionIndex;
    private boolean sessionOver;
    // brass clip that tracks progress on the ladder, see paintLadderMarker()
    private double markerPos;
    private double markerAnimStart;
    private double markerAnimFrom;
    private double markerTarget;
    private boolean markerFailed;
    private boolean markerLatched;
    private Timer markerAnimTimer;
    private Timer markerRollbackTimer;
    // index of the key that was pressed to answer the current question, -1 if none
    private int selectedKeyIndex = -1;
    private final boolean[] lampsOn = new boolean[CAPABILITY_NAMES.length];

    // ai vitals monitor state, see paintMonitor()/tickMonitor()
    private MonitorState monitorState = MonitorState.LISTENING;
    private boolean waitingForAnswer;
    private int heartRate = 72;
    private double waveOffset;
    private boolean eyesClosed;
    private double faceX = (MONITOR_W - 28) / 2.0 - FACE_W / 2.0;
    private double faceY = (MONITOR_H - 28) / 2.0 - FACE_H / 2.0;
    private double faceVX = 1, faceVY = 0.72;
    private int idleTintIndex;
    private Color idleTint = IDLE_TINTS[0];
    private Timer monitorTimer;
    private Timer heartRateTimer;
    private Timer idleTimer;
    // the board only pulls a fresh question on showQuestion() (start / Next click),
    // never mid-turn, so the just-answered prompt stays up until the player advances
    private Question displayedQuestion;
    private int displayedIndex;
    // chalk marks on the board: null/false until the displayed question is answered
    private String displayedAnsweredChoiceId;
    private String displayedCorrectChoiceId;
    private boolean displayedWasCorrect;
    // lifeline effects on the CURRENT question only -- cleared in showQuestion(),
    // same convention as PlayPanel.removedChoiceIds (each lifeline is spent once
    // per session, but what it revealed only applies to the question it was used on)
    private final Set<String> removedChoiceIds = new HashSet<>();
    private String predictedChoiceId;
    private int predictedConfidence;
    private double scale = 1;
    private double offsetX, offsetY;

    public ScenePanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setLayout(null);
        setBackground(WOOD_DARK);

        String[] lifelineLabels = { "BINARY", "PREDICT", "OVERRIDE" };
        for (int i = 0; i < lifelineButtons.length; i++) {
            TaperedButton button = new TaperedButton(lifelineLabels[i], FONT_LIFELINE, BRASS, PAPER);
            Lifeline lifeline = LIFELINE_ORDER[i];
            if (lifeline == Lifeline.OVERRIDE) {
                // spent automatically on the first wrong answer, never clicked --
                // same treatment as PlayPanel's "OV" button
                button.setEnabled(false);
            } else {
                button.addActionListener(e -> pullLifeline(lifeline));
            }
            lifelineButtons[i] = button;
            add(button);
        }

        String[] keyLabels = { "A", "B", "C", "D" };
        for (int i = 0; i < keyButtons.length; i++) {
            TaperedButton button = new TaperedButton(keyLabels[i], FONT_PROMPT, BRASS_LIT, WOOD_DARK);
            final int position = i;
            button.addActionListener(e -> submitAnswerAt(position));
            keyButtons[i] = button;
            add(button);
        }

        nextButton = new TaperedButton("NEXT PROBE", FONT_LABEL, BRASS, PAPER);
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            if (sessionOver) {
                cardPanel.showScreen(CardPanel.MENU);
            } else {
                showQuestion();
            }
        });
        add(nextButton);

        switchButton = new SwitchButton();
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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                wakeMonitor();
            }
        });

        monitorTimer = new Timer(MONITOR_TICK_MS, e -> tickMonitor());
        monitorTimer.start();
        heartRateTimer = new Timer(HEART_RATE_TICK_MS, e -> tickHeartRate());
        heartRateTimer.start();
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

    // a control painted via TaperedButton/SwitchButton's shared trapezoid clip
    private interface Tapered {
        void setTaperInsets(double left, double right);
    }

    // positions a desk-row control at its tapered trapezoid (taperedQuad()): swing
    // bounds become the bounding box (hit-testing stays rectangular), and the
    // control's own paint clips inward to the actual trapezoid via scaled insets
    private void placeTapered(JComponent button, double x, double y, double w, double h) {
        double[] quad = taperedQuad(x, y, w, h);
        double boundLeft = Math.min(quad[0], quad[2]);
        double boundRight = Math.max(quad[1], quad[3]);
        place(button, boundLeft, y, boundRight - boundLeft, h);
        ((Tapered) button).setTaperInsets((quad[0] - boundLeft) * scale, (boundRight - quad[1]) * scale);
    }

    private void relayout() {
        scale = Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H);
        offsetX = (getWidth() - LOGICAL_W * scale) / 2;
        offsetY = (getHeight() - LOGICAL_H * scale) / 2;

        for (int i = 0; i < lifelineButtons.length; i++) {
            placeTapered(lifelineButtons[i], LIFELINE_X[i], LIFELINE_Y, LIFELINE_W[i], LIFELINE_H);
        }
        for (int i = 0; i < keyButtons.length; i++) {
            double y = KEYS_Y + (i == selectedKeyIndex ? KEY_DEPRESS : 0);
            placeTapered(keyButtons[i], KEYS_X + i * (KEY_W + KEY_GAP), y, KEY_W, KEY_H);
        }
        placeTapered(nextButton, NEXT_X, NEXT_Y, NEXT_W, NEXT_H);
        placeTapered(switchButton, SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H);
        place(backButton, BACK_X, BACK_Y, BACK_W, BACK_H);
        repaint();
    }

    // starts rendering a session that's already been started on engine
    @Override
    public void startGame(GameEngine engine) {
        this.engine = engine;
        this.questionIndex = 0;
        this.sessionOver = false;
        java.util.Arrays.fill(ladderOutcomes, null);
        paperLines.clear();
        paperGood.clear();
        if (markerAnimTimer != null) {
            markerAnimTimer.stop();
        }
        if (markerRollbackTimer != null) {
            markerRollbackTimer.stop();
        }
        if (paperFeedTimer != null) {
            paperFeedTimer.stop();
        }
        paperFeedT = 1;
        markerPos = 0;
        markerTarget = 0;
        markerFailed = false;
        markerLatched = false;
        switchButton.turned = false;
        java.util.Arrays.fill(lampsOn, false);
        if (idleTimer != null) {
            idleTimer.stop();
        }
        monitorState = MonitorState.LISTENING;
        heartRate = 72;
        faceX = (MONITOR_W - 28) / 2.0 - FACE_W / 2.0;
        faceY = (MONITOR_H - 28) / 2.0 - FACE_H / 2.0;
        print("BAY 04 LINK OPEN", true);
        print("AWAITING HUMAN VERIFIER", true);
        showQuestion();
    }

    private void showQuestion() {
        displayedQuestion = engine.currentQuestion();
        displayedIndex = questionIndex;
        removedChoiceIds.clear();
        predictedChoiceId = null;
        for (JButton keyButton : keyButtons) {
            keyButton.setEnabled(true);
            keyButton.setBackground(BRASS_LIT);
        }
        refreshLifelines();
        switchButton.setEnabled(true);
        nextButton.setText("NEXT PROBE");
        nextButton.setEnabled(false);
        selectedKeyIndex = -1;
        displayedAnsweredChoiceId = null;
        displayedCorrectChoiceId = null;
        monitorState = MonitorState.LISTENING;
        waitingForAnswer = true;
        scheduleIdle();
        relayout();
    }

    /**
     * Enabled state comes from the engine's remaining set, never a local flag --
     * High Risk never grants Override, and a spent lifeline must stay spent
     * across questions. Same convention as PlayPanel.refreshLifelines().
     */
    private void refreshLifelines() {
        Set<Lifeline> remaining = engine.getRemainingLifelines();
        for (int i = 0; i < lifelineButtons.length; i++) {
            Lifeline lifeline = LIFELINE_ORDER[i];
            boolean clickable = remaining.contains(lifeline) && !sessionOver && lifeline != Lifeline.OVERRIDE;
            lifelineButtons[i].setEnabled(clickable);
        }
    }

    private void pullLifeline(Lifeline lifeline) {
        if (sessionOver || !engine.getRemainingLifelines().contains(lifeline)) {
            // stale click: the button is disabled whenever this would be true, but
            // a defensive check here beats a stack trace from useLifeline()
            refreshLifelines();
            return;
        }
        wakeMonitor();
        LifelineResult result = engine.useLifeline(lifeline);
        if (lifeline == Lifeline.BINARY_CHOICE) {
            removedChoiceIds.addAll(result.getRemovedChoiceIds());
            List<Choice> choices = displayedQuestion.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                if (removedChoiceIds.contains(choices.get(i).getChoiceId())) {
                    keyButtons[i].setEnabled(false);
                }
            }
            print("BINARY CHOICE: TWO WRONG UNITS PURGED", true);
        } else {
            predictedChoiceId = result.getPredictedChoiceId();
            predictedConfidence = result.getConfidencePercent();
            int position = positionOfChoiceId(predictedChoiceId);
            String letter = position >= 0 ? String.valueOf("ABCD".charAt(position)) : "?";
            print("PREDICT: AI SUGGESTS " + letter + " AT " + predictedConfidence + "%", true);
        }
        refreshLifelines();
        repaint();
    }

    /** Display position of a choiceId in the currently displayed question, or -1. */
    private int positionOfChoiceId(String choiceId) {
        List<Choice> choices = displayedQuestion.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).getChoiceId().equals(choiceId)) {
                return i;
            }
        }
        return -1;
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
        switchButton.turned = true;
        engine.endSession();
        markerLatched = !markerFailed;
        waitingForAnswer = false;
        if (idleTimer != null) {
            idleTimer.stop();
        }
        monitorState = MonitorState.CLOSED;
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
        selectedKeyIndex = position;
        keyButtons[selectedKeyIndex].setBackground(BRASS_LIT.darker());
        waitingForAnswer = false;
        if (idleTimer != null) {
            idleTimer.stop();
        }

        int answeredRung = questionIndex;
        TurnResult result = engine.submitAnswer(choiceId);
        if (result.isRetry()) {
            // Override absorbed the miss. The engine keeps the same question and
            // local ladder state; re-arm only the answer keys for the retry.
            print("OVERRIDE ENGAGED: RETRY AUTHORIZED", true);
            for (JButton keyButton : keyButtons) {
                keyButton.setEnabled(true);
                keyButton.setBackground(BRASS_LIT);
            }
            selectedKeyIndex = -1;
            waitingForAnswer = true;
            monitorState = MonitorState.LISTENING;
            scheduleIdle();
            repaint();
            return;
        }
        ladderOutcomes[questionIndex] = result.isCorrect() ? TAPE_GREEN : TAPE_RED;
        questionIndex++;
        sessionOver = result.isGameOver();

        displayedAnsweredChoiceId = choiceId;
        displayedWasCorrect = result.isCorrect();
        for (Choice choice : displayedQuestion.getChoices()) {
            if (displayedQuestion.isCorrect(choice.getChoiceId())) {
                displayedCorrectChoiceId = choice.getChoiceId();
            }
        }

        if (result.isCorrect()) {
            print("P" + questionIndex + " VALIDATED +" + result.getTokensAwarded(), true);
            if (result.isCapabilityUnlocked()) {
                String capability = result.getCapabilityName().toUpperCase();
                print(capability + " RESTORED", true);
                for (int lampIndex = 0; lampIndex < CAPABILITY_NAMES.length; lampIndex++) {
                    if (CAPABILITY_NAMES[lampIndex].equals(capability)) {
                        lampsOn[lampIndex] = true;
                    }
                }
            }
            markerFailed = false;
            animateMarkerTo(Math.min(questionIndex, TOTAL_QUESTIONS - 1));
            monitorState = MonitorState.HAPPY;
        } else {
            print("P" + questionIndex + " REJECTED", false);
            print("UNCOMMITTED UNITS DISCARDED", false);
            markerFailed = true;
            animateMarkerTo(answeredRung);
            scheduleMarkerRollback(answeredRung);
            monitorState = MonitorState.ALERT;
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
        relayout();
    }

    private void print(String text, boolean good) {
        paperLines.add(text);
        paperGood.add(good);
        // generous ceiling against unbounded growth, not a visual-capacity cap --
        // paintPaper() decides how many lines actually show
        while (paperLines.size() > 200) {
            paperLines.remove(0);
            paperGood.remove(0);
        }

        if (paperFeedTimer != null) {
            paperFeedTimer.stop();
        }
        paperFeedT = 0;
        long feedStart = System.currentTimeMillis();
        paperFeedTimer = new Timer(16, e -> {
            paperFeedT = Math.min(1, (System.currentTimeMillis() - feedStart) / (double) PAPER_FEED_MS);
            repaint();
            if (paperFeedT >= 1) {
                paperFeedTimer.stop();
            }
        });
        paperFeedTimer.start();
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
        paintLamps(g2);
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
        g2.fillPolygon(taperedPolygon(DECK_X, DECK_Y, DECK_W, DECK_H));

        // raised rear shelf: thin brass top lip over a wood-dark facade (see
        // SHELFBAR_LIP_H comment), then the shelf platform holding the lifeline
        // bay/carrier, teletype, and switch -- every piece tapers with the desk
        g2.setColor(BRASS);
        g2.fillPolygon(taperedPolygon(SHELFBAR_X, SHELFBAR_Y, SHELFBAR_W, SHELFBAR_LIP_H));
        g2.setColor(WOOD_DARK);
        g2.fillPolygon(taperedPolygon(SHELFBAR_X, SHELFBAR_Y + SHELFBAR_LIP_H, SHELFBAR_W, SHELFBAR_H - SHELFBAR_LIP_H));
        g2.fillPolygon(taperedPolygon(SHELF_X, SHELF_Y, SHELF_W, SHELF_H));

        g2.setColor(SHELF_WELL);
        g2.fillPolygon(taperedPolygon(BAY_X, BAY_Y, BAY_W, BAY_H));
        g2.setColor(STEEL);
        g2.fillPolygon(taperedPolygon(CARRIER_X, CARRIER_Y, CARRIER_W, CARRIER_H));
        for (int i = 0; i < LIFELINE_X.length; i++) {
            g2.setColor(SHELF_WELL);
            g2.fillPolygon(taperedPolygon(LIFELINE_X[i] - 4, LIFELINE_Y - 4, LIFELINE_W[i] + 8, LIFELINE_H + 8));
        }

        g2.setColor(BRASS_OX);
        g2.fillPolygon(taperedPolygon(TELETYPE_X, TELETYPE_Y, TELETYPE_W, TELETYPE_H));
        paintPaper(g2);
        g2.setColor(CHALK);
        g2.setFont(FONT_LADDER);
        g2.drawString("RECORD", (int) (TELETYPE_X + TELETYPE_W / 2 - 20), (int) (TELETYPE_Y + TELETYPE_H - 6));

        // monitor/log/card are siblings of .desk, not children -- plain 2d rotations
        // around their own centers, unrelated to the taper field above
        paintRotated(g2, CARD_X + CARD_W / 2, CARD_Y + CARD_H / 2, CARD_ROTATE_DEG, this::paintCard);
        paintRotated(g2, LOG_X + LOG_W / 2, LOG_Y + LOG_H / 2, LOG_ROTATE_DEG, this::paintLog);
        paintRotated(g2, MONITOR_X + MONITOR_W / 2, MONITOR_Y + MONITOR_H / 2, MONITOR_ROTATE_DEG, g2m -> {
            g2m.setColor(BRASS);
            g2m.fillRoundRect((int) MONITOR_X, (int) MONITOR_Y, (int) MONITOR_W, (int) MONITOR_H, 24, 24);
            g2m.setColor(new Color(0x0e, 0x1d, 0x14));
            g2m.fillRoundRect((int) MONITOR_X + 14, (int) MONITOR_Y + 14, (int) MONITOR_W - 28, (int) MONITOR_H - 28, 14, 14);
            paintMonitor(g2m);
        });

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

    // one lamp per capability, lighting up as GameEngineImpl reports an unlock.
    // no glow cone/gradient here, that's phase-4 texture territory
    private void paintLamps(Graphics2D g2) {
        double lampGap = (LAMPS_W - CAPABILITY_NAMES.length * LAMP_W) / (CAPABILITY_NAMES.length - 1);
        g2.setFont(FONT_LADDER);
        for (int i = 0; i < CAPABILITY_NAMES.length; i++) {
            double x = LAMPS_X + i * (LAMP_W + lampGap);
            g2.setColor(lampsOn[i] ? COLD_LIGHT : STEEL_DARK);
            g2.fillRect((int) x, (int) LAMPS_Y, (int) LAMP_W, (int) LAMP_H);
            g2.setColor(lampsOn[i] ? CHALK : STEEL_DARK.brighter());
            g2.drawString(CAPABILITY_NAMES[i], (int) x, (int) (LAMPS_Y + LAMP_H + 14));
        }
    }

    // (re)starts the 12s countdown to the idle dvd-bounce; only meaningful while
    // waitingForAnswer, called again every time showQuestion()/wakeMonitor() applies
    private void scheduleIdle() {
        if (idleTimer != null) {
            idleTimer.stop();
        }
        idleTimer = new Timer(IDLE_MS, e -> {
            if (waitingForAnswer && !sessionOver) {
                monitorState = MonitorState.IDLE;
            }
        });
        idleTimer.setRepeats(false);
        idleTimer.start();
    }

    // ports the mockup's wake(): any press resets the idle clock and, if the
    // monitor had drifted into its bounce, brings it back to listening
    private void wakeMonitor() {
        if (monitorState == MonitorState.IDLE) {
            monitorState = MonitorState.LISTENING;
        }
        if (waitingForAnswer && !sessionOver) {
            scheduleIdle();
        }
    }

    // ~20fps: advances the sine-wave scroll and either the idle dvd-bounce physics
    // or a gentle settle-to-center bob, plus the time-based blink flag
    private void tickMonitor() {
        double dtSec = MONITOR_TICK_MS / 1000.0;
        waveOffset += (monitorState == MonitorState.ALERT ? 220 : 90) * dtSec;

        long now = System.currentTimeMillis();
        double blinkPhase = (now % BLINK_CYCLE_MS) / BLINK_CYCLE_MS;
        eyesClosed = blinkPhase > 0.93 && blinkPhase < 0.95;

        double innerW = MONITOR_W - 28, innerH = MONITOR_H - 28;
        double minX = 6, maxX = innerW - FACE_W - 6;
        double minY = 28, maxY = innerH - FACE_H - 10;

        if (monitorState == MonitorState.IDLE) {
            faceX += faceVX * 60 * dtSec;
            faceY += faceVY * 44 * dtSec;
            boolean hit = false;
            if (faceX < minX) {
                faceX = minX;
                faceVX = Math.abs(faceVX);
                hit = true;
            }
            if (faceX > maxX) {
                faceX = maxX;
                faceVX = -Math.abs(faceVX);
                hit = true;
            }
            if (faceY < minY) {
                faceY = minY;
                faceVY = Math.abs(faceVY);
                hit = true;
            }
            if (faceY > maxY) {
                faceY = maxY;
                faceVY = -Math.abs(faceVY);
                hit = true;
            }
            if (hit) {
                idleTintIndex = (idleTintIndex + 1) % IDLE_TINTS.length;
                idleTint = IDLE_TINTS[idleTintIndex];
            }
        } else {
            double targetX = (minX + maxX) / 2;
            double targetY = (minY + maxY) / 2 + Math.sin(now / 700.0) * innerH * 0.03;
            faceX += (targetX - faceX) * 0.25;
            faceY += (targetY - faceY) * 0.25;
        }
        repaint();
    }

    // 900ms heart-rate jitter, base rate per state straight off the mockup's table
    private void tickHeartRate() {
        int base;
        switch (monitorState) {
            case CLOSED:
                base = 0;
                break;
            case ALERT:
                base = 118;
                break;
            case HAPPY:
                base = 86;
                break;
            case IDLE:
                base = 58;
                break;
            default:
                base = 72;
        }
        heartRate = base == 0 ? 0 : base + (int) (Math.random() * 7) - 3;
        repaint();
    }

    private String monitorLabel() {
        switch (monitorState) {
            case HAPPY:
                return "STABLE";
            case ALERT:
                return "REJECTED";
            case CLOSED:
                return "LINK CLOSED";
            case IDLE:
                return "IDLE";
            default:
                return "LISTENING";
        }
    }

    // face-led identity (PlayPanel.AiMonitorPanel's brass frame + eye/mouth arcs),
    // extended with the mockup's blink/sine-wave/idle-bounce behavior. the face and
    // wave shapes approximate the mockup's svg paths rather than parsing them --
    // the state machine and timing above are the ported part, not this geometry
    private void paintMonitor(Graphics2D g2) {
        double innerX = MONITOR_X + 14, innerY = MONITOR_Y + 14;
        double innerW = MONITOR_W - 28, innerH = MONITOR_H - 28;
        Color tint = monitorState == MonitorState.ALERT ? PHOSPHOR_WARN
                : monitorState == MonitorState.IDLE ? idleTint : PHOSPHOR;

        g2.setFont(FONT_LADDER);
        g2.setColor(tint);
        g2.drawString("CORE / " + monitorLabel(), (int) innerX + 8, (int) innerY + 16);
        String hr = "HR " + String.format("%03d", heartRate);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hr, (int) (innerX + innerW - fm.stringWidth(hr) - 8), (int) innerY + 16);

        g2.setColor(tint);
        g2.setStroke(new BasicStroke(1.6f));
        GeneralPath wave = new GeneralPath();
        double waveY = innerY + innerH * 0.72;
        double waveAmp = innerH * 0.08;
        for (double x = 0; x <= innerW; x += 4) {
            double y = waveY + Math.sin((x + waveOffset) * 0.07) * waveAmp;
            if (x == 0) {
                wave.moveTo(innerX + x, y);
            } else {
                wave.lineTo(innerX + x, y);
            }
        }
        g2.draw(wave);

        Composite oldComposite = g2.getComposite();
        if (monitorState == MonitorState.CLOSED) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        }
        g2.setColor(tint);
        g2.setStroke(new BasicStroke(2.5f));
        double fx = innerX + faceX, fy = innerY + faceY;
        boolean happy = monitorState == MonitorState.HAPPY;
        boolean alert = monitorState == MonitorState.ALERT;
        double eyeH = happy ? FACE_H * 0.12 : eyesClosed ? FACE_H * 0.04 : FACE_H * 0.2;
        g2.drawOval((int) (fx + FACE_W * 0.18), (int) (fy + FACE_H * 0.12), (int) (FACE_W * 0.14), (int) eyeH);
        g2.drawOval((int) (fx + FACE_W * 0.68), (int) (fy + FACE_H * 0.12), (int) (FACE_W * 0.14), (int) eyeH);
        int mouthX = (int) (fx + FACE_W * 0.25);
        int mouthW = (int) (FACE_W * 0.5);
        int mouthH = (int) (FACE_H * 0.3);
        if (alert) {
            g2.drawArc(mouthX, (int) (fy + FACE_H * 0.6), mouthW, mouthH, 20, 140);
        } else if (happy) {
            g2.drawArc(mouthX, (int) (fy + FACE_H * 0.52), mouthW, mouthH, 200, 140);
        } else {
            g2.drawLine(mouthX, (int) (fy + FACE_H * 0.66), mouthX + mouthW, (int) (fy + FACE_H * 0.66));
        }
        g2.setComposite(oldComposite);
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
        paintLadderMarker(g2, rungW);
    }

    // brass clip that slides to the current rung, reddens on a failed rung, then
    // slides back to the last secured checkpoint. ports restoration-bay-fold-claude-
    // table.html's moveMarker()/lmark, not re-derived.
    private void paintLadderMarker(Graphics2D g2, double rungW) {
        double x = LADDER_X + markerPos * (rungW + LADDER_GAP);
        double y = LADDER_Y - MARKER_GAP - MARKER_H;
        Polygon marker = new Polygon();
        marker.addPoint((int) x, (int) y);
        marker.addPoint((int) (x + rungW), (int) y);
        marker.addPoint((int) (x + rungW), (int) (y + MARKER_H * 0.55));
        marker.addPoint((int) (x + rungW / 2), (int) (y + MARKER_H));
        marker.addPoint((int) x, (int) (y + MARKER_H * 0.55));
        g2.setColor(markerFailed ? MARKER_FAIL : markerLatched ? MARKER_LATCH : BRASS_LIT);
        g2.fillPolygon(marker);
    }

    // eases markerPos from its current value to target over MARKER_ANIM_MS
    private void animateMarkerTo(double target) {
        if (markerAnimTimer != null) {
            markerAnimTimer.stop();
        }
        markerAnimFrom = markerPos;
        markerTarget = target;
        markerAnimStart = System.currentTimeMillis();
        markerAnimTimer = new Timer(16, e -> {
            double elapsed = System.currentTimeMillis() - markerAnimStart;
            double f = Math.min(1, elapsed / MARKER_ANIM_MS);
            double eased = f * f * (3 - 2 * f);
            markerPos = markerAnimFrom + (markerTarget - markerAnimFrom) * eased;
            repaint();
            if (f >= 1) {
                markerAnimTimer.stop();
            }
        });
        markerAnimTimer.start();
    }

    // 900ms after a failed rung, slide back to the last secured checkpoint
    private void scheduleMarkerRollback(int failedRung) {
        if (markerRollbackTimer != null) {
            markerRollbackTimer.stop();
        }
        markerRollbackTimer = new Timer(MARKER_ROLLBACK_MS, e -> {
            markerFailed = false;
            animateMarkerTo(lastSecuredRung(failedRung));
        });
        markerRollbackTimer.setRepeats(false);
        markerRollbackTimer.start();
    }

    // highest checkpoint question (CONTEXT.md 2.2, Protocol.isCheckpoint) already
    // passed before the failed rung, as a 0-based ladder index; 0 if none secured yet
    private int lastSecuredRung(int failedRung) {
        int lastCheckpoint = 0;
        for (int questionNumber = 1; questionNumber <= failedRung; questionNumber++) {
            if (engine.getProtocol().isCheckpoint(questionNumber)) {
                lastCheckpoint = questionNumber;
            }
        }
        return lastCheckpoint > 0 ? lastCheckpoint - 1 : 0;
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
        FontMetrics answerMetrics = g2.getFontMetrics();
        List<Choice> choices = question.getChoices();
        int colW = (int) (BOARD_W / 2);
        int letterW = 30;
        int textMaxWidth = colW - letterW - 24;
        int lineHeight = 22;
        int rowGap = 14;
        int cy = (int) BOARD_Y + 200;
        // two rows of two, same as the mockup's grid -- a row's height follows
        // whichever of its two choices wraps to the most lines, so a long answer
        // doesn't run into the row below it
        for (int row = 0; row < 2; row++) {
            List<List<String>> rowLines = new ArrayList<>();
            int lineCount = 1;
            for (int col = 0; col < 2; col++) {
                List<String> lines = wrapText(answerMetrics, choices.get(row * 2 + col).getText(), textMaxWidth);
                rowLines.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }
            for (int col = 0; col < 2; col++) {
                int i = row * 2 + col;
                int cx = (int) BOARD_X + 24 + col * colW;
                paintChoice(g2, answerMetrics, choices.get(i), "ABCD".charAt(i), cx, cy, rowLines.get(col),
                        letterW, lineHeight);
            }
            cy += lineCount * lineHeight + rowGap;
        }
    }

    private void paintChoice(Graphics2D g2, FontMetrics fm, Choice choice, char letter, int cx, int cy,
            List<String> lines, int letterW, int lineHeight) {
        boolean removed = removedChoiceIds.contains(choice.getChoiceId());
        boolean predicted = choice.getChoiceId().equals(predictedChoiceId);
        g2.setColor(removed ? BRASS_OX : CHALK_YELLOW);
        g2.drawString(letter + ".", cx, cy);
        g2.setColor(removed ? BRASS_OX : predicted ? CHALK_YELLOW : CHALK);
        int lineY = cy;
        int maxLineWidth = 0;
        for (String line : lines) {
            g2.drawString(line, cx + letterW, lineY);
            maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(line));
            lineY += lineHeight;
        }

        if (displayedAnsweredChoiceId != null) {
            Rectangle markRect = new Rectangle(cx - 10, cy - 18, letterW + maxLineWidth + 20,
                    lines.size() * lineHeight + 10);
            String choiceId = choice.getChoiceId();
            if (choiceId.equals(displayedCorrectChoiceId)) {
                drawChalkCircle(g2, markRect);
            }
            if (!displayedWasCorrect && choiceId.equals(displayedAnsweredChoiceId)) {
                drawChalkStrike(g2, markRect);
            }
        }
    }

    // ports the mockup's .mark svg (wobbly hand-drawn circle/strike), approximated
    // with a few cubic curves instead of parsing the mockup's bezier path verbatim
    private void drawChalkCircle(Graphics2D g2, Rectangle r) {
        GeneralPath path = new GeneralPath();
        double x = r.x, y = r.y, w = r.width, h = r.height;
        path.moveTo(x + w * 0.08, y + h * 0.55);
        path.curveTo(x - w * 0.04, y + h * 0.2, x + w * 0.5, y - h * 0.1, x + w * 0.96, y + h * 0.25);
        path.curveTo(x + w * 1.08, y + h * 0.55, x + w * 0.7, y + h * 1.05, x + w * 0.3, y + h * 0.95);
        path.curveTo(x + w * 0.05, y + h * 0.9, x, y + h * 0.5, x + w * 0.22, y + h * 0.22);
        g2.setColor(CHALK_YELLOW);
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawChalkStrike(Graphics2D g2, Rectangle r) {
        GeneralPath path = new GeneralPath();
        path.moveTo(r.x - r.width * 0.03, r.y + r.height * 0.58);
        path.curveTo(r.x + r.width * 0.3, r.y + r.height * 0.48, r.x + r.width * 0.7, r.y + r.height * 0.55,
                r.x + r.width * 0.97, r.y + r.height * 0.42);
        g2.setColor(TAPE_RED.brighter());
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        g2.setColor(CHALK);
        int curY = y;
        for (String line : wrapText(g2.getFontMetrics(), text, maxWidth)) {
            g2.drawString(line, x, curY);
            curY += lineHeight;
        }
    }

    // greedy word wrap, shared by the prompt and the answer choices so neither
    // overflows its column/board width
    private List<String> wrapText(FontMetrics fm, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    // the printed strip feeds up out of the housing's top edge into its own taller
    // zone above it (mockup's .paper), not inside the compact housing itself
    private void paintPaper(Graphics2D g2) {
        g2.setFont(FONT_PAPER);
        int maxVisible = (int) (PAPER_H / LINE_HEIGHT);
        int y = (int) TELETYPE_Y - 8;
        int shown = 0;
        for (int i = paperLines.size() - 1; i >= 0 && shown < maxVisible; i--, shown++) {
            // the newest line slides up into place instead of just appearing
            double slide = shown == 0 ? (1 - paperFeedT) * LINE_HEIGHT : 0;
            g2.setColor(paperGood.get(i) ? TAPE_GREEN : TAPE_RED);
            g2.drawString(paperLines.get(i), (int) TELETYPE_X + 10, (int) (y + slide));
            y -= LINE_HEIGHT;
        }
    }

    // a desk-row control (key/next/lifeline) whose swing bounds are the tapered
    // trapezoid's bounding box (full width at the bottom edge, see taperedQuad());
    // paintComponent fills/clips to the actual trapezoid within those bounds. hit-
    // testing stays rectangular -- same approximation tier as the desk polygon.
    private static class TaperedButton extends JButton implements Tapered {
        double topInsetLeft, topInsetRight;

        TaperedButton(String text, Font font, Color background, Color foreground) {
            super(text);
            setFont(font);
            setForeground(foreground);
            setBackground(background);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        public void setTaperInsets(double left, double right) {
            topInsetLeft = left;
            topInsetRight = right;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            Polygon trapezoid = new Polygon();
            trapezoid.addPoint((int) topInsetLeft, 0);
            trapezoid.addPoint((int) (w - topInsetRight), 0);
            trapezoid.addPoint(w, h);
            trapezoid.addPoint(0, h);
            g2.setColor(isEnabled() ? getBackground() : getBackground().darker());
            g2.fillPolygon(trapezoid);
            g2.setClip(trapezoid);

            g2.setColor(isEnabled() ? getForeground() : getForeground().darker());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }

    // ports the mockup's .switch .cyl/.key-bit: a brass disc with a bit that
    // flips angle on pullSwitch(), labels dimming on whichever side isn't active
    private static class SwitchButton extends JButton implements Tapered {
        boolean turned;
        double topInsetLeft, topInsetRight;

        SwitchButton() {
            setText("");
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBackground(BRASS);
        }

        public void setTaperInsets(double left, double right) {
            topInsetLeft = left;
            topInsetRight = right;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Polygon trapezoid = new Polygon();
            trapezoid.addPoint((int) topInsetLeft, 0);
            trapezoid.addPoint((int) (w - topInsetRight), 0);
            trapezoid.addPoint(w, h);
            trapezoid.addPoint(0, h);
            g2.setColor(getBackground());
            g2.fillPolygon(trapezoid);
            g2.setClip(trapezoid);

            g2.setFont(FONT_LABEL);
            FontMetrics fm = g2.getFontMetrics();
            int labelY = fm.getAscent() + 4;
            // labels sit near the trapezoid's top edge, which the taper clips narrower
            // than the full (bottom) width w -- anchor to that edge, not to w directly,
            // or the side further from the desk's center clips off (found live: "SECURE"
            // was cut to "SEC")
            double topLeft = topInsetLeft;
            double topRight = w - topInsetRight;
            g2.setColor(turned ? WOOD_DARK.brighter() : WOOD_DARK);
            g2.drawString("TRAIN", (int) topLeft + 6, labelY);
            String secure = "SECURE";
            g2.setColor(turned ? WOOD_DARK : WOOD_DARK.brighter());
            g2.drawString(secure, (int) topRight - fm.stringWidth(secure) - 6, labelY);

            int cx = w / 2;
            int cy = (int) (h * 0.62);
            int r = Math.min(w, h) / 5;
            g2.setColor(BRASS_OX);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            double angle = Math.toRadians(turned ? 40 : -40);
            int len = (int) (r * 1.6);
            int dx = (int) (Math.cos(angle) * len);
            int dy = (int) (Math.sin(angle) * len);
            g2.setColor(CHALK);
            g2.setStroke(new BasicStroke(Math.max(2f, r / 4f)));
            g2.drawLine(cx - dx, cy - dy, cx + dx, cy + dy);
            g2.dispose();
        }
    }
}
