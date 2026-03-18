package com.portfolio.controller;

import com.portfolio.model.Investment;
import com.portfolio.service.AuthService;
import com.portfolio.service.PortfolioService;
import com.portfolio.util.FileHandler;
import com.portfolio.util.InvestmentRepository;
import com.portfolio.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Router {

    private final AuthService auth = AuthService.getInstance();
    private final PortfolioService svc = PortfolioService.getInstance();

    public void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { send(ex, 204, ""); return; }

        String method  = ex.getRequestMethod().toUpperCase();
        String rawPath = ex.getRequestURI().getPath();
        String path    = rawPath.length() > 1 && rawPath.endsWith("/")
                         ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
        String query   = ex.getRequestURI().getRawQuery();

        try {
            String response = dispatch(method, path, query, ex);
            send(ex, 200, response);
        } catch (IllegalArgumentException e) {
            send(ex, 400, JsonParser.error(e.getMessage()));
        } catch (SecurityException e) {
            send(ex, 401, JsonParser.error(e.getMessage()));
        } catch (FileNotFoundException e) {
            send(ex, 404, JsonParser.error("Not found: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JsonParser.error("Server error: " + e.getMessage()));
        }
    }

    private String dispatch(String method, String path, String query, HttpExchange ex) throws Exception {

        if (path.equals("/api/health") && "GET".equals(method))
            return "{\"status\":\"ok\",\"server\":\"PortfolioX\",\"version\":\"2.0.0\"}";

        if (path.equals("/api/auth/register") && "POST".equals(method)) {
            Map<String,String> body = JsonParser.parseObject(readBody(ex));
            String username = body.getOrDefault("username", "").trim();
            String password = body.getOrDefault("password", "").trim();
            auth.register(username, password);
            String token = auth.login(username, password);
            return "{\"status\":\"ok\",\"token\":\"" + token + "\"}";
        }

        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            Map<String,String> body = JsonParser.parseObject(readBody(ex));
            String username = body.getOrDefault("username", "").trim();
            String password = body.getOrDefault("password", "").trim();
            String token = auth.login(username, password);
            return "{\"status\":\"ok\",\"token\":\"" + token + "\"}";
        }

        String token  = extractToken(ex);
        int    userId = auth.getUserIdFromToken(token);
        if (userId < 0) throw new SecurityException("Unauthorized — please log in");

        if (path.equals("/api/auth/logout") && "POST".equals(method)) {
            auth.logout(token);
            return JsonParser.okMsg("Logged out");
        }

        if (path.equals("/api/investments")) {
            return switch (method) {
                case "GET"  -> getInvestments(userId, query);
                case "POST" -> addInvestment(userId, readBody(ex));
                default     -> throw new IllegalArgumentException("Method not allowed: " + method);
            };
        }

        if (path.matches("/api/investments/\\d+")) {
            int id = extractId(path);
            return switch (method) {
                case "GET"    -> getSingle(userId, id);
                case "PUT"    -> updateInvestment(userId, id, readBody(ex));
                case "DELETE" -> deleteInvestment(userId, id);
                default       -> throw new IllegalArgumentException("Method not allowed: " + method);
            };
        }

        if (path.equals("/api/summary") && "GET".equals(method))
            return JsonParser.ok(svc.buildSummaryJson(InvestmentRepository.findAll(userId)));

        if (path.equals("/api/calculate") && "POST".equals(method))
            return calculate(readBody(ex));

        if (path.equals("/api/import/csv") && "POST".equals(method)) {
            String csv = readBody(ex);
            List<Investment> imported = FileHandler.importFromCsvContent(csv);
            for (Investment inv : imported) {
                if (inv.getAmount() == 0) inv.setAmount(inv.getBuyPrice() * inv.getQuantity());
                InvestmentRepository.insert(userId, inv);
            }
            return JsonParser.okMsg("Imported " + imported.size() + " investment" + (imported.size() != 1 ? "s" : ""));
        }

        if (path.equals("/api/export/csv") && "POST".equals(method)) {
            List<Investment> all = InvestmentRepository.findAll(userId);
            StringBuilder sb = new StringBuilder(Investment.csvHeader() + "\n");
            for (Investment inv : all) sb.append(inv.toCsvLine()).append("\n");
            return JsonParser.ok("csv", sb.toString());
        }

        if (path.equals("/api/save")     && "POST".equals(method)) return JsonParser.okMsg("Data is automatically saved to the database");
        if (path.equals("/api/load")     && "POST".equals(method)) return JsonParser.okMsg("Data loaded from database");
        if (path.equals("/api/fileinfo") && "GET".equals(method))  return JsonParser.ok("{\"source\":\"database\",\"investmentCount\":" + InvestmentRepository.findAll(userId).size() + "}");
        if (path.equals("/api/logs")     && "GET".equals(method))  return "{\"status\":\"ok\",\"data\":[]}";
        if (path.equals("/api/logs")     && "DELETE".equals(method)) return JsonParser.okMsg("Log cleared");
        if (path.equals("/api/settings/autosave") && "POST".equals(method)) return JsonParser.okMsg("Auto-save always on in database mode");

        throw new FileNotFoundException(method + " " + path);
    }

    private String getInvestments(int userId, String query) throws Exception {
        List<Investment> list = InvestmentRepository.findAll(userId);
        String filter = "all";
        if (query != null)
            for (String p : query.split("&"))
                if (p.startsWith("filter="))
                    filter = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8).trim();
        if ("profit".equalsIgnoreCase(filter)) list.removeIf(i -> !i.isProfit());
        else if ("loss".equalsIgnoreCase(filter)) list.removeIf(Investment::isProfit);
        return JsonParser.ok(JsonParser.listToJson(list));
    }

    private String addInvestment(int userId, String body) throws Exception {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Request body is required");
        Investment inv = JsonParser.mapToInvestment(JsonParser.parseObject(body));
        if (inv.getAmount() == 0) inv.setAmount(inv.getBuyPrice() * inv.getQuantity());
        return JsonParser.ok(InvestmentRepository.insert(userId, inv).toJson());
    }

    private String getSingle(int userId, int id) throws Exception {
        Investment inv = InvestmentRepository.findById(userId, id);
        if (inv == null) throw new FileNotFoundException("Investment not found: id=" + id);
        return JsonParser.ok(inv.toJson());
    }

    private String updateInvestment(int userId, int id, String body) throws Exception {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Request body is required");
        Investment inv = JsonParser.mapToInvestment(JsonParser.parseObject(body));
        inv.setId(id);
        if (!InvestmentRepository.update(userId, inv)) throw new FileNotFoundException("Investment not found: id=" + id);
        return JsonParser.ok(inv.toJson());
    }

    private String deleteInvestment(int userId, int id) throws Exception {
        if (!InvestmentRepository.delete(userId, id)) throw new FileNotFoundException("Investment not found: id=" + id);
        return JsonParser.okMsg("Investment " + id + " deleted successfully");
    }

    private String calculate(String body) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("buyPrice, sellPrice, quantity are required");
        Map<String,String> m = JsonParser.parseObject(body);
        double buy  = parseDouble(m, "buyPrice");
        double sell = parseDouble(m, "sellPrice");
        int    qty  = (int) parseDouble(m, "quantity");
        double fees = parseDouble(m, "fees");
        if (buy <= 0) throw new IllegalArgumentException("buyPrice must be > 0");
        if (qty <= 0) throw new IllegalArgumentException("quantity must be > 0");
        return JsonParser.ok(svc.calculatePL(buy, sell, qty, fees));
    }

    private String extractToken(HttpExchange ex) {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.substring(7).trim();
        return "";
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
        } else { ex.getResponseBody().close(); }
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
