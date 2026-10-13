# How to Train Your AI

A *Who Wants to Be a Millionaire?*-style quiz game about Artificial Intelligence, built in Java Swing for CMSC 170 (Introduction to Artificial Intelligence), UP Tacloban College, First Semester AY 2026-2027.

You play a technician restoring an AI whose core survived a catastrophe but whose six cognitive capabilities (Memory, Understanding, Application, Analysis, Evaluation, Synthesis) were corrupted. Answer 15 multiple-choice questions, arranged by Bloom's Taxonomy, to bring the AI back online.

**Team:** Nathan Gen F. Planas, Geoffrey Adam A. Tomagan, Mark Asher G. Abesia, Adrian Raphaello D. Mayores
**Professor:** Sir John Paul Yusiong

## Features

- Mouse-driven menus, tutorial, options (music, SFX and speech volume) and a splash screen.
- 15 questions per session across six Bloom stages (Q1-3 Remember, Q4-5 Understand, Q6-8 Apply, Q9-10 Analyze, Q11-13 Evaluate, Q14-15 Synthesis). Questions and the order of their choices are randomized every session.
- Two restoration protocols:

  | | Standard | High Risk |
  |---|---|---|
  | Lifelines | 3 (Binary Choice, Predict, Override) | 2 (Binary Choice, Predict) |
  | Checkpoints | Q5 and Q10 | Q5 |
  | Timer | none | 30 seconds per question |
  | Token scoring | normal | double |

- Lifelines: **Binary Choice** removes two wrong answers, **Predict** gives a simulated AI suggestion with a confidence level (not always right), and **Override** automatically absorbs your first wrong answer.
- Token prize ladder (10 to 500 tokens on Standard, 20 to 1000 on High Risk). A wrong answer falls back to the last checkpoint score.
- Capabilities light up as each Bloom stage is completed.
- Return option to end a session and keep your current score.
- End screen showing trainer name, final token score, highest capability restored, protocol and game result.
- Training Records saved locally, a top-10 board on the setup screen, and a log viewer in Options.
- Optional offline text-to-speech for questions.

## Requirements

- JDK 17 or newer
- Run all commands from the project root

## Compile and Run

### Windows (PowerShell)

1. Open PowerShell in the project root.
2. Run:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
$sources = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib\freetts\*" -d bin $sources
Copy-Item -Recurse -Force src/main/resources/* bin/
java -cp "bin;lib\freetts\*" com.howtotrainyourai.core.Main
```

### macOS or Linux

1. Open a terminal in the project root.
2. Run:

```sh
mkdir -p bin
javac -encoding UTF-8 -cp 'lib/freetts/*' -d bin $(find src/main/java -name '*.java')
cp -R src/main/resources/. bin/
java -cp 'bin:lib/freetts/*' com.howtotrainyourai.core.Main
```

The question bank and screen images are loaded from the classpath, so copy the contents of `src/main/resources` into the compiled output directory before running or packaging the application.

Offline speech uses FreeTTS 1.2.2 and its `kevin16` voice. The five required jars are in `lib/freetts/` (7,468,959 bytes total); keep that directory beside the app when packaging. It includes the FreeTTS license terms. The app makes no runtime downloads and leaves text questions usable if audio initialization fails.

## Packaging

For a runnable jar/distribution, compile and copy resources as above, then from the project root:

```powershell
New-Item -ItemType Directory -Force dist/lib/freetts | Out-Null
Copy-Item lib/freetts/* dist/lib/freetts/
jar --create --file dist/HowToTrainYourAI.jar --manifest launch4j.mf -C bin .
```

On macOS/Linux, use `mkdir -p dist/lib/freetts`, then run `cp lib/freetts/* dist/lib/freetts/` and the same `jar` command.

Run the packaged jar offline with:

```
java -jar dist/HowToTrainYourAI.jar
```

For Launch4j, wrap that jar and ship the entire `dist/lib/freetts/` directory beside the executable; keep the launcher working directory at the distribution root.

## How to Play

1. Click the menu buttons on the main screen: Play, Tutorial, Options, Exit.
2. On Play, enter your trainer name and choose Standard or High Risk.
3. Click an answer key (A-D) to answer each question. Use the Binary Choice and Predict lifelines before answering if you need help.
4. Correct answers earn tokens and advance you up the ladder; checkpoints secure your score.
5. Pull the switch or press Back to return and end the session at any time.

The in-game Tutorial covers the same rules.

## Training Records

Sessions are appended to `TrainingRecords.csv` inside the `HowToTrainYourAI` folder in your `%APPDATA%` directory (your home directory if `APPDATA` is not set). Columns: `trainer|score|highest capability|protocol|result`.

## Project Structure

```
src/main/java/com/howtotrainyourai/
  core/     Main entry point, engine factory
  engine/   game rules: scoring, protocols, lifelines, records, audio, speech
  data/     question bank loading
  model/    Question and Choice
  gui/      Swing screens (menu, setup, scene, tutorial, settings, ending)
src/main/resources/
  data/     Question_Bank_PDL.csv
  images/   screen artwork
  audio/    sound effects and story audio
lib/freetts/  offline text-to-speech jars
```

## Documentation

- `ReadMeFirst.txt`: plain-text compile and run steps for submission
- `CONTEXT.md`: game rules and domain notes
- `How_To_Train_Your_AI_Plan_v2.md`: project plan
