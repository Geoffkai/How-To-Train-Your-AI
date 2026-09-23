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
// result banner after each answer (CONTEXT.md 2.6).
//
// session start (trainer name + protocol) and real lifeline behavior belong to other weeks'
// owners, this just renders whatever GameEngine hands back per its "single seam" comment.
// call startGame() once a session's been started on an engine to start showing questions.
public class PlayPanel extends JPanel {

    private static final int TOTAL_QUESTIONS = 15;
    private static final Color BOARD_BG = new Color(0x2f3b30);
    private static final Color CORRECT_BG = new Color(0x4a7a4a);
    private static final Color WRONG_BG = new Color(0x8a3a3a);
    private static final Color PENDING_SEGMENT = new Color(224, 224, 224);
    private static final Color CURRENT_SEGMENT = new Color(0xc9, 0xa2, 0x27);

    private final CardPanel cardPanel;
    private final JLabel[] ladderSegments = new JLabel[TOTAL_QUESTIONS];
    private final JLabel questionLabel;
    private final JButton[] choiceButtons = new JButton[4];
    private final JLabel bannerLabel;
    private final JButton nextButton;

    private GameEngine engine;
    private int questionIndex; // 0-based position in the current session
    private boolean sessionOver;

    public PlayPanel(CardPanel cardPanel) {
        this.cardPanel = cardPanel;
        setBackground(Color.WHITE);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Color.WHITE);

        JPanel ladderPanel = new JPanel(new GridLayout(1, TOTAL_QUESTIONS, 3, 0));
        ladderPanel.setBackground(Color.WHITE);
        for (int i = 0; i < TOTAL_QUESTIONS; i++) {
            JLabel segment = new JLabel();
            segment.setOpaque(true);
            segment.setBackground(PENDING_SEGMENT);
            segment.setPreferredSize(new Dimension(20, 14));
            ladderSegments[i] = segment;
            ladderPanel.add(segment);
        }
        topPanel.add(ladderPanel, BorderLayout.CENTER);

        JPanel lifelinePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        lifelinePanel.setBackground(Color.WHITE);
        lifelinePanel.add(createLifelineButton("BC"));
        lifelinePanel.add(createLifelineButton("PR"));
        lifelinePanel.add(createLifelineButton("OV"));
        topPanel.add(lifelinePanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JPanel questionCard = new JPanel();
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        questionCard.setBackground(BOARD_BG);
        questionCard.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        questionLabel = new JLabel(" ");
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(questionLabel);
        questionCard.add(Box.createVerticalStrut(20));

        for (int i = 0; i < choiceButtons.length; i++) {
            JButton choiceButton = new JButton();
            choiceButton.setFont(new Font("SansSerif", Font.PLAIN, 16));
            choiceButton.setHorizontalAlignment(SwingConstants.LEFT);
            choiceButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            choiceButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            choiceButton.setBackground(new Color(0x3d, 0x4c, 0x3e));
            choiceButton.setForeground(Color.WHITE);
            choiceButton.setFocusPainted(false);
            final String choiceId = String.valueOf((char) ('a' + i));
            choiceButton.addActionListener(e -> submitAnswer(choiceId));
            choiceButtons[i] = choiceButton;
            questionCard.add(choiceButton);
            questionCard.add(Box.createVerticalStrut(8));
        }

        add(questionCard, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);

        bannerLabel = new JLabel(" ", SwingConstants.CENTER);
        bannerLabel.setOpaque(true);
        bannerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        bannerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bannerLabel.setBackground(Color.WHITE);
        bottomPanel.add(bannerLabel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        navPanel.setBackground(Color.WHITE);
        JButton backButton = new JButton("Back");
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> cardPanel.showScreen(CardPanel.MENU));
        nextButton = new JButton("Next");
        nextButton.setFocusPainted(false);
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            if (sessionOver) {
                cardPanel.showScreen(CardPanel.MENU);
            } else {
                showQuestion();
            }
        });
        navPanel.add(backButton);
        navPanel.add(nextButton);
        bottomPanel.add(navPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createLifelineButton(String label) {
        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(PENDING_SEGMENT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        // binary choice/predict/override behavior is week 4 scope, layout only here
        return button;
    }

    // starts rendering a session that's already been started on engine
    public void startGame(GameEngine engine) {
        this.engine = engine;
        this.questionIndex = 0;
        this.sessionOver = false;
        showQuestion();
    }

    private void showQuestion() {
        Question question = engine.currentQuestion();
        questionLabel.setText(question.getText());

        List<Choice> choices = question.getChoices();
        for (int i = 0; i < choiceButtons.length; i++) {
            Choice choice = choices.get(i);
            choiceButtons[i].setText((char) ('A' + i) + ".  " + choice.getText());
            choiceButtons[i].setEnabled(true);
        }

        for (int i = 0; i < ladderSegments.length; i++) {
            ladderSegments[i].setBackground(
                    i < questionIndex ? CORRECT_BG : i == questionIndex ? CURRENT_SEGMENT : PENDING_SEGMENT);
        }

        bannerLabel.setText(" ");
        bannerLabel.setBackground(Color.WHITE);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
    }

    private void submitAnswer(String choiceId) {
        Question answeredQuestion = engine.currentQuestion();
        for (JButton choiceButton : choiceButtons) {
            choiceButton.setEnabled(false);
        }

        TurnResult result = engine.submitAnswer(choiceId);
        questionIndex++;
        sessionOver = result.isGameOver();

        bannerLabel.setForeground(Color.WHITE);
        if (result.isCorrect()) {
            bannerLabel.setBackground(CORRECT_BG);
            String text = "CORRECT - Training Score +" + result.getTokensAwarded()
                    + " (Total: " + result.getRunningTotal() + ")";
            if (result.isCapabilityUnlocked()) {
                text += "  |  " + result.getCapabilityName() + " restored!";
            }
            bannerLabel.setText(text);
        } else {
            bannerLabel.setBackground(WRONG_BG);
            bannerLabel.setText("INCORRECT - " + answeredQuestion.getExplanation());
        }

        nextButton.setText(sessionOver ? "Back to Menu" : "Next");
        nextButton.setEnabled(true);
    }

    // manual visual check only, GameEngineImpl (real rules/ladder) is week 2 work owned
    // elsewhere. this stubs just enough of the interface to click through the layout.
    public static void main(String[] args) {
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
        frame.setSize(900, 650);
        frame.setVisible(true);
        panel.startGame(demoEngine);
    }
}
