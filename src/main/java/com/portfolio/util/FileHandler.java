package com.portfolio.util;

import com.portfolio.model.ActivityLog;
import com.portfolio.model.Investment;
import com.portfolio.model.Portfolio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FileHandler — all disk I/O for the Portfolio Manager.
 *
 * Supports:
 *  • Binary serialisation (.dat) — save / load
 *  • CSV export / import
 *  • JSON export
 *  • TXT human-readable export
 *  • Activity log persistence (.log)
 */
public class FileHandler {

    private static final String DATA_DIR      = "data/";
    private static final String PORTFOLIO_DAT = DATA_DIR + "portfolio.dat";
    private static final String ACTIVITY_LOG  = DATA_DIR + "activity.log";
    private static final String BACKUP_DAT    = DATA_DIR + "portfolio.bak";

    // ── Ensure data directory exists ─────────────────────────
    static {
        new File(DATA_DIR).mkdirs();
    }

    // ══════════════════════════════════════════════════════════
    //  SAVE / LOAD  (binary .dat)
    // ══════════════════════════════════════════════════════════

    /**
     * Save portfolio to binary file.
     * @param portfolio Portfolio to save
     * @param backup    If true, also write a .bak copy
     */
    public static void save(Portfolio portfolio, boolean backup) throws IOException {
        if (backup && Files.exists(Paths.get(PORTFOLIO_DAT))) {
            Files.copy(Paths.get(PORTFOLIO_DAT), Paths.get(BACKUP_DAT),
                       StandardCopyOption.REPLACE_EXISTING);
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(PORTFOLIO_DAT)))) {
            oos.writeObject(portfolio);
        }
    }

    /** Load portfolio from binary file. Returns new empty Portfolio if file missing. */
    public static Portfolio load() throws IOException, ClassNotFoundException {
        File f = new File(PORTFOLIO_DAT);
        if (!f.exists()) return new Portfolio();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            return (Portfolio) ois.readObject();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CSV EXPORT / IMPORT
    // ══════════════════════════════════════════════════════════

    public static String exportToCsv(Portfolio portfolio) throws IOException {
        String path = DATA_DIR + "portfolio_export.csv";
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            pw.println(Investment.csvHeader());
            for (Investment inv : portfolio.getAll()) {
                pw.println(inv.toCsvLine());
            }
        }
        return path;
    }

    /**
     * Import investments from a CSV file.
     * Expected columns: name,type,sector,amount,buyPrice,sellPrice,quantity,purchaseDate,notes
     * (id column optional — will be reassigned)
     */
    public static List<Investment> importFromCsvContent(String csvContent) throws IOException {
        List<Investment> list = new ArrayList<>();
        List<String> lines = java.util.Arrays.asList(csvContent.split("\\r?\\n"));
        return parseCsvLines(list, lines);
    }

    public static List<Investment> importFromCsv(String filePath) throws IOException {
        List<Investment> list = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        return parseCsvLines(list, lines);
    }

    private static List<Investment> parseCsvLines(List<Investment> list, List<String> lines) {
        if (lines.isEmpty()) return list;

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] cols = parseCsvLine(line);
            if (cols.length < 8) continue;

            Investment inv = new Investment();
            int offset = 0;
            // If first col looks like a number, it's an id column — skip it
            try { Integer.parseInt(cols[0]); offset = 1; } catch (NumberFormatException ignored) {}

            inv.setName(cols[offset]);
            inv.setType(cols.length > offset + 1 ? cols[offset + 1] : "Stock");
            inv.setSector(cols.length > offset + 2 ? cols[offset + 2] : "");
            inv.setAmount(parseDouble(cols, offset + 3));
            inv.setBuyPrice(parseDouble(cols, offset + 4));
            inv.setSellPrice(parseDouble(cols, offset + 5));
            inv.setQuantity((int) parseDouble(cols, offset + 6));
            inv.setPurchaseDate(cols.length > offset + 7 ? cols[offset + 7] : "");
            inv.setNotes(cols.length > offset + 8 ? cols[offset + 8] : "");
            list.add(inv);
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  JSON EXPORT
    // ══════════════════════════════════════════════════════════

    public static String exportToJson(Portfolio portfolio) throws IOException {
        String path = DATA_DIR + "portfolio_export.json";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"portfolio\":[");
        List<Investment> all = portfolio.getAll();
        for (int i = 0; i < all.size(); i++) {
            sb.append(all.get(i).toJson());
            if (i < all.size() - 1) sb.append(",");
        }
        sb.append("],\"summary\":").append(portfolio.summaryToJson()).append("}");

        Files.writeString(Paths.get(path), sb.toString(), StandardCharsets.UTF_8);
        return path;
    }

    // ══════════════════════════════════════════════════════════
    //  TXT HUMAN-READABLE EXPORT
    // ══════════════════════════════════════════════════════════

    public static String exportToTxt(Portfolio portfolio) throws IOException {
        String path = DATA_DIR + "portfolio_report.txt";
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            pw.println("=".repeat(70));
            pw.println("              PORTFOLIO MANAGER — INVESTMENT REPORT");
            pw.println("=".repeat(70));
            pw.printf("%-5s %-22s %-12s %10s %10s %12s%n",
                      "ID", "Name", "Type", "Buy(₹)", "Sell(₹)", "P&L(₹)");
            pw.println("-".repeat(70));
            for (Investment inv : portfolio.getAll()) {
                pw.printf("%-5d %-22s %-12s %10.2f %10.2f %+12.2f%n",
                          inv.getId(), inv.getName(), inv.getType(),
                          inv.getBuyPrice(), inv.getSellPrice(), inv.getProfitLoss());
            }
            pw.println("-".repeat(70));
            pw.printf("%-40s %+.2f%n", "NET P&L:", portfolio.getTotalProfitLoss());
            pw.printf("%-40s %.2f%n",  "TOTAL INVESTED:", portfolio.getTotalInvested());
            pw.printf("%-40s %.2f%%%n","OVERALL RETURN:", portfolio.getOverallReturnPercent());
            pw.println("=".repeat(70));
        }
        return path;
    }

    // ══════════════════════════════════════════════════════════
    //  ACTIVITY LOG
    // ══════════════════════════════════════════════════════════

    public static void appendLog(ActivityLog log) {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(ACTIVITY_LOG, true), StandardCharsets.UTF_8))) {
            pw.println(log.toJson());
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to write activity log: " + e.getMessage());
        }
    }

    public static List<ActivityLog> loadLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        File f = new File(ACTIVITY_LOG);
        if (!f.exists()) return logs;
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                // minimal parse — just extract fields
                ActivityLog al = parseLogJson(line);
                if (al != null) logs.add(al);
            }
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to read logs: " + e.getMessage());
        }
        return logs;
    }

    public static void clearLogs() throws IOException {
        Files.deleteIfExists(Paths.get(ACTIVITY_LOG));
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private static double parseDouble(String[] arr, int idx) {
        try { return idx < arr.length ? Double.parseDouble(arr[idx].trim()) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    /** Very simple CSV line parser (handles quoted fields) */
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(sb.toString().trim()); sb.setLength(0); }
            else { sb.append(c); }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    /** Minimal JSON parser for ActivityLog lines */
    private static ActivityLog parseLogJson(String json) {
        try {
            ActivityLog al = new ActivityLog();
            al.setId(Integer.parseInt(extractJson(json, "id")));
            al.setActionType(ActivityLog.ActionType.valueOf(extractJson(json, "actionType")));
            al.setDescription(extractJson(json, "description"));
            al.setTimestamp(extractJson(json, "timestamp"));
            return al;
        } catch (Exception e) { return null; }
    }

    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        idx += search.length();
        if (idx >= json.length()) return "";
        char first = json.charAt(idx);
        if (first == '"') {
            int end = json.indexOf('"', idx + 1);
            return end > idx ? json.substring(idx + 1, end) : "";
        } else {
            int end = json.indexOf(',', idx);
            if (end < 0) end = json.indexOf('}', idx);
            return end > idx ? json.substring(idx, end).trim() : "";
        }
    }

    public static String getPortfolioFilePath() { return new File(PORTFOLIO_DAT).getAbsolutePath(); }
    public static boolean portfolioFileExists()  { return new File(PORTFOLIO_DAT).exists(); }
    public static long    portfolioFileSize()    { return new File(PORTFOLIO_DAT).length(); }
}
