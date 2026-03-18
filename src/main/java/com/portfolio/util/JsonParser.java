package com.portfolio.util;

import com.portfolio.model.Investment;
import java.util.HashMap;
import java.util.Map;

/**
 * JsonParser — lightweight hand-rolled JSON parser.
 * No external dependencies; handles flat key-value JSON objects.
 */
public class JsonParser {

    /** Parse a flat JSON object into a String→String map */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);

        // Split by top-level commas
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            if (c == ',' && depth == 0) {
                parseKeyValue(current.toString().trim(), map);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) parseKeyValue(current.toString().trim(), map);
        return map;
    }

    private static void parseKeyValue(String pair, Map<String, String> map) {
        int colon = pair.indexOf(':');
        if (colon < 0) return;
        String key   = pair.substring(0, colon).trim().replace("\"", "");
        String value = pair.substring(colon + 1).trim();
        // Strip surrounding quotes if string value
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            // Unescape
            value = value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
        } else if (value.equals("null")) {
            value = "";
        }
        map.put(key, value);
    }

    /** Build Investment from parsed JSON map */
    public static Investment mapToInvestment(Map<String, String> m) {
        Investment inv = new Investment();
        if (m.containsKey("id"))           inv.setId(parseInt(m.get("id")));
        if (m.containsKey("name"))         inv.setName(m.get("name"));
        if (m.containsKey("type"))         inv.setType(m.get("type"));
        if (m.containsKey("sector"))       inv.setSector(m.get("sector"));
        if (m.containsKey("amount"))       inv.setAmount(parseDouble(m.get("amount")));
        if (m.containsKey("buyPrice"))     inv.setBuyPrice(parseDouble(m.get("buyPrice")));
        if (m.containsKey("sellPrice"))    inv.setSellPrice(parseDouble(m.get("sellPrice")));
        if (m.containsKey("quantity"))     inv.setQuantity(parseInt(m.get("quantity")));
        if (m.containsKey("purchaseDate")) inv.setPurchaseDate(m.get("purchaseDate"));
        if (m.containsKey("notes"))        inv.setNotes(m.get("notes"));
        return inv;
    }

    private static int    parseInt(String s)    { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }

    // ── Response Builders ────────────────────────────────────

    public static String ok(String dataJson) {
        return "{\"status\":\"ok\",\"data\":" + dataJson + "}";
    }

    public static String ok(String key, String value) {
        return "{\"status\":\"ok\",\"" + key + "\":\"" + escape(value) + "\"}";
    }

    public static String okMsg(String message) {
        return "{\"status\":\"ok\",\"message\":\"" + escape(message) + "\"}";
    }

    public static String error(String message) {
        return "{\"status\":\"error\",\"message\":\"" + escape(message) + "\"}";
    }

    public static String listToJson(java.util.List<Investment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
