How to Train Your AI

Requirements
- JDK 17 or newer
- Run commands from the project root

Compile and run on Windows (PowerShell)
1. Open PowerShell in the project root.
2. Run:
   New-Item -ItemType Directory -Force bin | Out-Null
   $sources = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
   javac -encoding UTF-8 -cp "lib\freetts\*" -d bin $sources
   Copy-Item -Recurse -Force src/main/resources/* bin/
   java -cp "bin;lib\freetts\*" com.howtotrainyourai.core.Main

Compile and run on macOS or Linux
1. Open a terminal in the project root.
2. Run:
   mkdir -p bin
   javac -encoding UTF-8 -cp 'lib/freetts/*' -d bin $(find src/main/java -name '*.java')
   cp -R src/main/resources/. bin/
   java -cp 'bin:lib/freetts/*' com.howtotrainyourai.core.Main

The question bank and screen images are loaded from the classpath, so copy the
contents of src/main/resources into the compiled output directory before
running or packaging the application.

Offline speech uses FreeTTS 1.2.2 and its `kevin16` voice. The five required
jars are in `lib/freetts/` (7,468,959 bytes total); keep that directory beside the app
when packaging. It includes the FreeTTS license terms. The app makes no runtime
downloads and leaves text questions usable if audio initialization fails.

For a runnable jar/distribution, compile and copy resources as above, then from
the project root:

    New-Item -ItemType Directory -Force dist/lib/freetts | Out-Null
    Copy-Item lib/freetts/* dist/lib/freetts/
    jar --create --file dist/HowToTrainYourAI.jar --manifest launch4j.mf -C bin .

On macOS/Linux, use `mkdir -p dist/lib/freetts`, then run `cp lib/freetts/* dist/lib/freetts/`
and the same `jar` command. Run the packaged jar offline
with `java -jar dist/HowToTrainYourAI.jar`. For Launch4j, wrap that jar and
ship the entire `dist/lib/freetts/` directory beside the executable. Training
records are stored per user: `%APPDATA%\HowToTrainYourAI` on Windows,
`~/.local/share/HowToTrainYourAI` on Linux (or `$XDG_DATA_HOME`), and
`~/Library/Application Support/HowToTrainYourAI` on macOS.
