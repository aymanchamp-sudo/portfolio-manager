package com.portfolio;

import com.portfolio.util.Database;
import com.portfolio.controller.Router;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * PortfolioServer — entry point.
 *
 * Starts a lightweight HTTP server on port 8080 using Java's built-in
 * com.sun.net.httpserver (no external dependencies needed).
 *
 * Run:
 *   javac -d out src/**‌/*.java && java -cp out com.portfolio.PortfolioServer
 *
 * Or with the provided build script:
 *   ./build.sh
 */
public class PortfolioServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = PORT;
        // Render (and most cloud platforms) assign port via $PORT env variable
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try { port = Integer.parseInt(envPort.trim()); }
            catch (NumberFormatException e) { /* use default */ }
        } else if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { /* use default */ }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        try {
            Database.init();
            Database.initSchema();
        } catch (Exception e) {
            System.err.println("[Server] Database init failed: " + e.getMessage());
        }
        Router router = new Router();

        // All API requests
        server.createContext("/api", router::handle);

        // Optional: serve the frontend HTML at /
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                java.io.File html = new java.io.File("index.html");
                if (html.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(html.toPath());
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                    return;
                }
            }
            // 404 for anything else
            String body = "Not Found";
            exchange.sendResponseHeaders(404, body.length());
            exchange.getResponseBody().write(body.getBytes());
            exchange.getResponseBody().close();
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       PortfolioX Backend Server v1.0.0       ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf( "║  Server  : http://localhost:%-17d║%n", port);
        System.out.println("║  API     : http://localhost:" + port + "/api/health    ║");
        System.out.println("║  Storage : ./data/portfolio.dat              ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  Press Ctrl+C to stop                        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutting down...");
            server.stop(1);
            System.out.println("[Server] Goodbye.");
        }));
    }
}
