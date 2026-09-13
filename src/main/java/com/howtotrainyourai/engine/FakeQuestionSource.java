package com.howtotrainyourai.engine;

import com.howtotrainyourai.model.Choice;
import com.howtotrainyourai.model.Question;
import java.util.ArrayList;
import java.util.List;

/**
 * Hardcoded, in-memory QuestionSource used only for Week 1's terminal loop.
 * No file I/O, no shuffling — just 15 dummy questions so GameEngine/
 * TerminalGameLoop have something real to run against before R2's real
 * loader exists.
 */
public class FakeQuestionSource implements QuestionSource {

        @Override
        public List<Question> buildSession() {
                // TODO: Build and return a List<Question> with EXACTLY 15 entries.
                //
                // For each question you need two things:
                //
                // 1) Its 4 Choices — new Choice(choiceId, text), where choiceId is
                // the letter "a", "b", "c", or "d" (this is the ORIGINAL letter,
                // see Choice.java's comments — doesn't matter for this fake data
                // since nothing shuffles it, but keep the habit).
                //
                // Example shape (don't copy verbatim — this is illustrative):
                // Choice choiceA = new Choice("a", "some answer text");
                // Choice choiceB = new Choice("b", "some other text");
                // ... etc for c and d
                //
                // 2) The Question itself — check Question.java's constructor for the
                // exact parameter order: (id, module, bloom, text, explanation,
                // choices, correctChoiceId).
                //
                // - id: anything unique per question, e.g. "fake-01"
                // - module: can be a dummy value like "week01" for all of them —
                // doesn't matter yet, nothing reads it this week
                // - bloom: use the 6 real stage names since TerminalGameLoop might
                // want to print them later — Remember, Understand, Apply,
                // Analyze, Evaluate, Synthesis
                // - choices: a List<Choice> containing the 4 you built above
                // (List.of(choiceA, choiceB, choiceC, choiceD) works)
                // - correctChoiceId: the letter of whichever choice you decided
                // is correct — must match one of the choiceIds you used above
                //
                // Repeat that 15 times (placeholder/dummy question text is totally
                // fine, this data is throwaway), collect the 15 Questions into a
                // List, and return it.
                //
                // Tip: writing one helper method that builds a single Question from
                // a few plain arguments will save you from repeating the Choice
                // boilerplate 15 times — but that's your call, not a requirement.
                ArrayList<Question> questions = new ArrayList<>();
                ArrayList<Choice> choices1 = new ArrayList<>();
                choices1.add(new Choice("a", "dummychoice1a"));
                choices1.add(new Choice("b", "dummychoice1b"));
                choices1.add(new Choice("c", "dummychoice1c"));
                choices1.add(new Choice("d", "dummychoice1d"));
                Question question1 = new Question("fake01", "1", "Remember", "Dummy Question 1", "Dummy Explanation 1",
                                choices1, "a");

                ArrayList<Choice> choices2 = new ArrayList<>();
                choices2.add(new Choice("a", "dummychoice2a"));
                choices2.add(new Choice("b", "dummychoice2b"));
                choices2.add(new Choice("c", "dummychoice2c"));
                choices2.add(new Choice("d", "dummychoice2d"));
                Question question2 = new Question("fake02", "1", "Remember", "Dummy Question 2", "Dummy Explanation 2",
                                choices2, "c");

                ArrayList<Choice> choices3 = new ArrayList<>();
                choices3.add(new Choice("a", "dummychoice3a"));
                choices3.add(new Choice("b", "dummychoice3b"));
                choices3.add(new Choice("c", "dummychoice3c"));
                choices3.add(new Choice("d", "dummychoice3d"));
                Question question3 = new Question("fake03", "2", "Remember", "Dummy Question 3",
                                "Dummy Explanation 3",
                                choices3, "a");

                ArrayList<Choice> choices4 = new ArrayList<>();
                choices4.add(new Choice("a", "dummychoice4a"));
                choices4.add(new Choice("b", "dummychoice4b"));
                choices4.add(new Choice("c", "dummychoice4c"));
                choices4.add(new Choice("d", "dummychoice4d"));
                Question question4 = new Question("fake04", "2", "Understand", "Dummy Question 4",
                                "Dummy Explanation 4",
                                choices4, "d");

                ArrayList<Choice> choices5 = new ArrayList<>();
                choices5.add(new Choice("a", "dummychoice5a"));
                choices5.add(new Choice("b", "dummychoice5b"));
                choices5.add(new Choice("c", "dummychoice5c"));
                choices5.add(new Choice("d", "dummychoice5d"));
                Question question5 = new Question("fake05", "3", "Understand", "Dummy Question 5",
                                "Dummy Explanation 5",
                                choices5, "b");

                ArrayList<Choice> choices6 = new ArrayList<>();
                choices6.add(new Choice("a", "dummychoice6a"));
                choices6.add(new Choice("b", "dummychoice6b"));
                choices6.add(new Choice("c", "dummychoice6c"));
                choices6.add(new Choice("d", "dummychoice6d"));
                Question question6 = new Question("fake06", "3", "Apply", "Dummy Question 6", "Dummy Explanation 6",
                                choices6, "a");

                ArrayList<Choice> choices7 = new ArrayList<>();
                choices7.add(new Choice("a", "dummychoice7a"));
                choices7.add(new Choice("b", "dummychoice7b"));
                choices7.add(new Choice("c", "dummychoice7c"));
                choices7.add(new Choice("d", "dummychoice7d"));
                Question question7 = new Question("fake07", "4", "Apply", "Dummy Question 7", "Dummy Explanation 7",
                                choices7, "b");

                ArrayList<Choice> choices8 = new ArrayList<>();
                choices8.add(new Choice("a", "dummychoice8a"));
                choices8.add(new Choice("b", "dummychoice8b"));
                choices8.add(new Choice("c", "dummychoice8c"));
                choices8.add(new Choice("d", "dummychoice8d"));
                Question question8 = new Question("fake08", "4", "Apply", "Dummy Question 8", "Dummy Explanation 8",
                                choices8, "a");

                ArrayList<Choice> choices9 = new ArrayList<>();
                choices9.add(new Choice("a", "dummychoice9a"));
                choices9.add(new Choice("b", "dummychoice9b"));
                choices9.add(new Choice("c", "dummychoice9c"));
                choices9.add(new Choice("d", "dummychoice9d"));
                Question question9 = new Question("fake09", "5", "Analyze", "Dummy Question 9", "Dummy Explanation 9",
                                choices9, "d");

                ArrayList<Choice> choices10 = new ArrayList<>();
                choices10.add(new Choice("a", "dummychoice10a"));
                choices10.add(new Choice("b", "dummychoice10b"));
                choices10.add(new Choice("c", "dummychoice10c"));
                choices10.add(new Choice("d", "dummychoice10d"));
                Question question10 = new Question("fake10", "5", "Analyze", "Dummy Question 10",
                                "Dummy Explanation 10",
                                choices10, "d");

                ArrayList<Choice> choices11 = new ArrayList<>();
                choices11.add(new Choice("a", "dummychoice11a"));
                choices11.add(new Choice("b", "dummychoice11b"));
                choices11.add(new Choice("c", "dummychoice11c"));
                choices11.add(new Choice("d", "dummychoice11d"));
                Question question11 = new Question("fake11", "6", "Evaluate", "Dummy Question 11",
                                "Dummy Explanation 11",
                                choices11, "b");

                ArrayList<Choice> choices12 = new ArrayList<>();
                choices12.add(new Choice("a", "dummychoice12a"));
                choices12.add(new Choice("b", "dummychoice12b"));
                choices12.add(new Choice("c", "dummychoice12c"));
                choices12.add(new Choice("d", "dummychoice12d"));
                Question question12 = new Question("fake12", "6", "Evaluate", "Dummy Question 12",
                                "Dummy Explanation 12",
                                choices12, "b");

                ArrayList<Choice> choices13 = new ArrayList<>();
                choices13.add(new Choice("a", "dummychoice13a"));
                choices13.add(new Choice("b", "dummychoice13b"));
                choices13.add(new Choice("c", "dummychoice13c"));
                choices13.add(new Choice("d", "dummychoice13d"));
                Question question13 = new Question("fake13", "7", "Evaluate", "Dummy Question 13",
                                "Dummy Explanation 13", choices13, "a");

                ArrayList<Choice> choices14 = new ArrayList<>();
                choices14.add(new Choice("a", "dummychoice14a"));
                choices14.add(new Choice("b", "dummychoice14b"));
                choices14.add(new Choice("c", "dummychoice14c"));
                choices14.add(new Choice("d", "dummychoice14d"));
                Question question14 = new Question("fake14", "7", "Synthesis", "Dummy Question 14",
                                "Dummy Explanation 14", choices14, "c"

                ArrayList<Choice> choices15 = new ArrayList<>();
                choices15.add(new Choice("a", "dummychoice15a"));
                choices15.add(new Choice("b", "dummychoice15b"));
                choices15.add(new Choice("c", "dummychoice15c"));
                choices15.add(new Choice("d", "dummychoice15d"));
                Question question15 = new Question("fake15", "8", "Synthesis", "Dummy Question 15",
                                "Dummy Explanation 15", choices15, "c");

                questions.add(question1);
                questions.add(question2);
                questions.add(question3);
                questions.add(question4);
                questions.add(question5);
                questions.add(question6);
                questions.add(question7);
                questions.add(question8);
                questions.add(question9);
                questions.add(question10);
                questions.add(question11);
                questions.add(question12);
                questions.add(question13);
                questions.add(question14);
                questions.add(question15);
                return questions; // replace with your finished List<Question>
        }
}
