package server;

import java.io.*;
import java.net.*;

public class Server {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Serveur lancé sur http://localhost:8080");

            while (true) {
                Socket client = serverSocket.accept();
                handleClient(client);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Détection Windows / Linux pour exécuter Play.sh correctement
    private static ProcessBuilder getProcessBuilder() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            // Windows → utiliser Git Bash
            return new ProcessBuilder("bash", "Script_Bash/Play.sh");
        } else {
            // Linux → exécuter directement
            return new ProcessBuilder("./Script_Bash/Play.sh");
        }
    }

    private static void handleClient(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream());

            // Lire la requête HTTP
            String request = in.readLine();
            System.out.println("Request: " + request);

            if (request == null) {
                client.close();
                return;
            }

            // Extraire le chemin demandé
            String path = request.split(" ")[1];

            // --- ROUTE /play : exécuter Play.sh ---
            if (path.equals("/play")) {

                // Lire les headers jusqu'à la ligne vide
                String line;
                while (!(line = in.readLine()).isEmpty()) {}

                // Lire le body (choix du joueur)
                StringBuilder body = new StringBuilder();
                while (in.ready()) {
                    body.append((char) in.read());
                }
                String playerChoice = body.toString().trim();
                System.out.println("Player choice = " + playerChoice);

                // Exécuter Play.sh
                ProcessBuilder pb = getProcessBuilder();
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Envoyer le choix au script Bash
                try (OutputStream os = process.getOutputStream()) {
                    os.write(playerChoice.getBytes());
                    os.flush();
                }

                // Lire la sortie JSON du script
                BufferedReader scriptOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String json = scriptOut.readLine();

                // Réponse HTTP JSON
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + json.length());
                out.println();
                out.println(json);
                out.flush();

                client.close();
                return;
            }

            // Par défaut → index.html
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Dossiers où chercher les fichiers statiques
            File[] searchPaths = {
                new File("Src/template" + path),
                new File("Src" + path),
                new File("." + path)
            };

            File file = null;

            for (File f : searchPaths) {
                if (f.exists()) {
                    file = f;
                    break;
                }
            }

            // Si aucun fichier trouvé → 404
            if (file == null) {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("404 - File not found");
                out.flush();
                client.close();
                return;
            }

            // Déterminer le type MIME
            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            if (path.endsWith(".css")) contentType = "text/css";
            if (path.endsWith(".js")) contentType = "application/javascript";

            // Lire le fichier
            byte[] content = java.nio.file.Files.readAllBytes(file.toPath());

            // Réponse HTTP
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + content.length);
            out.println();

            out.flush();

            client.getOutputStream().write(content);
            client.getOutputStream().flush();

            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}