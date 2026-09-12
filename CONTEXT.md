# CONTEXT — *How to Train Your AI*

Single source of truth for what this project is and what it must satisfy.
Synthesized from `CMSC 170 MP.pdf` (our game proposal), `CMSC 170 Assignment
Guide_1.pdf` (the grading requirements), and `How_To_Train_Your_AI_Plan_v2.md`
(the team's execution plan). Read this before making a design decision that
touches scoring, data format, or submission — it's the tie-breaker.

- **Course:** CMSC 170 – Introduction to AI, UP Tacloban, 1st Sem AY 2026–2027
- **Team:** Nathan Planas, Geoffrey Tomagan (R1), Mark Asher Abesia, Adrian Mayores
- **Deadline:** Thursday, October 15, 2026, midnight (submit Wed Oct 14 as buffer)
- **Stack:** Java + Swing, plain `javac`/`java` (no build tool), CSV data, no database

---

## 1. What we're building

*How to Train Your AI* — a *Who Wants to Be a Millionaire?*-style quiz game,
required by Assignment Guide #1 as "the approved WWTBAM game variant with
AI-themed topics." Instead of money, the player restores a corrupted AI's six
cognitive capabilities by answering 15 AI-themed multiple-choice questions in
increasing Bloom's Taxonomy order.

**Framing:** the player is a technician restoring an AI whose core survived a
catastrophe but whose cognitive systems were corrupted. All six capabilities
start offline/corrupted; each Bloom stage completed brings one online. Winning
(Q15 correct) triggers "AI CORE: FULLY ACTIVATED" and a story revelation about
what really happened in the catastrophe.

## 2. Core mechanics (from `CMSC 170 MP.pdf`)

### 2.1 Six capabilities ↔ Bloom stages ↔ questions
| Questions | Bloom level | Capability unlocked |
|---|---|---|
| 1–3 | Remember | Memory |
| 4–5 | Understand | Understanding |
| 6–8 | Apply | Application |
| 9–10 | Analyze | Analysis |
| 11–13 | Evaluate | Evaluation |
| 14–15 | Synthesis | Synthesize |

All 15 questions are randomly selected from `MCQ/`; choice order is
independently randomized per question, per Assignment Guide #1 items 1b–1d.

### 2.2 Protocols
| | Standard | High Risk |
|---|---|---|
| Lifelines | 3 (all) | 2 (Binary Choice, Predict — **no Override**) |
| Checkpoints | Q5 and Q10 | Q5 only |
| Scoring | normal ladder | normal ladder × 2 |
| Timer | none | yes |
| Special | — | Secret Ending / Corrupt AI on win |

Both protocols share the same 15 questions and 6 stages — only assistance and
failure consequences differ. Checkpoints **secure** the token score and
capability progress reached so far; a later failure rolls back to the most
recent secured checkpoint, not to zero. Verified capabilities stay verified
even on rollback — only progress *after* the last checkpoint is lost.

### 2.3 Token ladders (exact values — treat as constants, not re-derived)

**Standard** (`ScoreLadder.STANDARD`):
```
Q1:10  Q2:15  Q3:25  Q4:35  Q5:50   Q6:70   Q7:95   Q8:125
Q9:165 Q10:215 Q11:275 Q12:350 Q13:425 Q14:475 Q15:500
```

**High Risk**, already includes the 2× multiplier (`ScoreLadder.HIGH_RISK`):
```
Q1:20  Q2:30  Q3:50  Q4:70  Q5:100  Q6:140  Q7:190  Q8:250
Q9:330 Q10:430 Q11:550 Q12:700 Q13:850 Q14:950 Q15:1000
```

### 2.4 Lifelines (once each per game)
- **Binary Choice** — removes 2 incorrect choices, leaving 2.
- **Predict** — AI gives a suggested answer + confidence level; *not
  guaranteed correct*. Tuning target (R4, Week 4): right ~70% of the time,
  confidence should loosely track correctness so players learn to read it.
- **Override** — if you picked wrong, retry with another option once. Standard
  protocol only.

### 2.5 End conditions
1. Wrong answer with no lifeline left → session ends, score finalizes at last
   secured checkpoint.
2. All 15 correct → win sequence, all capabilities ONLINE, story revelation.
3. Player chooses **Return** → ends session voluntarily, no penalty, current
   score finalizes as-is.

At session end, record: **Trainer Name, Final Token Score, Highest Capability
Restored, Restoration Protocol, Game Result** → Training Records.

### 2.6 GUI screens (wireframed in MP.pdf, owned per `How_To_Train_Your_AI_Plan_v2.md` §1)
Main Menu (Start/Tutorial/Options/Exit) → Settings (Music/SFX sliders) →
Instructions ("How to Play") → Question screen (progress ladder of 15
segments, 3 lifeline buttons top-right, question + 4 choices, green/red result
banner, small AI "face" monitor reacting to state) → Game Over ("RESTORATION
FAILED", capability list with last-good state, Retry/Back to Menu) → Win
("AI CORE: FULLY ACTIVATED", all six ONLINE, Retry/Back to Menu).

Controls are mouse-only end to end (Assignment Guide #1 item 9) — no keyboard
requirement anywhere in the design.

---

## 3. Data format — **DECIDED: CSV, pipe-delimited**

Per `How_To_Train_Your_AI_Plan_v2.md` §2a, locked before any real code:

- Delimiter is **`|`**, not comma — questions contain commas, and comma-CSV
  needs quote-escaping logic no one wants to write in Week 1.
- Directory: **`MCQ/`** (required literally by Assignment Guide #1 item 1a).
- Proposed layout: `MCQ/week03.csv`, one file per week/module.
- Header row: `id|module|bloom|question|choiceA|choiceB|choiceC|choiceD|correct|explanation`
- Rules: one question per line, no `|` in any field, `correct` is a single
  letter A–D, `bloom` is exactly one of `Remember/Understand/Apply/Analyze/
  Evaluate/Synthesis`, first line is header and is skipped by the loader.

This supersedes the two pre-existing draft formats in the repo (`MCQ/week01.json`
structured JSON, and `CMSC 170 Question Bank - Question Bank.csv` which is
comma-delimited with quote-escaping). Both predate this decision; question
content in them still needs porting into the pipe-delimited `MCQ/*.csv` files —
it is not thrown away, just reformatted.

**Open items for R2** (data owner) per the team plan: what `Choice.choiceId`
stores across a shuffle (original letter, not display position), and whether
the loader crashes loudly or skips silently on a malformed row (9 fields
instead of 10). Both must be settled before Week 2's real selector logic.

---

## 4. Assignment requirements checklist (`CMSC 170 Assignment Guide_1.pdf`)

Non-negotiable, independent of the game design above:

- [ ] All questions inside `MCQ/`, organized for easy checking at submission
- [ ] Questions randomly generated every run; choice order dynamic every run
- [ ] Categories/modules randomly selected, **equal question count per module**
- [ ] Prize-ladder scoring (satisfied by §2.3 above)
- [ ] Mouse-only controls
- [ ] Instructions/how-to-play screen included
- [ ] Sounds + background music (`.wav` only via `javax.sound.sampled` — no
  `.mp3`, no external audio libs, per the team plan's risk table — grader
  machines may lack MP3 codecs)
- [ ] GUI themed around AI
- [ ] Splash screen with a **new title** (i.e., not literally "Who Wants to Be
  a Millionaire?" — "How to Train Your AI" satisfies this)
- [ ] Standalone `.exe` via Launch4j/JSmooth (jar → exe)
- [ ] **Command-prompt validation**: compile and run using only `javac`/`java`,
  confirming all source files + dependencies are inside the submitted source
  directory — already verified working as of 2026-09-12 (see project notes;
  zero external deps currently, so no classpath complications yet)
- [ ] **`ReadMeFirst.txt`** — step-by-step CLI compile/run guide, "so your code
  can be processed using any available IDE" — this is R1's Week 5 deliverable
- [ ] Two submission zips: source code, and standalone executable

**Grading rubric** (0/5/7/10): full marks require correct output for *all*
valid inputs per spec, a design that's "excellent, polished, and visually
engaging" — not just functional. A buggy or incomplete submission is
**returned to the group to fix**, not silently penalized — so shipping
something that runs cleanly matters more than shipping every stretch feature.

**Submission mechanics:** email `submit2jpyusiong@gmail.com`, subject
`Submission [CMSC 170]: Assignment 1 (Surname, First Name)` using the group
leader's name, body lists all members. Deadline Thu of Week 10 (Oct 15,
2026) midnight — team plan targets submitting Wed Oct 14 as a buffer.

---

## 5. Timeline anchor (see `How_To_Train_Your_AI_Plan_v2.md` for full detail)

| Week | Dates | Gate |
|---|---|---|
| 1 | Sep 8–14 | Shared classes + fake game loop/loader/screens/audio, independently |
| 2 | Sep 15–21 | Real rules/selector/screens/question-screen UI; half the bank done |
| 3 | Sep 22–28 | **Integration week — hard gate Sun Sep 27**: real CSV → engine → GUI, scored, game-over reached |
| 4 | Sep 29–Oct 5 | Features (timer, multiplier, records, lifelines, endings); full bank by Sun Oct 4 |
| 5 | Oct 6–12 | **Freeze Oct 5**; cross-testing; final `.exe` + `ReadMeFirst.txt` |
| Final | Oct 13–15 | Final build Tue, submit Wed, buffer Thu |

Role ownership (R1 = Geoffrey/this user): see
`How_To_Train_Your_AI_Plan_v2.md` §1, or `R1_Engine_Weekly_Plan.md` for the
day-by-day R1-specific breakdown already written.
