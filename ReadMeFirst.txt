How to Train Your AI

Requirements
- JDK 17 or newer
- Run commands from the project root

Compile and run on Windows (PowerShell)
1. Open PowerShell in the project root.
2. Run:
   New-Item -ItemType Directory -Force bin | Out-Null
   $sources = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
   javac -encoding UTF-8 -d bin $sources
   Copy-Item -Recurse -Force src/main/resources/* bin/
   java -cp bin com.howtotrainyourai.core.Main

Compile and run on macOS or Linux
1. Open a terminal in the project root.
2. Run:
   mkdir -p bin
   javac -encoding UTF-8 -d bin $(find src/main/java -name '*.java')
   cp -R src/main/resources/. bin/
   java -cp bin com.howtotrainyourai.core.Main

The question bank and screen images are loaded from the classpath, so copy the
contents of src/main/resources into the compiled output directory before
running or packaging the application.
