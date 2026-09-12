# *How to Train Your AI* — Work Plan v2

**Deadline:** Thursday, October 15, 2026
**Today:** Tuesday, September 8, 2026
**Time available:** 5 weeks + 2 days
**Team:** 4 · **Stack:** Java + Swing · **Data:** CSV files, no database

---

## 0. Reality check

5 weeks is enough, but only if Week 3 goes well.

The single biggest risk in a 4-person project is that everyone builds their own piece beautifully in isolation, then discovers in Week 5 that the pieces don't fit. **Week 3 is integration week.** Everything before it exists to make Week 3 possible. Everything after it is features and polish.

Your leader's plan for this week is correct — data layer first, game logic second, screens third. One gap: **that's 3 tasks for 4 people.** Task 4 is below.

---

## 1. The four roles (carried across all 5 weeks)

| # | Role | Owns |
|---|---|---|
| **R1** | **Engine + Integration + Packaging** | Game flow, scoring, checkpoints, protocols. Git repo. Final `.exe` + `ReadMeFirst.txt`. |
| **R2** | **Data + Randomization** | CSV format, loader, question selection, shuffling, records file. |
| **R3** | **Screens + Navigation + Theme** | Main menu, tutorial, options, splash, protocol select, records screen, scaling fix. |
| **R4** | **Gameplay Screen + Lifelines + Audio + Endings** | The question screen itself, all 3 lifelines, AudioManager, win/fail sequences. |

**Why R3 and R4 are split this way:** the lifelines all live *on* the question screen (dimming choices, the Predict popup, the Override retry state). Keeping the question screen and lifelines with one person avoids two people editing the same file all project.

**Load balance:** R1 and R2 carry harder logic. R3 carries more files but simpler ones. R4 carries the most variety. Packaging sits with R1 because they already own the repo and the build.

---

## 2. Do this before anyone writes real code (this week, ~90 min)

### 2a. Lock the CSV format

**Use `|` (pipe) as your delimiter, not a comma.**

Your questions will contain commas. "Which of the following, when applied to a state space, ..." — `split(",")` will shred that into pieces and your loader will silently produce garbage. Commas inside CSV fields require quote-handling logic that you do not want to write this week.

Pipes never appear in normal English text. Problem gone.

**Proposed file layout — `MCQ/week03.csv`:**

```
id|module|bloom|question|choiceA|choiceB|choiceC|choiceD|correct|explanation
w03-q001|week03|Apply|Which search strategy fits this scenario?|Breadth-first|A* search|Depth-first|Hill climbing|B|A* uses a heuristic to...
```

Rules everyone writing questions must follow:
- One question per line, no line breaks inside a field
- No `|` characters anywhere in your text
- `correct` is a single letter: A, B, C, or D
- `bloom` is exactly one of: `Remember`, `Understand`, `Apply`, `Analyze`, `Evaluate`, `Synthesis`
- First line of every file is the header row — your loader skips it

### 2b. Write the shared classes together (empty bodies)

```java
public class Question {
    String id, module, bloom, text, explanation;
    List<Choice> choices;   // already shuffled when handed to the GUI
    boolean isCorrect(String choiceId) { /* TODO */ }
}

public class Choice {
    String choiceId;   // "a".."d" — the ORIGINAL letter, never the display position
    String text;
}

public interface QuestionSource {
    List<Question> buildSession();   // exactly 15, in Bloom order
}

public interface GameEngine {
    void startSession(String trainerName, Protocol protocol);
    Question currentQuestion();
    TurnResult submitAnswer(String choiceId);
    SessionResult endSession();
}

public interface AudioManager {
    void playMusic(String trackId);
    void playSfx(String sfxId);
    void setMusicVolume(int percent);
    void setSfxVolume(int percent);
}

public class TurnResult {
    boolean correct;
    int tokensAwarded, runningTotal;
    boolean capabilityUnlocked;
    String capabilityName;
    boolean gameOver;
}
```

Once these are committed, **nobody waits for anybody.** R3 builds screens against fake `Question` objects. R1 runs the game loop against a fake `QuestionSource`. R4 tests audio against nothing at all.

### 2c. Divide the modules for question writing

Split your modules 4 ways. Each person writes questions for their own modules, in the pipe format above. Not one person's job.

---

## 3. Week-by-week

### Week 1 · Sept 8–14 — Foundations
*Your leader's plan, with the fourth task added.*

| | Task | Done when |
|---|---|---|
| **R1** | Game loop in the terminal. Show question → show 4 choices → read input → check answer → next question. No GUI, no scoring yet. | You can play 15 fake questions with `System.out.println` |
| **R2** | `MCQ/` folder, pipe-delimited format, loader that reads every file into `Question` objects. Basic "pick 15 at random" — the fancy rules come next week. | Loader prints all questions from all files without crashing |
| **R3** | `MainWindow` with CardLayout. Main menu, tutorial screen, options screen. Buttons navigate between them. | You can click Menu → Tutorial → back → Options → back |
| **R4** | `AudioManager`: play a looping `.wav` background track, play one SFX. Expose volume methods. | Music loops, a sound plays on demand |

**The one handoff this week:** R4 finishes `AudioManager` first, gives it to R3, R3 wires the two sliders on the Options screen to it. Agree on this by Thursday so R3 isn't blocked.

**Everyone:** start writing questions. Target 25% of your quota done by Sunday.

**Two things for R2 to think through, not code blindly:**
1. When you shuffle the four choices, how do you make sure you still know which one was correct? What's stored in `Choice.choiceId` — the original letter, or the new display position? Only one of those survives a shuffle.
2. What should the loader do if a line has 9 fields instead of 10? Crash loudly with the filename and line number, or skip silently? One of those saves you three hours in Week 4.

---

### Week 2 · Sept 15–21 — Rules and the gameplay screen

| | Task |
|---|---|
| **R1** | Both token ladders as constants. Checkpoint logic (Standard: Q5+Q10, High Risk: Q5). Protocol selection. Three end conditions. Capability unlock events at stage boundaries. |
| **R2** | Real selector: random modules, equal count per module, Bloom distribution 3/2/3/2/3/2, different every run. Independent choice shuffle. **Plus `BankValidator`** — a terminal tool printing questions-per-module-per-Bloom-level. |
| **R3** | Splash screen. Protocol Select screen. `Theme.java` (all colors, fonts, sizes in one place). **Fix the window scaling** — `setResizable(false)` is the safe answer for now. |
| **R4** | Question screen UI: 15-segment progress ladder, 3 lifeline buttons (non-functional), 4 clickable answer rows, green/red result banner, AI face monitor, token counter. |

**Sunday milestone:** half the question bank written and passing `BankValidator`.

`BankValidator` is the highest-value thing on this week's list. It's what stops you finding out on October 13 that week06 has zero Synthesis questions.

---

### Week 3 · Sept 22–28 — INTEGRATION WEEK ⚠️

**This is the week the project succeeds or fails.** Treat it differently from the others.

Goal: a real question, loaded from a real CSV, displayed on the real screen, answered with a real mouse click, scored by the real engine.

| | Task |
|---|---|
| **R1** | Leads integration. Wires `QuestionSource` → `GameEngine` → GUI. Resolves every "your thing doesn't match my thing" argument. **Also: build one throwaway `.exe` with Launch4j this week.** |
| **R2** | Supports R1. Fixes whatever the loader does wrong under real conditions. |
| **R3** | Connects screens to the engine. Menu → Protocol Select → actual game. |
| **R4** | Question screen consumes real `Question` objects. Starts lifeline logic once integration is stable. |

**Sunday Sept 27 milestone — hard gate:**
> A player can launch the game, pick Standard, answer 15 real questions from the CSV files, get scored correctly, and reach a game-over screen. No lifelines. No endings. No polish.

**If you miss this gate, cut scope immediately.** First things to go: secret ending, High Risk timer, extra audio. Never cut: randomization, the `.exe`, `ReadMeFirst.txt`.

Meet in person or on call for at least two long sessions this week. Integration over chat is painful.

---

### Week 4 · Sept 29–Oct 5 — Features

| | Task |
|---|---|
| **R1** | High Risk timer. 2× multiplier. `SessionResult` writing to records. Highest-capability calculation. |
| **R2** | `RecordsStore` read/write. Question bank finished and validated. |
| **R3** | Records screen. Final tutorial content. Polish pass on every screen against the wireframes. |
| **R4** | All 3 lifelines fully working. Win sequence (capabilities flipping ONLINE one by one). Fail sequence. Secret ending. |

**Sunday Oct 4 milestone:** full question bank in, `BankValidator` reports zero gaps, all features present.

**Lifeline tuning note for R4:** Predict should be right roughly 70% of the time, and its stated confidence should loosely track whether it's actually right. If confidence is random, players learn to ignore the number and the lifeline stops being interesting.

---

### Week 5 · Oct 6–12 — Freeze and test

**Monday Oct 5: FEATURE FREEZE.** No new features after this point. Bug fixes only. If someone says "wouldn't it be cool if" — write it down, don't build it.

**Cross-testing — you test someone else's work, never your own:**

| Tester | Tests | Hammer this |
|---|---|---|
| R1 | R2's randomization | 20 sessions back to back. Repeats? Wrong Bloom counts? Uneven modules? |
| R2 | R3's screens | Every button, every path, double-clicks, rapid clicking, different screen sizes |
| R3 | R4's lifelines + audio | Every lifeline in every order, all sound cues, both endings, High Risk secret |
| R4 | R1's scoring | Fail on purpose at Q4, Q6, Q11. Check checkpoint fallback. Check the 2× multiplier. Test **Return**. |

Also this week:
- R1: final `.exe`, `ReadMeFirst.txt`, and a **clean-machine test** — a laptop that has never had the project on it
- Everyone: write your own section of the report

---

### Final stretch · Oct 13–15

| Day | Action |
|---|---|
| **Tue Oct 13** | Final build. All four of you play one complete game each. |
| **Wed Oct 14** | **Submit.** Both zip files, correct subject line, all member names in the body. |
| **Thu Oct 15** | Actual deadline. This is your buffer, not your plan. |

Submitting Wednesday instead of Thursday midnight costs you nothing and saves you if something breaks.

---

## 4. Rules of engagement

**Git**
- `main` always runs. Never push broken code to it.
- One branch each: `engine/`, `data/`, `screens/`, `gameplay/`
- Merge to `main` on the Friday call, with R1 present

**Meetings**
- **Monday, 15 min** — what you're doing this week, what's blocking you
- **Friday, 45 min** — merge, build together, demo

**The 2-hour rule:** stuck for more than 2 hours? Post it in the group chat. Sitting on a blocker silently for three days is the number one thing that kills group projects.

**Definition of Done:** compiles + runs + a teammate has seen it work + merged to `main`.

---

## 5. Risk table

| Risk | Watch for it | Fix |
|---|---|---|
| Commas break the CSV parser | Week 1 | Pipe delimiter, decided today |
| Integration fails Week 3 | Sept 27 gate | Shared classes locked in Week 1 |
| Question bank unfinished | Sept 21 / Oct 4 | Half by W2, full by W4. `BankValidator` catches gaps. |
| Launch4j `.exe` doesn't work | Week 3 | Build a throwaway one in Week 3, not October 13 |
| Someone falls behind | Every Monday | Standup surfaces it in Week 1, not Week 5 |
| Scope creep | Week 4 | Feature freeze Oct 5, no exceptions |
| Audio doesn't play on grader's machine | Week 5 | `.wav` only, via `javax.sound.sampled`. No `.mp3`, no external libraries. |
