package com.portfolio.service;

import com.portfolio.model.ActivityLog;
import com.portfolio.model.ActivityLog.ActionType;
import com.portfolio.model.Investment;
import com.portfolio.model.Portfolio;
import com.portfolio.util.FileHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PortfolioService — business logic layer.
 * Sits between HTTP controllers and the Portfolio/FileHandler.
 */
public class PortfolioService {

    private final Portfolio       portfolio   = new Portfolio();
    private final List<ActivityLog> activityLogs = new ArrayList<>();
    private int logIdSeq = 1;
    private boolean autoSave = true;

    // ── Singleton ─────────────────────────────────────────────
    private static PortfolioService instance;
    private PortfolioService() {
        // Try to load persisted data on startup
        try {
            Portfolio loaded = FileHandler.load();
            portfolio.replaceAll(loaded.getAll(), loaded.getNextId());
            log(ActionType.LOAD, "Portfolio loaded from file on startup (" + portfolio.count() + " records)");
        } catch (Exception e) {
            log(ActionType.LOAD, "No saved portfolio found. Starting fresh.");
        }
        // Load activity logs
        activityLogs.addAll(FileHandler.loadLogs());
        if (!activityLogs.isEmpty()) logIdSeq = activityLogs.get(activityLogs.size() - 1).getId() + 1;
    }

    public static synchronized PortfolioService getInstance() {
        if (instance == null) instance = new PortfolioService();
        return instance;
    }

    // ── CRUD ─────────────────────────────────────────────────

    public Investment addInvestment(Investment inv) {
        validate(inv);
        if (inv.getAmount() == 0) {
            inv.setAmount(inv.getBuyPrice() * inv.getQuantity());
        }
        Investment saved = portfolio.add(inv);
        log(ActionType.ADD, "Added investment: " + saved.getName() +
            " (" + saved.getQuantity() + " units @ ₹" + saved.getBuyPrice() + ")");
        if (autoSave) silentSave();
        return saved;
    }

    public Investment getInvestment(int id) {
        Investment inv = portfolio.findById(id);
        if (inv == null) throw new IllegalArgumentException("Investment not found: " + id);
        return inv;
    }

    public List<Investment> getAllInvestments() {
        return portfolio.getAll();
    }

    public List<Investment> getInvestmentsByFilter(String filter) {
        return switch (filter.toLowerCase()) {
            case "profit"    -> portfolio.getProfitable();
            case "loss"      -> portfolio.getLossMaking();
            default          -> portfolio.getAll();
        };
    }

    public Investment updateInvestment(Investment inv) {
        validate(inv);
        if (!portfolio.update(inv))
            throw new IllegalArgumentException("Investment not found: " + inv.getId());
        log(ActionType.UPDATE, "Updated investment: " + inv.getName() +
            " — sell price ₹" + inv.getSellPrice());
        if (autoSave) silentSave();
        return inv;
    }

    public void deleteInvestment(int id) {
        Investment inv = portfolio.findById(id);
        if (inv == null) throw new IllegalArgumentException("Investment not found: " + id);
        portfolio.delete(id);
        log(ActionType.DELETE, "Deleted investment: " + inv.getName() + " (ID=" + id + ")");
        if (autoSave) silentSave();
    }

    // ── Summary ───────────────────────────────────────────────

    public String getSummaryJson() {
        return portfolio.summaryToJson();
    }

    // ── P&L Calculator (stateless) ────────────────────────────

    public String calculatePL(double buyPrice, double sellPrice, int qty, double fees) {
        double gross  = (sellPrice - buyPrice) * qty;
        double net    = gross - fees;
        double invest = buyPrice * qty;
        double pct    = invest == 0 ? 0 : (net / invest) * 100.0;
        log(ActionType.CALCULATE,
            "Calc: Buy=₹" + buyPrice + " Sell=₹" + sellPrice + " Qty=" + qty +
            " → P&L=₹" + String.format("%.2f", net));
        return String.format(
            "{\"grossPL\":%.2f,\"fees\":%.2f,\"netPL\":%.2f,\"returnPercent\":%.2f,\"isProfit\":%b}",
            gross, fees, net, pct, net >= 0);
    }

    // ── Save / Load ───────────────────────────────────────────

    public void save(boolean backup) throws IOException {
        FileHandler.save(portfolio, backup);
        log(ActionType.SAVE, "Portfolio saved to " + FileHandler.getPortfolioFilePath());
    }

    public void load() throws IOException, ClassNotFoundException {
        Portfolio loaded = FileHandler.load();
        portfolio.replaceAll(loaded.getAll(), loaded.getNextId());
        log(ActionType.LOAD, "Portfolio loaded: " + portfolio.count() + " investments");
    }

    // ── Export / Import ───────────────────────────────────────

    public String exportCsv() throws IOException {
        String path = FileHandler.exportToCsv(portfolio);
        log(ActionType.EXPORT, "Exported portfolio to CSV: " + path);
        return path;
    }

    public String exportJson() throws IOException {
        String path = FileHandler.exportToJson(portfolio);
        log(ActionType.EXPORT, "Exported portfolio to JSON: " + path);
        return path;
    }

    public String exportTxt() throws IOException {
        String path = FileHandler.exportToTxt(portfolio);
        log(ActionType.EXPORT, "Exported portfolio to TXT: " + path);
        return path;
    }

    public int importCsv(String filePath) throws IOException {
        List<Investment> imported = FileHandler.importFromCsv(filePath);
        for (Investment inv : imported) portfolio.add(inv);
        log(ActionType.IMPORT, "Imported " + imported.size() + " investments from CSV: " + filePath);
        if (autoSave) silentSave();
        return imported.size();
    }

    // ── Activity Log ──────────────────────────────────────────

    public List<ActivityLog> getActivityLogs() {
        return new ArrayList<>(activityLogs);
    }

    public void clearActivityLogs() throws IOException {
        activityLogs.clear();
        logIdSeq = 1;
        FileHandler.clearLogs();
    }

    // ── File Info ─────────────────────────────────────────────

    public String getFileInfoJson() {
        return String.format(
            "{\"path\":\"%s\",\"exists\":%b,\"sizeBytes\":%d,\"investmentCount\":%d}",
            FileHandler.getPortfolioFilePath().replace("\\", "\\\\"),
            FileHandler.portfolioFileExists(),
            FileHandler.portfolioFileSize(),
            portfolio.count()
        );
    }

    // ── Settings ─────────────────────────────────────────────
    public void setAutoSave(boolean autoSave) { this.autoSave = autoSave; }
    public boolean isAutoSave()               { return autoSave; }

    // ── Helpers ───────────────────────────────────────────────

    private void validate(Investment inv) {
        if (inv.getName() == null || inv.getName().isBlank())
            throw new IllegalArgumentException("Investment name is required");
        if (inv.getBuyPrice() < 0)
            throw new IllegalArgumentException("Buy price must be >= 0");
        if (inv.getSellPrice() < 0)
            throw new IllegalArgumentException("Sell price must be >= 0");
        if (inv.getQuantity() <= 0)
            throw new IllegalArgumentException("Quantity must be > 0");
    }

    private void log(ActionType type, String description) {
        ActivityLog al = new ActivityLog(logIdSeq++, type, description);
        activityLogs.add(0, al); // newest first
        FileHandler.appendLog(al);
    }

    private void silentSave() {
        try { FileHandler.save(portfolio, false); }
        catch (IOException e) { System.err.println("[Service] Auto-save failed: " + e.getMessage()); }
    }
}
