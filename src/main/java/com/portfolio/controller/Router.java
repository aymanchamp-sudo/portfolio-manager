package com.portfolio.controller;

import com.portfolio.model.ActivityLog;
import com.portfolio.model.Investment;
import com.portfolio.service.PortfolioService;
import com.portfolio.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Router — maps every HTTP method + path to a handler.
 * Hardened for deployment: proper CORS, 404s, trailing slash normalisation,
 * OPTIONS preflight, null-safe body reading, meaningful error messages.
 */
public class Router {

    private final PortfolioService svc = PortfolioService.getInstance();

    public void handle(HttpExchange ex) throws IOException {
        // CORS headers — required for all browser fetch() calls
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        // Browser sends OPTIONS before every cross-origin request
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 204, "");
            return;
        }

        String method = ex.getRequestMethod().toUpperCase();
        String rawPath = ex.getRequestURI().getPath();
        // Normalise trailing slash
        String path = rawPath.length() > 1 && rawPath.endsWith("/")
                      ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
        String query = ex.getRequestURI().getRawQuery();

        try {
            String response = dispatch(method, path, query, ex);
            send(ex, 200, response);
        } catch (IllegalArgumentException e) {
            send(ex, 400, JsonParser.error(e.getMessage()));
        } catch (FileNotFoundException e) {
            send(ex, 404, JsonParser.error("Not found: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonParser.error("Server error: " + e.getMessage()));
        }
    }

    private String dispatch(String method, String path, String query, HttpExchange ex)
            throws Exception {

        // HEALTH
        if (path.equals("/api/health") && "GET".equals(method)) {
            return "{\"status\":\"ok\",\"server\":\"PortfolioX Backend\",\"version\":\"1.0.0\","
                 + "\"investments\":" + svc.getAllInvestments().size() + "}";
        }

        // INVESTMENTS — collection
        if (path.equals("/api/investments")) {
            return switch (method) {
                case "GET"  -> getInvestments(query);
                case "POST" -> addInvestment(readBody(ex));
                default     -> throw new IllegalArgumentException("Method not allowed: " + method);
            };
        }

        // INVESTMENTS — single resource
        if (path.matches("/api/investments/\\d+")) {
            int id = extractId(path);
            return switch (method) {
                case "GET"    -> JsonParser.ok(getOrThrow(id).toJson());
                case "PUT"    -> updateInvestment(id, readBody(ex));
                case "DELETE" -> deleteInvestment(id);
                default       -> throw new IllegalArgumentException("Method not allowed: " + method);
            };
        }

        // SUMMARY
        if (path.equals("/api/summary") && "GET".equals(method)) {
            return JsonParser.ok(svc.getSummaryJson());
        }

        // P&L CALCULATOR
        if (path.equals("/api/calculate") && "POST".equals(method)) {
            return calculate(readBody(ex));
        }

        // SAVE
        if (path.equals("/api/save") && "POST".equals(method)) {
            String body = readBody(ex);
            boolean backup = true;
            if (!body.isBlank()) {
                Map<String,String> m = JsonParser.parseObject(body);
                backup = !"false".equals(m.getOrDefault("backup", "true"));
            }
            svc.save(backup);
            return JsonParser.okMsg("Portfolio saved successfully" + (backup ? " (backup created)" : ""));
        }

        // LOAD
        if (path.equals("/api/load") && "POST".equals(method)) {
            svc.load();
            int count = svc.getAllInvestments().size();
            return JsonParser.okMsg("Portfolio loaded: " + count + " investment" + (count != 1 ? "s" : ""));
        }

        // FILE INFO
        if (path.equals("/api/fileinfo") && "GET".equals(method)) {
            return JsonParser.ok(svc.getFileInfoJson());
        }

        // EXPORT
        if ("POST".equals(method)) {
            if (path.equals("/api/export/csv"))  return JsonParser.ok("path", svc.exportCsv());
            if (path.equals("/api/export/json")) return JsonParser.ok("path", svc.exportJson());
            if (path.equals("/api/export/txt"))  return JsonParser.ok("path", svc.exportTxt());
        }

        // IMPORT
        if (path.equals("/api/import/csv") && "POST".equals(method)) {
            Map<String,String> body = JsonParser.parseObject(readBody(ex));
            String filePath = body.getOrDefault("filePath", "").trim();
            if (filePath.isEmpty())
                throw new IllegalArgumentException("filePath is required in request body");
            int count = svc.importCsv(filePath);
            return JsonParser.okMsg("Imported " + count + " investment" + (count != 1 ? "s" : ""));
        }

        // ACTIVITY LOGS
        if (path.equals("/api/logs")) {
            return switch (method) {
                case "GET"    -> logsToJson(svc.getActivityLogs());
                case "DELETE" -> { svc.clearActivityLogs(); yield JsonParser.okMsg("Activity log cleared"); }
                default       -> throw new IllegalArgumentException("Method not allowed: " + method);
            };
        }

        // SETTINGS — auto-save toggle
        if (path.equals("/api/settings/autosave") && "POST".equals(method)) {
            Map<String,String> body = JsonParser.parseObject(readBody(ex));
            boolean autoSave = !"false".equalsIgnoreCase(body.getOrDefault("autoSave", "true"));
            svc.setAutoSave(autoSave);
            return JsonParser.okMsg("Auto-save " + (autoSave ? "enabled" : "disabled"));
        }

        throw new FileNotFoundException(method + " " + path);
    }

    // ── Handlers ─────────────────────────────────────────────

    private String getInvestments(String query) {
        String filter = "all";
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("filter=")) {
                    filter = URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8).trim();
                }
            }
        }
        return JsonParser.ok(JsonParser.listToJson(svc.getInvestmentsByFilter(filter)));
    }

    private String addInvestment(String body) {
        if (body == null || body.isBlank())
            throw new IllegalArgumentException("Request body is required");
        Investment saved = svc.addInvestment(JsonParser.mapToInvestment(JsonParser.parseObject(body)));
        return JsonParser.ok(saved.toJson());
    }

    private String updateInvestment(int id, String body) {
        if (body == null || body.isBlank())
            throw new IllegalArgumentException("Request body is required");
        Investment inv = JsonParser.mapToInvestment(JsonParser.parseObject(body));
        inv.setId(id);
        return JsonParser.ok(svc.updateInvestment(inv).toJson());
    }

    private String deleteInvestment(int id) {
        svc.deleteInvestment(id);
        return JsonParser.okMsg("Investment " + id + " deleted successfully");
    }

    private String calculate(String body) {
        if (body == null || body.isBlank())
            throw new IllegalArgumentException("buyPrice, sellPrice, quantity are required");
        Map<String,String> m = JsonParser.parseObject(body);
        double buy  = parseDouble(m, "buyPrice");
        double sell = parseDouble(m, "sellPrice");
        int    qty  = (int) parseDouble(m, "quantity");
        double fees = parseDouble(m, "fees");
        if (buy  <= 0) throw new IllegalArgumentException("buyPrice must be > 0");
        if (qty  <= 0) throw new IllegalArgumentException("quantity must be > 0");
        return JsonParser.ok(svc.calculatePL(buy, sell, qty, fees));
    }

    private String logsToJson(List<ActivityLog> logs) {
        StringBuilder sb = new StringBuilder("{\"status\":\"ok\",\"data\":[");
        for (int i = 0; i < logs.size(); i++) {
            sb.append(logs.get(i).toJson());
            if (i < logs.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ── Utilities ─────────────────────────────────────────────

    private Investment getOrThrow(int id) throws FileNotFoundException {
        return svc.getAllInvestments().stream()
                  .filter(i -> i.getId() == id)
                  .findFirst()
                  .orElseThrow(() -> new FileNotFoundException("Investment not found: id=" + id));
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        } else {
            ex.getResponseBody().close();
        }
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private double parseDouble(Map<String,String> m, String key) {
        String val = m.get(key);
        if (val == null || val.isBlank()) return 0.0;
        try { return Double.parseDouble(val.trim()); } catch (NumberFormatException e) { return 0.0; }
    }
}
