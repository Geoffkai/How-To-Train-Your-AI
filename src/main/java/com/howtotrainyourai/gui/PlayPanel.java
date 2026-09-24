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
import java.util.List;

// question screen: 15-segment ladder, 3 lifeline buttons, current question + 4 choices,
// result banner, ai vitals monitor. matches the "simple version" question/correct/incorrect
// frames in the team figma file (node 12:188/12:189/12:190), palette pulled from its
// colors variable collection.
//
// session start (trainer name + protocol) and real lifeline behavior belong to other weeks'
// owners, this just renders whatever GameEngine hands back per its "single seam" comment.
// call startGame() once a session's been started on an engine to start showing questions.
public class PlayPanel extends JPanel {

    private static final int TOTAL_QUESTIONS = 15;

    // figma "colors" variable collection (KJB4zCiGEjWFT3RivwklQM, id VariableCollectionId:3:2)
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
    private final JButton[] lifelineButtons = new JButton[3];
    private final JLabel questionLabel;
    private final JButton[] choiceButtons = new JButton[4];
    private final AiMonitorPanel monitorPanel;
    private final JLabel bannerLabel;
    private final JButton nextButton;

    private GameEngine engine;
    private int questionIndex; // 0-based position in the current session
    private boolean sessionOver;

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
        String[] lifelineLabels = { "BC", "PR", "OV" };
        for (int i = 0; i < lifelineButtons.length; i++) {
            JButton button = createLifelineButton(lifelineLabels[i]);
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
            final String choiceId = String.valueOf((char) ('a' + i));
            choiceButton.addActionListener(e -> submitAnswer(choiceId));
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
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        nextButton = createLifelineButton("Next");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            if (sessionOver) {
                cardPanel.showScreen(CardPanel.MENU);
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
        // binary choice/predict/override behavior is week 4 scope, layout only here
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
                button.setForeground(CHALK_LINE);
            }
        });
        return button;
    }

    // starts rendering a session that's already been started on engine
    public void startGame(GameEngine engine) {
        this.engine = engine;
        this.questionIndex = 0;
        this.sessionOver = false;
        java.util.Arrays.fill(ladderOutcomes, null);
        showQuestion();
    }

    private void showQuestion() {
        Question question = engine.currentQuestion();
        questionLabel.setText(question.getText());

        List<Choice> choices = question.getChoices();
        for (int i = 0; i < choiceButtons.length; i++) {
            Choice choice = choices.get(i);
            choiceButtons[i].setText((char) ('A' + i) + ".  " + choice.getText());
            choiceButtons[i].setForeground(CHALK_LINE);
            choiceButtons[i].setEnabled(true);
        }

        refreshLadder();
        for (JButton lifelineButton : lifelineButtons) {
            lifelineButton.setEnabled(true);
            lifelineButton.setBackground(BRASS);
        }

        bannerLabel.setText(" ");
        bannerLabel.setBackground(PAPER);
        monitorPanel.setMode(AiMonitorPanel.Mode.TENSE);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.revalidate();
        nextButton.repaint();
    }

    private void refreshLadder() {
        for (int i = 0; i < ladderSegments.length; i++) {
            Color outcome = ladderOutcomes[i];
            ladderSegments[i].setBackground(outcome != null ? outcome : i == questionIndex ? BRASS : PAPER_DARK);
        }
    }

    private void submitAnswer(String choiceId) {
        Question answeredQuestion = engine.currentQuestion();
        for (JButton choiceButton : choiceButtons) {
            choiceButton.setEnabled(false);
        }

        TurnResult result = engine.submitAnswer(choiceId);
        ladderOutcomes[questionIndex] = result.isCorrect() ? TAPE_GREEN : TAPE_RED;
        questionIndex++;
        sessionOver = result.isGameOver();
        refreshLadder();

        bannerLabel.setForeground(PAPER);
        if (result.isCorrect()) {
            bannerLabel.setBackground(TAPE_GREEN);
            String text = "CORRECT — TRAINING SCORE +" + result.getTokensAwarded();
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

        if (sessionOver) {
            for (JButton lifelineButton : lifelineButtons) {
                lifelineButton.setEnabled(false);
                lifelineButton.setBackground(PAPER_DARK);
            }
        }

        nextButton.setText(sessionOver ? "Back to Menu" : "Next");
        nextButton.setEnabled(true);
        // aqua sometimes doesn't repaint a re-enabled button on its own, force it
        nextButton.revalidate();
        nextButton.repaint();
    }

    // small crt-style "ai vitals" readout, same monitor shown on every screen in the figma file
    private static class AiMonitorPanel extends JComponent {

        enum Mode { TENSE, TALKING, FROWN }

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

    // manual visual check only, GameEngineImpl (real rules/ladder) is week 2 work owned
    // elsewhere. this stubs just enough of the interface to click through the layout.
    public static void main(String[] args) throws Exception {
        // aqua (macos) ignores custom JButton colors/borders otherwise, brass buttons
        // render as invisible/unstyled text instead
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

        JFrame frame = new JFrame("PlayPanel demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PlayPanel panel = new PlayPanel(new CardPanel());
        frame.add(panel);
        frame.setSize(1000, 720);
        frame.setVisible(true);
        panel.startGame(demoEngine);
    }
}
