package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on http://localhost:8080");

            while (true) {
                Socket client = serverSocket.accept();
                threadPool.submit(() -> handleClient(client));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ProcessBuilder getProcessBuilder() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        File script = new File(System.getProperty("user.dir"), "Script_Bash/Play.sh");

        System.out.println("Working dir : " + System.getProperty("user.dir"));
        System.out.println("Script path : " + script.getAbsolutePath());
        System.out.println("Script exists: " + script.exists());

        ProcessBuilder pb;
        if (isWindows) {
            pb = new ProcessBuilder("bash", "Script_Bash/Play.sh");
            pb.directory(new File(System.getProperty("user.dir")));
        } else {
            script.setExecutable(true);
            pb = new ProcessBuilder(script.getAbsolutePath());
        }
        return pb;
    }

    private static void handleClient(Socket client) {
        try {
            InputStream rawIn = client.getInputStream();
            BufferedInputStream in = new BufferedInputStream(rawIn);
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);

            String request = readHttpRequestLine(in);
            System.out.println("Request: " + request);

            if (request == null || request.isEmpty()) {
                client.close();
                return;
            }

            String path = request.split(" ")[1];

            // --- ROUTE /play : run Play.sh ---
            if (path.equals("/play")) {

                int contentLength = 0;
                String line;
                while ((line = readHttpRequestLine(in)) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
                    }
                }

                byte[] bodyBytes = new byte[contentLength];
                if (contentLength > 0) {
                    int read = 0;
                    while (read < contentLength) {
                        int n = in.read(bodyBytes, read, contentLength - read);
                        if (n < 0) break;
                        read += n;
                    }
                }
                String playerChoice = new String(bodyBytes, StandardCharsets.UTF_8).trim();
                System.out.println("Player choice = " + playerChoice);

                ProcessBuilder pb = getProcessBuilder();
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Write player choice to bash stdin with UTF-8 (emoji support)
                try (OutputStream os = process.getOutputStream()) {
                    os.write(playerChoice.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                // Read output BEFORE waitFor() to avoid pipe deadlock
                BufferedReader scriptOut = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String json = scriptOut.readLine();
                process.waitFor();

                if (json == null) {
                    json = "{\"result\":\"error\", \"computerChoice\":\"unknown\"}";
                }

                byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: application/json; charset=UTF-8\r\n");
                out.print("Content-Length: " + jsonBytes.length + "\r\n");
                out.print("\r\n");
                out.flush();
                client.getOutputStream().write(jsonBytes);
                client.getOutputStream().flush();

                client.close();
                return;
            }

            if (path.equals("/")) {
                path = "/index.html";
            }

            File[] searchPaths = {
                new File("Src/template" + path),
                new File("Src" + path),
                new File("." + path)
            };

            File file = null;
            for (File f : searchPaths) {
                if (f.exists() && f.isFile()) {
                    file = f;
                    break;
                }
            }

            if (file == null) {
                out.print("HTTP/1.1 404 Not Found\r\n");
                out.print("Content-Type: text/plain\r\n");
                out.print("\r\n");
                out.print("404 - File not found");
                out.flush();
                client.close();
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            if (path.endsWith(".css"))  contentType = "text/css";
            if (path.endsWith(".js"))   contentType = "application/javascript";

            byte[] content = java.nio.file.Files.readAllBytes(file.toPath());

            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: " + contentType + "\r\n");
            out.print("Content-Length: " + content.length + "\r\n");
            out.print("\r\n");
            out.flush();

            client.getOutputStream().write(content);
            client.getOutputStream().flush();

            client.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String readHttpRequestLine(BufferedInputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                break;
            }
            if (previous != -1) {
                buffer.write(previous);
            }
            previous = current;
        }

        if (current == -1 && buffer.size() == 0) {
            return null;
        }

        if (previous != -1 && previous != '\r') {
            buffer.write(previous);
        }

        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}
