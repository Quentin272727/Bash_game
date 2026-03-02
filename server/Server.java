
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

    private static void handleClient(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream());
        ) {
            // Lecture de la requête HTTP (on ignore le contenu)
            String line = in.readLine();
            System.out.println("Requête reçue : " + line);

            // Chargement du fichier HTML
            File file = new File("Src/template/test.html");
            String html = new String(java.nio.file.Files.readAllBytes(file.toPath()));

            // Réponse HTTP
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + html.length());
            out.println();
            out.println(html);
            out.flush();

            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
