# Technical Documentation — Bash Game (Pierre Papier Ciseaux)

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [File-by-File Reference](#3-file-by-file-reference)
4. [Request Lifecycle](#4-request-lifecycle)
5. [HTTP Routes](#5-http-routes)
6. [Data Flow & IPC](#6-data-flow--ipc)
7. [Cross-Platform Handling](#7-cross-platform-handling)
8. [UTF-8 & Emoji Support](#8-utf-8--emoji-support)
9. [Threading Model](#9-threading-model)
10. [Error Handling](#10-error-handling)
11. [Known Limitations](#11-known-limitations)
12. [Glossary](#12-glossary)

---

## 1. Project Overview

**Pierre Papier Ciseaux** is a fully functional browser game built on a deliberately minimal custom stack. Rather than using a traditional web framework, the project wires together three layers:

- A **Bash script** that acts as the pure game engine.
- A **Java HTTP server** written from scratch using raw `ServerSocket` that bridges the browser to the Bash script.
- A **single-page HTML/CSS/JS frontend** that communicates with the server using the Fetch API.

The primary educational goals of the project are to demonstrate:
- How HTTP works at a low level (raw socket programming).
- How a parent process (Java) can spawn and communicate with a child process (Bash).
- How shell scripts can be integrated into a web application pipeline.
- Multi-threading in Java with a thread pool.

---

## 2. Architecture

```
Browser
  index.html + style.css (static)
  JavaScript: fetch('/play', { method: 'POST', ... })
        |
        | HTTP (TCP port 8080)
        v
  Java HTTP Server
  Server.java — raw ServerSocket on :8080
  Thread Pool — up to 10 concurrent connections

  Route /         -> serves index.html (static file)
  Route /style.css -> serves style.css (static file)
  Route /play     -> spawns Play.sh subprocess
  Route (other)   -> 404 Not Found
        |
        | stdin / stdout (pipe)
        v
  Play.sh (Bash)
  Reads player choice from stdin
  Normalises input (case, emoji, shorthand)
  Generates random computer choice ($RANDOM)
  Evaluates game rules
  Prints JSON to stdout
```

---

## 3. File-by-File Reference

### 3.1 `Launcher_Serv.sh`

**Location:** `Script_Bash/Launcher_Serv.sh`
**Purpose:** Bootstrap script. Compiles the Java sources and starts the server.

**Step-by-step breakdown:**

```bash
cd "$(dirname "$0")/.." || exit 1
```
Navigates to the project root (the parent of `Script_Bash/`), regardless of where the script is called from. The `|| exit 1` ensures failure is not silently ignored.

```bash
chmod +x Script_Bash/Play.sh
```
Makes `Play.sh` executable on Linux. This is necessary because on some systems, files extracted from a zip archive lose their executable bit.

```bash
javac -cp Src Src/server/Server.java Src/main.java
```
Compiles both Java source files. The `-cp Src` flag tells the compiler that the `Src/` directory is the root of the classpath, so the `server` package (in `Src/server/`) is correctly resolved. The `.class` output files are placed alongside their `.java` sources.

```bash
java -cp Src main
```
Runs the `main` class with `Src/` as the classpath root.

---

### 3.2 `Play.sh`

**Location:** `Script_Bash/Play.sh`
**Purpose:** Pure game logic. Reads a player choice from stdin, computes the result, and prints JSON.

**Input:**

The script reads exactly one line from stdin (piped from the Java server):

```bash
read playerChoice
```

**Normalisation:**

A `case` statement maps all accepted input variants to three canonical strings: `rock`, `paper`, or `scissors`. Any unrecognised input maps to `invalid`.

```bash
case "$playerChoice" in
    rock|Rock|ROCK|r|R|rock_emoji) player="rock" ;;
    paper|Paper|PAPER|p|P|paper_emoji) player="paper" ;;
    scissors|Scissors|SCISSORS|s|S|scissors_emoji) player="scissors" ;;
    *) player="invalid" ;;
esac
```

Emoji are matched directly in the case patterns. The server ensures the input is encoded as UTF-8 before writing it to the process stdin.

**Computer Choice Generation:**

```bash
options=("rock" "paper" "scissors")
computer=${options[$RANDOM % 3]}
```

`$RANDOM` is a Bash built-in that returns a pseudo-random integer between 0 and 32767 on each access. Taking `% 3` yields a uniform index of 0, 1, or 2.

**Rule Evaluation:**

```bash
if [[ "$player" == "invalid" ]]; then
    result="invalid"
elif [[ "$player" == "$computer" ]]; then
    result="tie"
elif [[ "$player" == "rock" && "$computer" == "scissors" ]] ||
     [[ "$player" == "paper" && "$computer" == "rock" ]] ||
     [[ "$player" == "scissors" && "$computer" == "paper" ]]; then
    result="win"
else
    result="lose"
fi
```

All three winning conditions are encoded explicitly. Any other combination that is not a tie or invalid is a loss, handled by the final `else`.

**Output:**

```bash
echo "{\"result\":\"$result\", \"computerChoice\":\"$computer\"}"
```

The script prints a single line of JSON to stdout.

Example outputs:
```json
{"result":"win", "computerChoice":"scissors"}
{"result":"lose", "computerChoice":"paper"}
{"result":"tie", "computerChoice":"rock"}
{"result":"invalid", "computerChoice":"paper"}
```

---

### 3.3 `main.java`

**Location:** `Src/main.java`
**Purpose:** Java entry point. Acts as a thin wrapper that delegates to `Server.main()`.

```java
import server.Server;

public class main {
    public static void main(String[] args) {
        Server.main(args);
    }
}
```

This class exists purely to satisfy the Java requirement that the class containing `public static void main` must be in the default package when run as `java -cp Src main`. The actual logic lives in `server.Server`.

---

### 3.4 `Server.java`

**Location:** `Src/server/Server.java`
**Package:** `server`
**Purpose:** Raw HTTP/1.1 server. Handles all routing, static file serving, and subprocess invocation.

**Class-level Thread Pool:**

```java
private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
```

A single shared thread pool of 10 threads handles all incoming connections. This allows up to 10 simultaneous clients without creating a new thread per connection.

**`main()` — Server Startup:**

```java
try (ServerSocket serverSocket = new ServerSocket(8080)) {
    while (true) {
        Socket client = serverSocket.accept();
        threadPool.submit(() -> handleClient(client));
    }
}
```

The server binds to port 8080 and enters an infinite accept loop. Each accepted connection is immediately handed off to the thread pool, so the main thread is always free to accept new connections.

**`getProcessBuilder()` — Subprocess Configuration:**

```java
boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
File script = new File(System.getProperty("user.dir"), "Script_Bash/Play.sh");

if (isWindows) {
    return new ProcessBuilder("bash", script.getAbsolutePath());
} else {
    script.setExecutable(true);
    return new ProcessBuilder(script.getAbsolutePath());
}
```

On Windows, `bash` must be invoked explicitly since `.sh` files are not directly executable. On Linux/macOS, the script is marked executable at runtime and invoked directly.

**`handleClient()` — Request Handling:**

This method handles one HTTP request per call.

Step 1 — Parse the request line:
```java
String request = in.readLine();      // e.g. "POST /play HTTP/1.1"
String path = request.split(" ")[1]; // extracts "/play"
```

Step 2 — Route `/play` (game execution):
1. Read HTTP headers; capture `Content-Length`.
2. Read exactly `contentLength` characters from the body (player's choice).
3. Spawn `Play.sh` via `ProcessBuilder`.
4. Write the player choice to the subprocess stdin as UTF-8 bytes.
5. Read the subprocess stdout **before** calling `process.waitFor()` (prevents pipe deadlock).
6. Return the JSON response with proper HTTP headers.

Step 3 — Static file serving: Search for a matching file in three locations in order:
1. `Src/template/<path>`
2. `Src/<path>`
3. `./<path>`

If none is found, return a 404.

Content-Type is determined by file extension:

| Extension | Content-Type |
|-----------|-------------|
| `.html` | `text/html; charset=UTF-8` |
| `.css` | `text/css` |
| `.js` | `application/javascript` |
| other | `text/plain` |

---

### 3.5 `index.html`

**Location:** `Src/template/index.html`
**Purpose:** The game's single-page user interface.

**UI Structure:**

| Element | Role |
|---------|------|
| `.bandeau` | Title banner |
| `.buttons` | Three clickable emoji buttons |
| `.rules` | Displays the game rules |
| `.fight` | Shows the player icon vs. computer icon |
| `.result` | Displays the outcome message |

**JavaScript — `play()` function:**

```javascript
async function play(playerChoice) {
    document.getElementById('player-icon').textContent = playerChoice;
    document.getElementById('computer-icon').textContent = 'loading';
    document.getElementById('result').textContent = '';

    const response = await fetch('/play', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain; charset=UTF-8' },
        body: playerChoice
    });

    const data = await response.json();
    // update DOM with result
}
```

- The player's chosen emoji is shown immediately for a responsive feel.
- A loading spinner replaces the computer's icon while waiting for the server.
- The `choiceEmoji` map converts the server's text response (`"rock"`, `"paper"`, `"scissors"`) back to the corresponding emoji for display.
- Both network errors and invalid JSON responses are caught separately and shown as user-friendly messages.

---

### 3.6 `style.css`

**Location:** `Src/style.css`
**Purpose:** Visual styling for the game interface.

Key design decisions:
- Black-and-white banner (`.bandeau`) with large white title text.
- Olive-green buttons (`background-color: rgb(134, 161, 12)`) with large emoji font size (36px).
- Centred fight display (`.fight`) using flexbox with `gap`.
- Responsive media query at 600px for small screens, reducing the title font size.
- A `.footer` rule is defined in CSS but not currently used in the HTML — a prepared placeholder for future use.

---

## 4. Request Lifecycle

Complete flow for a single game round:

```
1. User clicks the Rock button in the browser.

2. JavaScript calls play('rock-emoji').
   - Sets player-icon to the rock emoji.
   - Sets computer-icon to the hourglass emoji.

3. Browser sends:
   POST /play HTTP/1.1
   Content-Type: text/plain; charset=UTF-8
   Content-Length: 4

   (rock emoji as UTF-8 bytes)

4. Java Server (on a thread pool thread):
   - Reads "POST /play HTTP/1.1".
   - Reads headers until blank line; captures Content-Length = 4.
   - Reads 4 chars from the body.
   - Calls getProcessBuilder() to set up Play.sh invocation.
   - Starts the process.
   - Writes the emoji (UTF-8 bytes) to the process stdin.
   - Reads one line from process stdout.
   - Calls process.waitFor().

5. Play.sh:
   - read playerChoice -> "rock-emoji"
   - case match: player="rock"
   - computer=${options[$RANDOM % 3]} -> e.g. "scissors"
   - Evaluates: rock beats scissors -> result="win"
   - Prints: {"result":"win", "computerChoice":"scissors"}

6. Java Server reads JSON, sends HTTP 200 response with JSON body.

7. Browser receives JSON:
   - computerChoice = "scissors" -> scissors emoji
   - Sets computer-icon to the scissors emoji.
   - Sets result text to: "You win!"
```

---

## 5. HTTP Routes

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| GET | `/` | Static | Serves `index.html` |
| GET | `/style.css` | Static | Serves `style.css` |
| POST | `/play` | Dynamic | Runs `Play.sh`, returns JSON |
| GET | `*` | Static | Tries to serve matching file; 404 if not found |

Note: The server does not validate the HTTP method. Only the `/play` path triggers subprocess execution.

---

## 6. Data Flow & IPC

The communication between Java and Bash uses standard Inter-Process Communication (IPC) via pipes:

```
Java                                 Bash (Play.sh)
ProcessBuilder.start()       ->      (process spawned)
os.write(choice.getBytes())  ->      stdin -> read playerChoice
                             <-      stdout <- echo JSON
scriptOut.readLine()         <-      (Java reads the JSON line)
process.waitFor()                    (process exits cleanly)
```

Key implementation details:
- **stdin is closed immediately after writing** (using try-with-resources). This sends EOF to the script, preventing `read` from hanging indefinitely.
- **stdout is read before `waitFor()`** to prevent a pipe deadlock: if the script tries to write more data than the OS pipe buffer can hold, it blocks. If Java is also blocking in `waitFor()`, neither side proceeds — a classic deadlock.
- `redirectErrorStream(true)` merges the script's stderr into its stdout, so any error messages from Bash are captured rather than silently lost.

---

## 7. Cross-Platform Handling

| Platform | Bash availability | Server behaviour |
|----------|-------------------|-----------------|
| Linux / macOS | Native | `setExecutable(true)` then direct invocation |
| Windows (WSL) | Via WSL | `new ProcessBuilder("bash", script.getAbsolutePath())` |
| Windows (Git Bash) | Via Git Bash | Same as WSL path |

Detection mechanism:
```java
boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
```

---

## 8. UTF-8 & Emoji Support

Emoji handling required explicit UTF-8 enforcement at every data boundary:

| Boundary | Mechanism |
|----------|-----------|
| Browser sends to server | `Content-Type: text/plain; charset=UTF-8` header |
| Server reads from socket | `InputStreamReader(..., StandardCharsets.UTF_8)` |
| Server writes to Bash stdin | `playerChoice.getBytes(StandardCharsets.UTF_8)` |
| Server reads from Bash stdout | `InputStreamReader(..., StandardCharsets.UTF_8)` |
| Server sends response | `Content-Type: application/json; charset=UTF-8`, byte-length calculated from UTF-8 bytes |

Without explicit UTF-8 enforcement, the JVM's platform default encoding (which may be ISO-8859-1 on some systems) would corrupt multi-byte emoji characters. Rock (U+1F5FF) encodes to 4 bytes in UTF-8 — a single incorrect byte would break pattern matching in the Bash `case` statement.

---

## 9. Threading Model

```
Main Thread
    ServerSocket.accept() loop (blocks indefinitely)
        |
        +-- Connection 1 -> Thread Pool Thread 1 -> handleClient()
        +-- Connection 2 -> Thread Pool Thread 2 -> handleClient()
        +-- ...
        +-- Connection 10 -> Thread Pool Thread 10 -> handleClient()
                                   (max 10 concurrent)
```

The `ExecutorService` with a fixed pool of 10 threads means:
- Up to 10 HTTP requests can be handled simultaneously.
- If all 10 threads are busy, additional connections queue in the OS socket backlog.
- Threads are reused, avoiding the overhead of creating a new thread per request.

---

## 10. Error Handling

| Scenario | Handling |
|----------|---------|
| `Play.sh` produces no output | Java falls back to `{"result":"error", "computerChoice":"unknown"}` |
| Invalid player input | `Play.sh` returns `result: "invalid"`; browser shows "Invalid choice." |
| Network error in browser | Caught by `try/catch` around `fetch()`; displays "Error: could not reach the server." |
| Non-200 HTTP response | Caught via `response.ok` check |
| Invalid JSON from server | Caught by second `try/catch` around `response.json()`; displays "Error: invalid server response." |
| Compilation failure | `Launcher_Serv.sh` checks `$?` after `javac`; prints message and exits with code 1 |
| File not found (static) | Returns `HTTP/1.1 404 Not Found` with plain-text body |
| IOException / InterruptedException | Caught in `handleClient()` and printed to server stderr via `e.printStackTrace()` |

**Pipe deadlock prevention (critical detail):** The subprocess stdout is always read *before* `process.waitFor()` is called. This prevents the scenario where: (1) the Bash script fills the pipe buffer with output and blocks; (2) Java calls `waitFor()` which blocks until the process exits; (3) neither side can proceed.

---

## 11. Known Limitations

- **No HTTPS:** All communication is plain HTTP. Suitable for localhost development only.
- **No score persistence:** There is no session tracking. Each round is stateless and scores reset on page reload.
- **No HTTP method validation:** The server does not return 405 for wrong methods.
- **Fixed thread pool:** Under very high load (>10 simultaneous connections), additional requests queue in the OS socket backlog.
- **Bash dependency:** Will not run on environments where Bash is unavailable (e.g., plain Windows without WSL).
- **Footer placeholder:** The `.footer` CSS class is defined but not wired to any HTML element.

---

## 12. Glossary

| Term | Definition |
|------|-----------|
| **ServerSocket** | A Java class that listens on a TCP port and accepts incoming connections. |
| **ProcessBuilder** | A Java class used to create and configure operating-system processes. |
| **Thread Pool** | A group of pre-created threads that pick up tasks from a queue, avoiding the cost of creating a new thread per request. |
| **IPC** | Inter-Process Communication — mechanisms for processes to exchange data. Here: stdin/stdout pipes. |
| **stdin / stdout** | Standard input and standard output — the default data channels for a process. |
| **UTF-8** | A variable-width character encoding that can represent all Unicode characters, including emoji. |
| **Classpath (-cp)** | A parameter telling the Java compiler and runtime where to find class files and packages. |
| **EOF** | End-of-File — a signal that there is no more data to read. Closing a pipe sends EOF to the reading process. |
| **Pipe buffer** | A fixed-size OS buffer holding data written by one process until another process reads it. |
| **$RANDOM** | A Bash built-in variable that returns a new pseudo-random integer (0–32767) each time it is referenced. |
| **Fetch API** | A modern browser API for making HTTP requests from JavaScript. Returns Promises. |
