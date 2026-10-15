package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.AudioManager;
import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.Lifeline;
import com.howtotrainyourai.engine.LifelineResult;
import com.howtotrainyourai.engine.Protocol;
import com.howtotrainyourai.engine.QuestionSpeechService;
import com.howtotrainyourai.engine.SessionResult;
import com.howtotrainyourai.engine.TurnResult;
import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

// blocking pass for the vintage ai lab scene, see
// ~/dev/Java/CMSC170/reference-photos/restoration-bay-fold-claude-table.html for the
// frozen mockup this tracks (not the earlier drafts -- restoration-bay-fold.html's
// top-right lifeline panel and chalkboard-ledge NEXT button are superseded). same seam
// as PlayPanel: ScenePanel(CardPanel), startGame(GameEngine). PlayPanel stays the
// working fallback until this is swapped in at Main.java after the oct 4 playthrough.
//
// flat shapes only, no gradients/textures.
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
    private static final Color PAPER_INK = Color.decode("#2e2618");
    private static final Color PAPER_GOOD = Color.decode("#3c4a22");
    private static final Color PAPER_BAD = Color.decode("#86382a");
    private static final Color CEILING = Color.decode("#12201c");
    // mockup .bay/.cell background (#0a0706/#141210/#1b211f/#0f1312 are all close
    // near-blacks in the mockup; one well color covers all of them here)
    private static final Color SHELF_WELL = Color.decode("#0a0706");
    // mockup .lmark.failed / .lmark.latched literal colors (not css vars there either)
    private static final Color MARKER_FAIL = Color.decode("#c97a62");
    private static final Color MARKER_LATCH = Color.decode("#f4e6a8");
    private static final Color COLD_LIGHT = Color.decode("#cfeee2");
    private static final BufferedImage GRAIN = loadTexture("/images/grain.png");
    private static final BufferedImage STAIN = loadTexture("/images/stain.png");
    private static final Map<String, BufferedImage> TEXTURE_CACHE = new HashMap<>();
    private static final double PARALLAX_MAX = 4;

    private static BufferedImage loadTexture(String path) {
        try (var input = ScenePanel.class.getResourceAsStream(path)) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException e) {
            return null;
        }
    }

    // capability unlock order, matches GameEngineImpl.CAPABILITY_UNLOCKS (Q3/5/8/10/13/15)
    private static final String[] CAPABILITY_NAMES = {
            "MEMORY", "UNDERSTANDING", "APPLICATION", "ANALYSIS", "EVALUATION", "SYNTHESIZE" };

    // lifelineButtons[i] <-> this enum, same order as the mockup's BINARY/PREDICT/
    // OVERRIDE cells. OVERRIDE is spent by the engine; after that it becomes the
    // manual RETRY control for the same question.
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
    private static final Font FONT_PAPER = new Font(Font.MONOSPACED, Font.PLAIN, 14);

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
    private static final int PAPER_LINE_HEIGHT = 18;
    private static final int PAPER_FEED_MS = 300;
    private static final int QUESTION_SWEEP_MS = 1500;
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

    // exact closed form for .desk's perspective(60cqw) rotateX(17deg) (java2d has no
    // true 3d transform, so this reproduces it by formula instead). DESK_BOTTOM_Y is
    // the CSS transform-origin:bottom pivot -- .desk's own box bottom (DESK_Y+DESK_H),
    // which .desk{bottom:-6%} places below the visible 1000-tall canvas, not at y=1000.
    // width(y) = DESK_BOTTOM_W / (1 + k*(DESK_BOTTOM_Y - y)), k = sin(rotateX)/perspective,
    // both centered on x=800. Every desk-row shape/button interpolates against this same
    // field so the whole assembly tapers consistently, not just the background.
    //
    // Derived from the live mockup's own computed transform matrix (getComputedStyle
    // .desk .transform -> matrix3d(...), decoded: rotateX=17deg, perspective=0.6x the
    // container width, exactly matching the CSS) and cross-checked against getBoundingClientRect
    // corner geometry for the desk, a key, and the switch (all matched within ~1px).
    // An earlier version of this file used a linear top/bottom width lerp that looked
    // plausible but overstated the real taper by roughly 2.6x -- confirmed by this same
    // live-DOM measurement after the lerp version was first reported as looking untapered,
    // then as looking right, from screenshots that were too coarse to catch a few-pixel
    // shift either way. Don't re-derive this from a screenshot; re-derive it from
    // getComputedStyle/getBoundingClientRect on the live mockup if it's ever in doubt again.
    private static final double DESK_TOP_Y = 720, DESK_BOTTOM_Y = DESK_Y + DESK_H;
    private static final double DESK_BOTTOM_W = 1728;
    private static final double DESK_CENTER_X = 800;
    private static final double DESK_ROTATE_DEG = 17;
    private static final double DESK_PERSPECTIVE = 0.6 * LOGICAL_W;
    private static final double DESK_TAPER_K = Math.sin(Math.toRadians(DESK_ROTATE_DEG)) / DESK_PERSPECTIVE;

    private double deskWidthAt(double y) {
        return DESK_BOTTOM_W / (1 + DESK_TAPER_K * (DESK_BOTTOM_Y - y));
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
    private final AudioManager audioManager;
    private final QuestionSpeechService speechService;
    private final Color[] ladderOutcomes = new Color[TOTAL_QUESTIONS]; // null = not answered yet
    private final TaperedButton[] lifelineButtons = new TaperedButton[3];
    private final TaperedButton[] keyButtons = new TaperedButton[4];
    private final TaperedButton nextButton;
    private final SwitchButton switchButton;
    private final JButton backButton;
    private final List<String> paperLines = new ArrayList<>();
    private final List<Boolean> paperGood = new ArrayList<>();
    private final List<Long> paperLineTimes = new ArrayList<>();
    private Timer paperFeedTimer;
    private Timer countdownTimer;
    private int countdownSeconds;
    private Timer endingTimer;
    private Timer questionSweepTimer;
    private long questionSweepStart;
    private double questionSweepProgress = 1;
    private int endingCapabilityIndex;

    private GameEngine engine;
    private int runningTotal;
    private int questionIndex;
    private boolean sessionOver;
    private boolean overrideRetryPending;
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
    // capabilities actually restored this session -- unlike lampsOn, never cleared by
    // a High Risk failure or replayed by the win sequence
    private final boolean[] capabilitiesRestored = new boolean[CAPABILITY_NAMES.length];
    private EndingPanel endingPanel;
    // set when the Training Records write failed; the result is still shown, just not as filed
    private boolean recordWriteFailed;

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
    private double parallaxX, parallaxY;

    public ScenePanel(CardPanel cardPanel) {
        this(cardPanel, null, null);
    }

    public ScenePanel(CardPanel cardPanel, AudioManager audioManager, QuestionSpeechService speechService) {
        this.cardPanel = cardPanel;
        this.audioManager = audioManager;
        this.speechService = speechService;
        setLayout(null);
        setBackground(WOOD_DARK);

        String[] lifelineLabels = { "BINARY", "PREDICT", "OVERRIDE" };
        for (int i = 0; i < lifelineButtons.length; i++) {
            TaperedButton button = new TaperedButton(lifelineLabels[i], FONT_LIFELINE, BRASS, PAPER);
            Lifeline lifeline = LIFELINE_ORDER[i];
            if (lifeline == Lifeline.OVERRIDE) {
                // The engine spends Override automatically; this button is enabled
                // only afterward so the player manually acknowledges the retry.
                button.setEnabled(false);
                button.addActionListener(e -> retryOverride());
            } else {
                button.addActionListener(e -> {
                    playControlSound();
                    pullLifeline(lifeline);
                });
            }
            lifelineButtons[i] = button;
            button.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { skipQuestionSweep(); }
            });
            add(button);
        }

        String[] keyLabels = { "A", "B", "C", "D" };
        for (int i = 0; i < keyButtons.length; i++) {
            TaperedButton button = new TaperedButton(keyLabels[i], FONT_PROMPT, BRASS_LIT, WOOD_DARK);
            final int position = i;
            button.addActionListener(e -> {
                playControlSound();
                submitAnswerAt(position);
            });
            button.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { skipQuestionSweep(); }
            });
            keyButtons[i] = button;
            add(button);
        }

        nextButton = new TaperedButton("NEXT PROBE", FONT_LABEL, BRASS, PAPER);
        nextButton.setEnabled(false);
        nextButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { skipQuestionSweep(); }
        });
        nextButton.addActionListener(e -> {
            playControlSound();
            if (sessionOver) {
                showEnding(finishSession());
            } else {
                showQuestion();
            }
        });
        add(nextButton);

        switchButton = new SwitchButton();
        switchButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { skipQuestionSweep(); }
        });
        switchButton.addActionListener(e -> {
            playControlSound();
            pullSwitch();
        });
        add(switchButton);

        backButton = flatButton("Back", FONT_LABEL, BRASS, PAPER);
        backButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { skipQuestionSweep(); }
        });
        backButton.addActionListener(e -> {
            playControlSound();
            // Return: ends the session, then shows the summary of what was filed
            showEnding(engine != null ? finishSession() : null);
        });
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
                skipQuestionSweep();
                wakeMonitor();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                parallaxX = (0.5 - e.getX() / (double) Math.max(1, getWidth())) * PARALLAX_MAX;
                parallaxY = (0.5 - e.getY() / (double) Math.max(1, getHeight())) * PARALLAX_MAX;
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                parallaxX = parallaxY = 0;
                repaint();
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isShowing() && !e.isControlDown()
                    && !e.isMetaDown() && !e.isAltDown()) skipQuestionSweep();
            return false;
        });

        monitorTimer = new Timer(MONITOR_TICK_MS, e -> tickMonitor());
        monitorTimer.start();
        heartRateTimer = new Timer(HEART_RATE_TICK_MS, e -> tickHeartRate());
        heartRateTimer.start();
    }

    /** Where NEXT PROBE / BACK TO MENU goes once a session is over. */
    public void setEndingPanel(EndingPanel endingPanel) {
        this.endingPanel = endingPanel;
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
        if (speechService != null) speechService.cancel();
        this.engine = engine;
        runningTotal = 0;
        this.questionIndex = 0;
        this.sessionOver = false;
        this.overrideRetryPending = false;
        java.util.Arrays.fill(ladderOutcomes, null);
        paperLines.clear();
        paperGood.clear();
        paperLineTimes.clear();
        if (markerAnimTimer != null) {
            markerAnimTimer.stop();
        }
        if (markerRollbackTimer != null) {
            markerRollbackTimer.stop();
        }
        if (paperFeedTimer != null) {
            paperFeedTimer.stop();
        }
        markerPos = 0;
        markerTarget = 0;
        markerFailed = false;
        markerLatched = false;
        switchButton.turned = false;
        java.util.Arrays.fill(lampsOn, false);
        java.util.Arrays.fill(capabilitiesRestored, false);
        recordWriteFailed = false;
        if (idleTimer != null) {
            idleTimer.stop();
        }
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (endingTimer != null) {
            endingTimer.stop();
        }
        monitorState = MonitorState.LISTENING;
        heartRate = 72;
        faceX = (MONITOR_W - 28) / 2.0 - FACE_W / 2.0;
        faceY = (MONITOR_H - 28) / 2.0 - FACE_H / 2.0;
        // opening state: Memory is corrupted, the other five capabilities are offline
        print("MEMORY CORRUPTED", false);
        print("UNDERSTAND OFFLINE");
        print("APPLICATION OFFLINE");
        print("ANALYSIS OFFLINE");
        print("EVALUATION OFFLINE");
        print("SYNTHESIS OFFLINE");
        showQuestion();
    }

    private void showQuestion() {
        if (questionSweepTimer != null) questionSweepTimer.stop();
        questionSweepProgress = 0;
        displayedQuestion = engine.currentQuestion();
        if (speechService != null) speechService.speak(displayedQuestion.getText());
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
        startCountdown();
        questionSweepStart = System.currentTimeMillis();
        questionSweepTimer = new Timer(16, e -> {
            questionSweepProgress = Math.min(1, (System.currentTimeMillis() - questionSweepStart)
                    / (double) QUESTION_SWEEP_MS);
            repaint();
            if (questionSweepProgress >= 1) questionSweepTimer.stop();
        });
        questionSweepTimer.start();
        relayout();
    }

    private void skipQuestionSweep() {
        if (questionSweepProgress >= 1) return;
        questionSweepProgress = 1;
        if (questionSweepTimer != null) questionSweepTimer.stop();
        repaint();
    }

    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (!engine.getProtocol().hasTimer()) {
            countdownSeconds = 0;
            return;
        }
        countdownSeconds = Protocol.HIGH_RISK_TIME_LIMIT_SECONDS;
        countdownTimer = new Timer(1000, e -> {
            countdownSeconds--;
            repaint();
            if (countdownSeconds <= 0) {
                countdownTimer.stop();
                print("HIGH RISK TIMER EXPIRED", false);
                List<Choice> choices = displayedQuestion.getChoices();
                for (int i = 0; i < choices.size(); i++) {
                    if (!displayedQuestion.isCorrect(choices.get(i).getChoiceId())) {
                        submitAnswerAt(i);
                        return;
                    }
                }
            }
        });
        countdownTimer.start();
    }

    // Leaves the lab for the summary screen (or the menu if there is nothing to show).
    // The win sequence is stopped first so its lamps and sounds don't keep playing
    // over the next screen.
    private void showEnding(SessionResult record) {
        if (endingTimer != null) {
            endingTimer.stop();
        }
        if (endingPanel != null && record != null) {
            endingPanel.show(record, capabilitiesRestored, !recordWriteFailed);
            cardPanel.showScreen(CardPanel.ENDING_COMIC);
        } else {
            cardPanel.showScreen(CardPanel.MENU);
        }
    }

    private SessionResult finishSession() {
        if (speechService != null) speechService.cancel();
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        try {
            return engine.endSession();
        } catch (java.io.UncheckedIOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Training record error", JOptionPane.ERROR_MESSAGE);
            // the engine keeps the result even when the write failed, so a second
            // call returns it without trying to write again. Remember the failure:
            // that second call succeeds, so it can't tell us the record wasn't filed.
            recordWriteFailed = true;
            return engine.endSession();
        }
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
            boolean clickable = lifeline == Lifeline.OVERRIDE ? overrideRetryPending
                    : remaining.contains(lifeline) && !sessionOver;
            lifelineButtons[i].setEnabled(clickable);
        }
    }

    private void retryOverride() {
        if (!overrideRetryPending || sessionOver) return;
        playControlSound();
        overrideRetryPending = false;
        lifelineButtons[2].setText("OVERRIDE");
        for (int i = 0; i < keyButtons.length; i++) {
            keyButtons[i].setEnabled(!removedChoiceIds.contains(displayedQuestion.getChoices().get(i).getChoiceId()));
            keyButtons[i].setBackground(BRASS_LIT);
        }
        selectedKeyIndex = -1;
        waitingForAnswer = true;
        monitorState = MonitorState.LISTENING;
        scheduleIdle();
        startCountdown();
        refreshLifelines();
        print("RETRY INPUT ACCEPTED");
        repaint();
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
            print("BINARY CHOICE: TWO WRONG UNITS PURGED");
        } else {
            predictedChoiceId = result.getPredictedChoiceId();
            predictedConfidence = result.getConfidencePercent();
            int position = positionOfChoiceId(predictedChoiceId);
            String letter = position >= 0 ? String.valueOf("ABCD".charAt(position)) : "?";
            print("PREDICT: AI SUGGESTS " + letter + " AT " + predictedConfidence + "%");
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
        markerLatched = !markerFailed;
        waitingForAnswer = false;
        if (idleTimer != null) {
            idleTimer.stop();
        }
        monitorState = MonitorState.CLOSED;
        SessionResult record = finishSession();
        print(recordWriteFailed || record == null ? "RECORD WRITE FAILED" :
                "BUNDLE WRITTEN TO TAPE / " + record.getFinalScore() + " UNITS", !recordWriteFailed && record != null);
        print("LINK CLOSED");
        nextButton.setText("VIEW RESULT");
        nextButton.setEnabled(true);
        repaint();
    }

    // submits whatever choice is actually shown at this display position right now
    // -- never a fixed letter. QuestionSource shuffles choice order per question
    // (CsvQuestionSource/CONTEXT.md 3), so the display position is not the choiceId;
    // same pattern PlayPanel.submitAnswerAt() already uses for the same reason.
    private void submitAnswerAt(int position) {
        if (speechService != null) speechService.cancel();
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
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        TurnResult result = engine.submitAnswer(choiceId);
        runningTotal = result.getRunningTotal();
        if (result.isRetry()) {
            // Override absorbed the miss. The engine keeps the same question;
            // wait for the operator to press the Override control before retrying.
            overrideRetryPending = true;
            lifelineButtons[2].setText("RETRY");
            lifelineButtons[2].setEnabled(true);
            print("OVERRIDE ENGAGED // RETRY AVAILABLE");
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
                        capabilitiesRestored[lampIndex] = true;
                        if (!(result.isGameOver() && result.isCorrect())) playCeilingLightSound();
                    }
                }
            }
            markerFailed = false;
            animateMarkerTo(Math.min(questionIndex, TOTAL_QUESTIONS - 1));
            monitorState = MonitorState.HAPPY;
        } else {
            print("P" + questionIndex + " REJECTED / ANSWER " + "ABCD".charAt(position), false);
            print("UNCOMMITTED UNITS DISCARDED", false);
            print("ROLLBACK TO " + result.getRunningTotal() + " SECURED", false);
            markerFailed = true;
            animateMarkerTo(answeredRung);
            scheduleMarkerRollback(answeredRung);
            monitorState = MonitorState.ALERT;
        }

        if (sessionOver) {
            SessionResult record = finishSession();
            if (result.isCorrect()) {
                startWinSequence();
            } else {
                print("RESTORATION FAILED", false);
                if (recordWriteFailed) {
                    print("RECORD WRITE FAILED", false);
                } else if (record != null) {
                    print("FAIL RECORD FILED / " + record.getFinalScore() + " UNITS");
                }
                nextButton.setEnabled(false);
                endingTimer = new Timer(1400, e -> {
                    endingTimer.stop();
                    nextButton.setEnabled(true);
                });
                endingTimer.setRepeats(false);
                endingTimer.start();
            }
            for (JButton lifelineButton : lifelineButtons) {
                lifelineButton.setEnabled(false);
            }
            switchButton.setEnabled(false);
            nextButton.setText("VIEW RESULT");
        } else {
            nextButton.setText("NEXT PROBE");
        }
        nextButton.setEnabled(!sessionOver);
        relayout();
    }

    private void playControlSound() {
        if (audioManager != null) audioManager.playSfx("control");
    }

    private void playCeilingLightSound() {
        if (audioManager != null) audioManager.playSfx("ceiling");
    }

    public void shutdown() {
        for (Timer timer : new Timer[] { paperFeedTimer, countdownTimer, endingTimer, questionSweepTimer,
                markerAnimTimer, markerRollbackTimer, monitorTimer, heartRateTimer, idleTimer }) {
            if (timer != null) timer.stop();
        }
    }

    private void startWinSequence() {
        nextButton.setEnabled(false);
        java.util.Arrays.fill(lampsOn, false);
        endingCapabilityIndex = 0;
        String[] online = { "MEMORY ONLINE", "UNDERSTANDING ONLINE", "APPLICATION ONLINE",
                "ANALYSIS ONLINE", "EVALUATION ONLINE", "SYNTHESIS ONLINE" };
        if (endingTimer != null) {
            endingTimer.stop();
        }
        endingTimer = new Timer(700, e -> {
            if (endingCapabilityIndex < lampsOn.length) {
                lampsOn[endingCapabilityIndex] = true;
                playCeilingLightSound();
                print(online[endingCapabilityIndex], true);
                endingCapabilityIndex++;
            } else {
                print("AI CORE: FULLY ACTIVATED", true);
                endingTimer.stop();
                nextButton.setEnabled(true);
            }
            repaint();
        });
        endingTimer.setInitialDelay(0);
        endingTimer.start();
    }

    private void print(String text) {
        print(text, null);
    }

    private void print(String text, Boolean good) {
        paperLines.add(text);
        paperGood.add(good);
        paperLineTimes.add(System.currentTimeMillis());
        while (paperLines.size() > 9) {
            paperLines.remove(0);
            paperGood.remove(0);
            paperLineTimes.remove(0);
        }

        if (paperFeedTimer != null) {
            paperFeedTimer.stop();
        }
        paperFeedTimer = new Timer(16, e -> {
            repaint();
            long now = System.currentTimeMillis();
            if (paperLineTimes.stream().noneMatch(time -> now - time < PAPER_FEED_MS)) {
                paperFeedTimer.stop();
            }
        });
        paperFeedTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        paintRoomBackdrop(g2);
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        // distant facility shifts subtly; foreground controls keep their hit targets fixed
        g2.translate(parallaxX, parallaxY);
        paintWall(g2);
        paintTexture(g2, new Rectangle(0, 0, (int) LOGICAL_W, (int) LOGICAL_H),
                0, 0, LOGICAL_W, LOGICAL_H, 0.45f, 0.12f);
        g2.setPaint(new GradientPaint(0, 0, Color.decode("#050706"), 0, (float) CEILING_H, CEILING));
        g2.fillRect(-8, -8, (int) LOGICAL_W + 16, (int) CEILING_H + 16);
        paintLamps(g2);
        g2.setPaint(new LinearGradientPaint((float) CAB_L_X, 0, (float) (CAB_L_X + CAB_L_W), 0,
                new float[] { 0, 0.4f, 1 }, new Color[] { Color.decode("#3f5249"), MINT, Color.decode("#435a50") }));
        g2.fillRect((int) CAB_L_X, (int) CAB_L_Y, (int) CAB_L_W, (int) CAB_L_H);
        paintTexture(g2, new Rectangle((int) CAB_L_X, (int) CAB_L_Y, (int) CAB_L_W, (int) CAB_L_H),
                CAB_L_X, CAB_L_Y, CAB_L_W, CAB_L_H, 0.85f, 0.16f);
        g2.setColor(Color.decode("#1f2825"));
        g2.fillRect((int) CAB_L_X, (int) CAB_L_Y, (int) CAB_L_W, 8);
        g2.fillRect((int) CAB_L_X, (int) (CAB_L_Y + CAB_L_H - 8), (int) CAB_L_W, 8);
        g2.setPaint(new LinearGradientPaint((float) CAB_R_X, 0, (float) (CAB_R_X + CAB_R_W), 0,
                new float[] { 0, 0.45f, 1 }, new Color[] { Color.decode("#434d4a"), STEEL, Color.decode("#3d4643") }));
        g2.fillRect((int) CAB_R_X, (int) CAB_R_Y, (int) CAB_R_W, (int) CAB_R_H);
        paintTexture(g2, new Rectangle((int) CAB_R_X, (int) CAB_R_Y, (int) CAB_R_W, (int) CAB_R_H),
                CAB_R_X, CAB_R_Y, CAB_R_W, CAB_R_H, 0.85f, 0.16f);
        g2.translate(-parallaxX, -parallaxY);

        // midground: the salvaged training station
        g2.setPaint(new LinearGradientPaint((float) STATION_X, (float) STATION_Y,
                (float) (STATION_X + STATION_W), (float) (STATION_Y + STATION_H),
                new float[] { 0, 0.55f, 1 },
                new Color[] { Color.decode("#e2d4ab"), PAPER, Color.decode("#c7b484") }));
        g2.fillRect((int) STATION_X, (int) STATION_Y, (int) STATION_W, (int) STATION_H);
        paintTexture(g2, new Rectangle((int) STATION_X, (int) STATION_Y,
                (int) STATION_W, (int) STATION_H), STATION_X, STATION_Y, STATION_W, STATION_H, 0.85f, 0.16f);
        g2.setColor(Color.decode("#9c8752"));
        g2.setStroke(new BasicStroke(5));
        g2.drawRect((int) STATION_X, (int) STATION_Y, (int) STATION_W, (int) STATION_H);

        paintLadder(g2);

        g2.setColor(SLATE);
        g2.fillRect((int) BOARD_X, (int) BOARD_Y, (int) BOARD_W, (int) BOARD_H);
        Graphics2D boardGraphics = (Graphics2D) g2.create();
        boardGraphics.clipRect((int) BOARD_X, (int) BOARD_Y,
                (int) (BOARD_W * questionSweepProgress), (int) BOARD_H);
        paintBoardContent(boardGraphics);
        boardGraphics.dispose();
        if (questionSweepProgress < 1) paintQuestionArm(g2);

        g2.setColor(WOOD_DARK);
        g2.fillRect((int) LEDGE_X, (int) LEDGE_Y, (int) LEDGE_W, (int) LEDGE_H);

        // foreground: operator's desk, faked perspective since java2d has no true tilt.
        // corners come from deskWidthAt() (see its comment), not hand-measured points --
        // the bottom pair sits at DESK_BOTTOM_Y (below the visible canvas by design),
        // clipped by the panel's own bounds same as the mockup's .desk bleed.
        double deskTopW = deskWidthAt(DESK_TOP_Y);
        Polygon desk = new Polygon();
        desk.addPoint((int) (DESK_CENTER_X - deskTopW / 2), (int) DESK_TOP_Y);
        desk.addPoint((int) (DESK_CENTER_X + deskTopW / 2), (int) DESK_TOP_Y);
        desk.addPoint((int) (DESK_CENTER_X + DESK_BOTTOM_W / 2), (int) DESK_BOTTOM_Y);
        desk.addPoint((int) (DESK_CENTER_X - DESK_BOTTOM_W / 2), (int) DESK_BOTTOM_Y);
        g2.setPaint(new LinearGradientPaint((float) DESK_CENTER_X, (float) DESK_Y,
                (float) DESK_CENTER_X, (float) DESK_BOTTOM_Y,
                new float[] { 0, 0.58f, 1 }, new Color[] {
                        Color.decode("#7d6043"), Color.decode("#48351f"), Color.decode("#2a1c10") }));
        g2.fillPolygon(desk);
        paintDeskGrain(g2, desk);
        paintTexture(g2, desk, DESK_X, DESK_Y, DESK_W, DESK_H, 0.85f, 0.16f);

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
        Polygon facade = taperedPolygon(SHELFBAR_X, SHELFBAR_Y + SHELFBAR_LIP_H,
                SHELFBAR_W, SHELFBAR_H - SHELFBAR_LIP_H);
        paintTexture(g2, facade, SHELFBAR_X, SHELFBAR_Y + SHELFBAR_LIP_H,
                SHELFBAR_W, SHELFBAR_H - SHELFBAR_LIP_H, 0.85f, 0.16f);
        g2.fillPolygon(taperedPolygon(SHELF_X, SHELF_Y, SHELF_W, SHELF_H));

        g2.setColor(SHELF_WELL);
        g2.fillPolygon(taperedPolygon(BAY_X, BAY_Y, BAY_W, BAY_H));
        g2.setColor(STEEL);
        g2.fillPolygon(taperedPolygon(CARRIER_X, CARRIER_Y, CARRIER_W, CARRIER_H));
        for (int i = 0; i < LIFELINE_X.length; i++) {
            g2.setColor(SHELF_WELL);
            g2.fillPolygon(taperedPolygon(LIFELINE_X[i] - 4, LIFELINE_Y - 4, LIFELINE_W[i] + 8, LIFELINE_H + 8));
        }

        Polygon teletype = taperedPolygon(TELETYPE_X, TELETYPE_Y, TELETYPE_W, TELETYPE_H);
        g2.setPaint(new GradientPaint(0, (float) TELETYPE_Y, Color.decode("#58625e"),
                0, (float) (TELETYPE_Y + TELETYPE_H), Color.decode("#2f3734")));
        g2.fillPolygon(teletype);
        g2.setColor(Color.decode("#171c1a"));
        g2.setStroke(new BasicStroke(4));
        g2.drawPolygon(teletype);
        paintPaper(g2);
        g2.setColor(Color.decode("#0b0e0d"));
        g2.fillRoundRect((int) (TELETYPE_X + 14), (int) (TELETYPE_Y - 2), (int) TELETYPE_W - 28, 8, 5, 5);
        g2.setColor(Color.decode("#c3cec8"));
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        String recordLabel = "R E C O R D";
        g2.drawString(recordLabel, (int) (TELETYPE_X + (TELETYPE_W - g2.getFontMetrics().stringWidth(recordLabel)) / 2),
                (int) (TELETYPE_Y + TELETYPE_H - 8));

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

    private void paintRoomBackdrop(Graphics2D g2) {
        g2.setColor(TILE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(57, 77, 72, 90));
        int firstX = (int) Math.floor(-offsetX / scale / 94) * 94;
        int lastX = (int) Math.ceil((getWidth() - offsetX) / scale / 94) * 94;
        for (int x = firstX; x <= lastX; x += 94) {
            int screenX = (int) Math.round(offsetX + x * scale);
            g2.fillRect(screenX, 0, Math.max(1, (int) Math.round(2 * scale)), getHeight());
        }

        int firstY = (int) Math.floor(-offsetY / scale / 62) * 62;
        int lastY = (int) Math.ceil((getHeight() - offsetY) / scale / 62) * 62;
        for (int y = firstY; y <= lastY; y += 62) {
            int screenY = (int) Math.round(offsetY + y * scale);
            g2.fillRect(0, screenY, getWidth(), Math.max(1, (int) Math.round(2 * scale)));
        }

        int canvasBottom = (int) Math.round(offsetY + LOGICAL_H * scale);
        if (canvasBottom < getHeight()) {
            g2.setColor(WOOD_DARK);
            g2.fillRect(0, canvasBottom, getWidth(), getHeight() - canvasBottom);
        }
    }

    private void paintTexture(Graphics2D g2, Shape clip, double x, double y, double w, double h,
            float grainOpacity, float stainOpacity) {
        if (GRAIN == null || STAIN == null) return;
        int width = (int) Math.round(w), height = (int) Math.round(h);
        String key = width + "x" + height + ":" + grainOpacity + ":" + stainOpacity;
        BufferedImage overlay = TEXTURE_CACHE.computeIfAbsent(key,
                ignored -> createTextureOverlay(width, height, grainOpacity, stainOpacity));
        Graphics2D texture = (Graphics2D) g2.create();
        texture.clip(clip);
        texture.drawImage(overlay, (int) x, (int) y, null);
        texture.dispose();
    }

    private static BufferedImage createTextureOverlay(int width, int height, float grainOpacity, float stainOpacity) {
        BufferedImage overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D texture = overlay.createGraphics();
        texture.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, grainOpacity));
        texture.setPaint(new TexturePaint(GRAIN, new Rectangle(0, 0, GRAIN.getWidth(), GRAIN.getHeight())));
        texture.fillRect(0, 0, width, height);
        texture.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, stainOpacity));
        double scale = Math.max(width / (double) STAIN.getWidth(), height / (double) STAIN.getHeight());
        double stainW = STAIN.getWidth() * scale, stainH = STAIN.getHeight() * scale;
        texture.setPaint(new TexturePaint(STAIN, new java.awt.geom.Rectangle2D.Double(
                (width - stainW) / 2, (height - stainH) / 2, stainW, stainH)));
        texture.fillRect(0, 0, width, height);
        texture.dispose();
        return overlay;
    }

    private void paintWall(Graphics2D g2) {
        g2.setColor(TILE);
        g2.fillRect(-8, -8, (int) LOGICAL_W + 16, (int) LOGICAL_H + 16);
        g2.setColor(new Color(57, 77, 72, 90));
        for (int y = 62; y < LOGICAL_H; y += 62) g2.fillRect(0, y, (int) LOGICAL_W, 2);
        for (int x = 94; x < LOGICAL_W; x += 94) g2.fillRect(x, 0, 2, (int) LOGICAL_H);
        g2.setPaint(new RadialGradientPaint((float) LOGICAL_W / 2, (float) LOGICAL_H * 0.45f,
                (float) LOGICAL_W * 0.58f, new float[] { 0.35f, 1 },
                new Color[] { new Color(6, 9, 8, 0), new Color(6, 9, 8, 192) }));
        g2.fillRect(0, 0, (int) LOGICAL_W, (int) LOGICAL_H);
        g2.setPaint(new LinearGradientPaint(0, 0, 0, (float) LOGICAL_H,
                new float[] { 0, 0.12f, 0.30f, 0.52f, 0.96f, 1 }, new Color[] {
                        new Color(7, 10, 9, 240), new Color(11, 15, 14, 153),
                        new Color(7, 10, 9, 0), new Color(7, 10, 9, 0),
                        new Color(7, 10, 9, 245), new Color(7, 10, 9, 245) }));
        g2.fillRect(0, 0, (int) LOGICAL_W, (int) LOGICAL_H);
    }

    private void paintDeskGrain(Graphics2D g2, Shape desk) {
        Graphics2D lines = (Graphics2D) g2.create();
        lines.clip(desk);
        lines.rotate(Math.toRadians(-6), DESK_CENTER_X, DESK_BOTTOM_Y);
        lines.setColor(new Color(0xd7, 0xbb, 0x84, 24));
        for (int y = (int) DESK_Y; y < DESK_BOTTOM_Y + 40; y += 30) {
            lines.drawLine(-100, y, 1700, y);
        }
        lines.dispose();
    }

    private void paintQuestionArm(Graphics2D g2) {
        double x = BOARD_X + BOARD_W * questionSweepProgress;
        double mountX = LAMPS_X + (LAMPS_W - 1) * questionSweepProgress;
        g2.setColor(STEEL_DARK);
        g2.fillRect((int) LAMPS_X, 34, (int) LAMPS_W, 9);
        g2.setColor(STEEL_DARK);
        g2.fillRect((int) mountX - 4, 32, 8, 36);
        g2.setColor(STEEL);
        g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) mountX, 66, (int) (mountX - 24), 142);
        g2.drawLine((int) (mountX - 24), 142, (int) (x - 12), (int) BOARD_Y);
        g2.setColor(BRASS_LIT);
        g2.fillOval((int) mountX - 7, 59, 14, 14);
        g2.fillOval((int) (mountX - 31), 135, 14, 14);
        g2.fillOval((int) (x - 18), (int) BOARD_Y - 6, 12, 12);
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
        g2.setColor(CHALK_YELLOW);
        g2.drawString("TOKENS " + runningTotal, (int) BOARD_X + 24, (int) BOARD_Y + 54);
        if (engine.getProtocol().hasTimer()) {
            g2.setColor(countdownSeconds <= 5 ? TAPE_RED : CHALK_YELLOW);
            g2.drawString("TIME " + countdownSeconds + "s", (int) (BOARD_X + BOARD_W) - 115,
                    (int) BOARD_Y + 54);
        }

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

    private void paintPaper(Graphics2D g2) {
        double paperX = TELETYPE_X + TELETYPE_W * 0.04;
        double paperW = TELETYPE_W * 0.92;
        double paperY = TELETYPE_Y - PAPER_H;
        Shape oldClip = g2.getClip();
        g2.clip(taperedPolygon(paperX, paperY, paperW, PAPER_H + 2));
        g2.setPaint(new GradientPaint(0, (float) paperY, new Color(217, 202, 159, 0),
                0, (float) (paperY + 36), Color.decode("#e6d8b1")));
        g2.fillRect((int) paperX, (int) paperY, (int) paperW, (int) PAPER_H + 2);

        g2.setColor(new Color(177, 157, 108, 180));
        g2.drawLine((int) paperX, (int) paperY, (int) paperX, (int) TELETYPE_Y);
        g2.drawLine((int) (paperX + paperW), (int) paperY,
                (int) (paperX + paperW), (int) TELETYPE_Y);

        g2.setFont(FONT_PAPER);
        FontMetrics metrics = g2.getFontMetrics();
        int textWidth = (int) paperW - 16;
        int maxVisible = (int) (PAPER_H / PAPER_LINE_HEIGHT);
        int y = (int) TELETYPE_Y - 8;
        int shown = 0;
        long now = System.currentTimeMillis();
        for (int i = paperLines.size() - 1; i >= 0 && shown < maxVisible; i--) {
            List<String> wrapped = wrapText(metrics, paperLines.get(i), textWidth);
            double progress = Math.min(1, (now - paperLineTimes.get(i)) / (double) PAPER_FEED_MS);
            float alpha = (float) Math.max(0, progress);
            g2.setColor(paperGood.get(i) == null ? PAPER_INK :
                    paperGood.get(i) ? PAPER_GOOD : PAPER_BAD);
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            for (int row = wrapped.size() - 1; row >= 0 && shown < maxVisible; row--, shown++) {
                double slide = (1 - progress) * PAPER_LINE_HEIGHT;
                g2.drawString(wrapped.get(row), (int) paperX + 8,
                        (int) (y - shown * PAPER_LINE_HEIGHT + slide));
            }
            g2.setComposite(oldComposite);
        }
        g2.setClip(oldClip);
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
