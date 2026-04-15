# 🗿📄✂️ Pierre Papier Ciseaux — Bash Game

A browser-based **Rock Paper Scissors** game powered by a custom Java HTTP server and a Bash script for game logic.

---

## Overview

This project combines three technologies in a simple but creative architecture:

- **Bash** — handles all game logic (randomisation, rule evaluation, JSON output)
- **Java** — acts as a lightweight multi-threaded HTTP server
- **HTML/CSS/JS** — provides the browser interface

When a player clicks a button in the browser, the choice is sent to the Java server via an HTTP POST request. The server pipes that input into a Bash script, which computes the result and returns JSON. The browser then displays the outcome.

---

## Project Structure

```
Bash_game/
├── Script_Bash/
│   ├── Play.sh              # Game logic (Bash)
│   └── Launcher_Serv.sh     # Compilation & server startup script
└── Src/
    ├── main.java            # Java entry point
    ├── main.class           # Compiled entry point
    ├── style.css            # Frontend styles
    ├── server/
    │   ├── Server.java      # HTTP server implementation
    │   └── Server.class     # Compiled server
    └── template/
        └── index.html       # Game UI
```

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java (JDK)  | 8 or higher |
| Bash        | Any modern version (Linux/macOS/WSL on Windows) |

> **Windows users:** Bash must be available via WSL or Git Bash.

---

## Getting Started

### 1. Clone or extract the project

```bash
unzip Bash_game.zip
cd Bash_game
```

### 2. Launch the server

```bash
bash Script_Bash/Launcher_Serv.sh
```

This script will:
1. Compile `Server.java` and `main.java`
2. Make `Play.sh` executable
3. Start the Java server on port **8080**

### 3. Play the game

Open your browser and navigate to:

```
http://localhost:8080
```

Click 🗿, 📄, or ✂️ to play against the computer.

---

## How It Works

```
Browser  →  POST /play  →  Java Server  →  Play.sh (Bash)  →  JSON response  →  Browser
```

1. The player clicks a button in the browser (emoji sent as plain text).
2. The browser sends an HTTP POST to `/play`.
3. The Java server receives the request, reads the player's choice from the body, and spawns `Play.sh` as a subprocess, writing the choice to its stdin.
4. `Play.sh` normalises the input, generates a random computer choice, evaluates the game rules, and prints a JSON object.
5. The Java server reads the JSON from the script's stdout and returns it as the HTTP response.
6. The browser parses the JSON and updates the display with the result.

---

## Rules

| Player | Computer | Result |
|--------|----------|--------|
| 🗿 Rock | ✂️ Scissors | Win |
| ✂️ Scissors | 📄 Paper | Win |
| 📄 Paper | 🗿 Rock | Win |
| Any | Same | Tie |
| Any other | Any | Lose |

---

## Accepted Input Formats

`Play.sh` accepts many formats for each choice:

| Choice   | Accepted inputs                              |
|----------|----------------------------------------------|
| Rock     | `rock`, `Rock`, `ROCK`, `r`, `R`, `🗿`       |
| Paper    | `paper`, `Paper`, `PAPER`, `p`, `P`, `📄`    |
| Scissors | `scissors`, `Scissors`, `SCISSORS`, `s`, `S`, `✂️` |

---

## License

This project was created for educational purposes.


for run code :
javac Src/server/Server.java Src/main.java
java -cp Src main