package server;

import ais.AISAttribution;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Backend for AirSarFusion.
 *
 * On startup:
 *   1) Runs the AIS attribution algorithm (backend/src/ais) against
 *      backend/data/drift_summary.csv + backend/data/ais_data.csv,
 *      writing backend/data/results.json.
 *   2) Starts an HTTP server that serves:
 *        GET /                 -> frontend/index.html
 *        GET /data/results.json -> the freshly generated results
 *
 * Reads the PORT environment variable so it works both locally and on
 * Railway (Railway assigns a random port and expects your app to read it
 * from $PORT -- it does NOT let you hardcode 8000/8080).
 */
public class ApiServer {

    public static void main(String[] args) throws Exception {

        String eventsFile = "data/drift_summary.csv";
        String aisFile = "data/ais_data.csv";
        String outFile = "data/results.json";

        System.out.println("Running AIS attribution...");
        AISAttribution.runAttribution(eventsFile, aisFile, outFile);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ApiServer::handleRequest);
        server.start();

        System.out.println("Server started on port " + port);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {

        String requestPath = exchange.getRequestURI().getPath();

        // Map "/" to the frontend page
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // Figure out which folder on disk this request maps to:
        //   /data/results.json  -> data/results.json
        //   anything else       -> ../frontend/<path>  (see Dockerfile, which
        //                          places frontend/ next to the compiled backend)
        Path filePath;
        if (requestPath.startsWith("/data/")) {
            filePath = Paths.get("." + requestPath).normalize();
        } else {
            filePath = Paths.get("../frontend" + requestPath).normalize();
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            String message = "404 - File not found: " + requestPath;
            exchange.sendResponseHeaders(404, message.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(message.getBytes());
            }
            return;
        }

        String contentType = "text/plain";
        if (requestPath.endsWith(".html")) contentType = "text/html";
        else if (requestPath.endsWith(".json")) contentType = "application/json";
        else if (requestPath.endsWith(".css")) contentType = "text/css";
        else if (requestPath.endsWith(".js")) contentType = "application/javascript";

        byte[] fileBytes = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(fileBytes);
        }
    }
}
