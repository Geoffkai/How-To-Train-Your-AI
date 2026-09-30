package com.howtotrainyourai.gui;

import com.howtotrainyourai.engine.GameEngine;
import com.howtotrainyourai.engine.Lifeline;
import com.howtotrainyourai.engine.LifelineResult;
import com.howtotrainyourai.engine.TurnResult;
import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

// question screen: 15-segment ladder, 3 lifeline buttons, current question + 4 choices,
// result banner, ai vitals monitor. matches the "simple version" question/correct/incorrect
// frames in the team figma file (node 12:188/12:189/12:190), palette pulled from its
// colors variable collection.
//
// wired to the real engine: SetupPanel collects the trainer name and protocol, starts a
// session, and calls startGame(). this panel only ever talks to the GameEngine INTERFACE,
// never GameEngineImpl or a QuestionSource, per GameEngine's "single seam" comment.
//
// two things here are easy to get wrong and are commented where they happen:
// a choice's display POSITION is not its choiceId (the bank shuffles), and an Override
// retry means the engine did NOT advance.
public class PlayPanel extends JPanel implements GameScreen {

    private static final int TOTAL_QUESTIONS = 15;

    // lifeline buttons, left to right. OVERRIDE is in this list so it can be shown
    // as held/spent, but it is never clickable -- the engine spends it on its own.
    private static final Lifeline[] LIFELINE_ORDER = {
            Lifeline.BINARY_CHOICE, Lifeline.PREDICT, Lifeline.OVERRIDE };
    private static final String[] LIFELINE_LABELS = { "BC", "PR", "OV" };

    // figma "colors" variable collection (KJB4zCiGEjWFT3RivwklQM, id
    // VariableCollectionId:3:2)
    private static final Color PAPER = new Color(0xED, 0xE3, 0xCD);
    private static final Color PAPER_DARK = new Color(0xE2, 0xD5, 0xB8);
    private static final Color CHALKBOARD = new Color(0x2E, 0x3D, 0x34);
    private static final Color CHALK_LINE = new Color(0xDC, 0xD6, 0xC0);
    private static final Color BRASS = new Color(0xA9, 0x86, 0x3F);
    private static final Color BRASS_DARK = new Color(0x7C, 0x60, 0x27);
    private static final Color TAPE_RED = new Color(0x8C, 0x3B, 0x2E);
    private static final Color TAPE_GREEN = new Color(0x4C, 0x6B, 0x4F);
    private static final Color MONITOR_BG = new Color(0x14, 0x12, 0x0E);
    private static final Color MONITOR_GREEN = new Color(0x6F, 0xE3, 0x9A);

    private static final Font FONT_HEADING = new Font(Font.MONOSPACED, Font.BOLD, 22);
    private static final Font FONT_BODY = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final Font FONT_LABEL = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font FONT_BANNER = new Font(Font.MONOSPACED, Font.BOLD, 15);

    private final CardPanel cardPanel;
    private final JLabel[] ladderSegments = new JLabel[TOTAL_QUESTIONS];
    private final Color[] ladderOutcomes = new Color[TOTAL_QUESTIONS]; // null = not answered yet
    private final JButton[] lifelineButtons = new JButton[LIFELINE_ORDER.length];
    private final JLabel questionLabel;
    private final JButton[] choiceButtons = new JButton[4];
    private final AiMonitorPanel monitorPanel;
    private final JLabel bannerLabel;
    private final JButton nextButton;

    private GameEngine engine;
    private int questionIndex; // 0-based position in the current session
    private boolean sessionOver;

    // the choices exactly as currently displayed -- index i is what button i shows.
    private List<Choice> currentChoices = List.of();

    // choiceIds Binary Choice removed from the CURRENT question. Cleared when the
    // question changes, kept across an Override retry (the lifeline was already
    // spent).
    private final Set<String> removedChoiceIds = new HashSet<>();

    public PlayPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setBackground(PAPER);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(PAPER);

        JPanel ladderPanel = new JPanel(new GridLayout(1, TOTAL_QUESTIONS, 4, 0));
        ladderPanel.setBackground(PAPER);
        for (int i = 0; i < TOTAL_QUESTIONS; i++) {
            JLabel segment = new JLabel();
            segment.setOpaque(true);
            segment.setBackground(PAPER_DARK);
            segment.setPreferredSize(new Dimension(20, 12));
            ladderSegments[i] = segment;
            ladderPanel.add(segment);
        }
        topPanel.add(ladderPanel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel lifelinePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        lifelinePanel.setBackground(PAPER);
        for (int i = 0; i < lifelineButtons.length; i++) {
            Lifeline lifeline = LIFELINE_ORDER[i];
            JButton button = createLifelineButton(LIFELINE_LABELS[i]);
            if (lifeline == Lifeline.OVERRIDE) {
                // Override is spent automatically by the engine on a wrong answer;
                // useLifeline(OVERRIDE) throws by design. Display only.
                button.setEnabled(false);
                button.setToolTipText("Override: absorbs your first wrong answer automatically");
            } else {
                button.setToolTipText(lifeline == Lifeline.BINARY_CHOICE
                        ? "Binary Choice: removes two wrong answers"
                        : "Predict: the AI suggests an answer (not always right)");
                button.addActionListener(e -> useLifeline(lifeline));
            }
            lifelineButtons[i] = button;
            lifelinePanel.add(button);
        }
        topPanel.add(lifelinePanel);

        add(topPanel, BorderLayout.NORTH);

        JPanel questionCard = new JPanel();
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        questionCard.setBackground(CHALKBOARD);
        questionCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BRASS, 3),
                BorderFactory.createEmptyBorder(30, 35, 30, 35)));

        questionLabel = new JLabel(" ");
        questionLabel.setForeground(CHALK_LINE);
        questionLabel.setFont(FONT_HEADING);
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(questionLabel);
        questionCard.add(Box.createVerticalStrut(25));

        for (int i = 0; i < choiceButtons.length; i++) {
            JButton choiceButton = createChoiceButton();
            final int position = i;
            // Submit the choiceId of whatever is SHOWN in this slot right now, looked
            // up at click time. Binding ('a' + i) here instead would be wrong: the
            // bank shuffles each question's choices, so slot 0 is not always "a".
            choiceButton.addActionListener(e -> submitAnswerAt(position));
            choiceButtons[i] = choiceButton;
            questionCard.add(choiceButton);
            questionCard.add(Box.createVerticalStrut(14));
        }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(PAPER);
        centerWrapper.add(questionCard, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 12));
        bottomPanel.setBackground(PAPER);

        bannerLabel = new JLabel(" ", SwingConstants.CENTER);
        bannerLabel.setOpaque(true);
        bannerLabel.setFont(FONT_BANNER);
        bannerLabel.setForeground(PAPER);
        bannerLabel.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        bannerLabel.setBackground(PAPER);
        bottomPanel.add(bannerLabel, BorderLayout.NORTH);

        JPanel navRow = new JPanel(new BorderLayout());
        navRow.setBackground(PAPER);

        monitorPanel = new AiMonitorPanel();
        JPanel monitorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        monitorWrapper.setBackground(PAPER);
        monitorWrapper.add(monitorPanel);
        navRow.add(monitorWrapper, BorderLayout.WEST);

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        navButtons.setBackground(PAPER);
        JButton backButton = createLifelineButton("Back");
        backButton.addActionListener(e -> leaveSession());
        nextButton = createLifelineButton("Next");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            if (sessionOver) {
                leaveSession();
            } else {
                showQuestion();
            }
        });
        navButtons.add(backButton);
        navButtons.add(nextButton);
        navRow.add(navButtons, BorderLayout.EAST);

        bottomPanel.add(navRow, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createLifelineButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_LABEL);
        button.setForeground(PAPER);
        button.setBackground(BRASS);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return button;
    }

    private JButton createChoiceButton() {
        JButton button = new JButton();
        button.setFont(FONT_BODY);
        button.setForeground(CHALK_LINE);
        button.setBackground(CHALKBOARD);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setForeground(BRASS);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setForeground(button.isEnabled() ? CHALK_LINE : BRASS_DARK);
            }
        });
        return button;
    }

    /**
     * Starts rendering a session that SetupPanel has already started on the engine.
     */

    @Override
    public void startGame(GameEngine engine) {
        this.engine = engine;
        this.questionIndex = 0;
        this.sessionOver = false;
        Arrays.fill(ladderOutcomes, null);
        removedChoiceIds.clear();
        showQuestion();
    }

    /**
     * Leaves the question screen. A session still in progress is ended here --
     * that's end condition (c), "Return", in CONTEXT.md 2.5, and without this
     * call the engine would sit believing the run is still live.
     */
    private void leaveSession() {
        if (engine != null && !sessionOver) {
            engine.endSession();
            sessionOver = true;
        }
        cardPanel.showScreen(CardPanel.MENU);
    }

    private void showQuestion() {
        removedChoiceIds.clear();
        Question question = engine.currentQuestion();
        questionLabel.setText(question.getText());
        currentChoices = question.getChoices();

        refreshChoices();
        refreshLadder();
        refreshLifelines();

        bannerLabel.setText(" ");
        bannerLabel.setBackground(PAPER);
        monitorPanel.setMode(AiMonitorPanel.Mode.TENSE);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.revalidate();
        nextButton.repaint();
    }

    /**
     * Repaints the four answer slots from currentChoices. Slots holding a choice
     * Binary Choice removed are struck through and disabled but keep their letter,
     * so the remaining letters don't shuffle under the player mid-question.
     */
    private void refreshChoices() {
        for (int i = 0; i < choiceButtons.length; i++) {
            JButton button = choiceButtons[i];
            if (i >= currentChoices.size()) {
                button.setVisible(false);
                continue;
            }
            Choice choice = currentChoices.get(i);
            boolean removed = removedChoiceIds.contains(choice.getChoiceId());
            button.setVisible(true);
            button.setText((char) ('A' + i) + ".  " + (removed
                    ? "<html><strike>" + choice.getText() + "</strike></html>"
                    : choice.getText()));
            button.setEnabled(!removed && !sessionOver);
            button.setForeground(removed ? BRASS_DARK : CHALK_LINE);
        }
    }

    private void refreshLadder() {
        for (int i = 0; i < ladderSegments.length; i++) {
            Color outcome = ladderOutcomes[i];
            ladderSegments[i].setBackground(outcome != null ? outcome : i == questionIndex ? BRASS : PAPER_DARK);
        }
    }

    /**
     * Enabled state comes from the engine's remaining set, never from a local
     * counter -- High Risk never grants Override, and a spent lifeline must stay
     * spent across questions.
     */
    private void refreshLifelines() {
        Set<Lifeline> remaining = engine.getRemainingLifelines();
        for (int i = 0; i < lifelineButtons.length; i++) {
            Lifeline lifeline = LIFELINE_ORDER[i];
            boolean held = remaining.contains(lifeline);
            boolean clickable = held && !sessionOver && lifeline != Lifeline.OVERRIDE;
            lifelineButtons[i].setEnabled(clickable);
            lifelineButtons[i].setBackground(held && !sessionOver ? BRASS : PAPER_DARK);
        }
    }

    private void useLifeline(Lifeline lifeline) {
        LifelineResult result;
        try {
            result = engine.useLifeline(lifeline);
        } catch (RuntimeException e) {
            // Shouldn't happen -- the button is disabled when unavailable -- but a
            // stale click beats a stack trace in the console.
            refreshLifelines();
            return;
        }

        if (lifeline == Lifeline.BINARY_CHOICE) {
            removedChoiceIds.addAll(result.getRemovedChoiceIds());
            refreshChoices();
            bannerLabel.setBackground(BRASS_DARK);
            bannerLabel.setText("BINARY CHOICE — two wrong answers removed");
        } else {
            int position = positionOfChoiceId(result.getPredictedChoiceId());
            String letter = position >= 0 ? String.valueOf((char) ('A' + position)) : "?";
            if (position >= 0) {
                choiceButtons[position].setForeground(BRASS);
            }
            bannerLabel.setBackground(BRASS_DARK);
            bannerLabel.setText("PREDICT — the AI suggests " + letter
                    + " at " + result.getConfidencePercent() + "% confidence (it can be wrong)");
        }
        refreshLifelines();
    }

    /** Display position of a choiceId in the current question, or -1 if absent. */
    private int positionOfChoiceId(String choiceId) {
        for (int i = 0; i < currentChoices.size(); i++) {
            if (currentChoices.get(i).getChoiceId().equals(choiceId)) {
                return i;
            }
        }
        return -1;
    }

    /** Answers with whatever choice is displayed in the given slot. */
    private void submitAnswerAt(int position) {
        Question answeredQuestion = engine.currentQuestion();
        Choice picked = currentChoices.get(position);
        for (JButton choiceButton : choiceButtons) {
            choiceButton.setEnabled(false);
        }

        TurnResult result = engine.submitAnswer(picked.getChoiceId());

        if (result.isRetry()) {
            // Override absorbed the miss. The engine did NOT advance: same question,
            // no ladder mark, no index bump, and no explanation -- printing it would
            // hand over the answer the player is about to re-attempt.
            bannerLabel.setBackground(BRASS_DARK);
            bannerLabel.setText("OVERRIDE ENGAGED — no tokens lost. Try this question again.");
            monitorPanel.setMode(AiMonitorPanel.Mode.FROWN);
            refreshChoices();
            refreshLifelines();
            return;
        }

        ladderOutcomes[questionIndex] = result.isCorrect() ? TAPE_GREEN : TAPE_RED;
        questionIndex++;
        sessionOver = result.isGameOver();
        refreshLadder();

        bannerLabel.setForeground(PAPER);
        if (result.isCorrect()) {
            bannerLabel.setBackground(TAPE_GREEN);
            String text = "CORRECT — TRAINING SCORE +" + result.getTokensAwarded()
                    + "  (total: " + result.getRunningTotal() + ")";
            if (result.isCapabilityUnlocked()) {
                text += "  |  " + result.getCapabilityName().toUpperCase() + " RESTORED";
            }
            bannerLabel.setText(text);
            monitorPanel.setMode(AiMonitorPanel.Mode.TALKING);
        } else {
            bannerLabel.setBackground(TAPE_RED);
            bannerLabel.setText("INCORRECT — " + answeredQuestion.getExplanation());
            monitorPanel.setMode(AiMonitorPanel.Mode.FROWN);
        }

        refreshLifelines();

        nextButton.setText(sessionOver ? "Back to Menu" : "Next");
        nextButton.setEnabled(true);
        // aqua sometimes doesn't repaint a re-enabled button on its own, force it
        nextButton.revalidate();
        nextButton.repaint();
    }

    // small crt-style "ai vitals" readout, same monitor shown on every screen in
    // the figma file
    private static class AiMonitorPanel extends JComponent {

        enum Mode {
            TENSE, TALKING, FROWN
        }

        private Mode mode = Mode.TENSE;

        AiMonitorPanel() {
            setPreferredSize(new Dimension(130, 95));
        }

        void setMode(Mode mode) {
            this.mode = mode;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(BRASS);
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(MONITOR_BG);
            g2.fillRoundRect(6, 6, w - 12, h - 12, 6, 6);

            if (mode == Mode.TENSE) {
                g2.setColor(new Color(0xE0, 0x6A, 0x45));
                g2.setStroke(new BasicStroke(2f));
                int midY = h / 2;
                int[] xs = { 14, 30, 40, 48, 56, 64, 74, w - 14 };
                int[] ys = { midY, midY, midY - 20, midY + 24, midY - 14, midY, midY, midY };
                g2.drawPolyline(xs, ys, xs.length);
            } else {
                g2.setColor(MONITOR_GREEN.darker());
                g2.drawLine(16, 22, w - 16, 22);

                g2.setColor(MONITOR_GREEN);
                g2.setStroke(new BasicStroke(2.5f));
                int eyeY = h / 2 - 5;
                g2.drawOval(w / 2 - 22, eyeY, 8, 8);
                g2.drawOval(w / 2 + 10, eyeY, 8, 8);

                int mouthY = h / 2 + 16;
                if (mode == Mode.TALKING) {
                    g2.drawArc(w / 2 - 18, mouthY - 8, 36, 16, 200, 140);
                } else {
                    g2.drawArc(w / 2 - 18, mouthY, 36, 16, 20, 140);
                }
            }

            g2.dispose();
        }
    }
}
