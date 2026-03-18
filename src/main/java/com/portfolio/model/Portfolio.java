package com.portfolio.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Portfolio — in-memory store for all investments.
 * Serializable so it can be written directly to a .dat file.
 */
public class Portfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Investment>  investments = new ArrayList<>();
    private int               nextId      = 1;

    // ── CRUD ─────────────────────────────────────────────────

    public Investment add(Investment inv) {
        inv.setId(nextId++);
        investments.add(inv);
        return inv;
    }

    public Investment findById(int id) {
        return investments.stream()
                          .filter(i -> i.getId() == id)
                          .findFirst()
                          .orElse(null);
    }

    public boolean update(Investment updated) {
        for (int i = 0; i < investments.size(); i++) {
            if (investments.get(i).getId() == updated.getId()) {
                investments.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public boolean delete(int id) {
        return investments.removeIf(i -> i.getId() == id);
    }

    public List<Investment> getAll() {
        return new ArrayList<>(investments);
    }

    public List<Investment> getByType(String type) {
        return investments.stream()
                          .filter(i -> type.equalsIgnoreCase(i.getType()))
                          .collect(Collectors.toList());
    }

    public List<Investment> getProfitable() {
        return investments.stream()
                          .filter(Investment::isProfit)
                          .collect(Collectors.toList());
    }

    public List<Investment> getLossMaking() {
        return investments.stream()
                          .filter(i -> !i.isProfit())
                          .collect(Collectors.toList());
    }

    // ── Aggregate Calculations ────────────────────────────────

    public double getTotalInvested() {
        return investments.stream().mapToDouble(Investment::getAmount).sum();
    }

    public double getTotalCurrentValue() {
        return investments.stream().mapToDouble(Investment::getCurrentValue).sum();
    }

    public double getTotalProfitLoss() {
        return investments.stream().mapToDouble(Investment::getProfitLoss).sum();
    }

    public double getTotalProfit() {
        return investments.stream()
                          .filter(Investment::isProfit)
                          .mapToDouble(Investment::getProfitLoss)
                          .sum();
    }

    public double getTotalLoss() {
        return investments.stream()
                          .filter(i -> !i.isProfit())
                          .mapToDouble(Investment::getProfitLoss)
                          .sum();
    }

    public double getOverallReturnPercent() {
        double invested = getTotalInvested();
        if (invested == 0) return 0;
        return (getTotalProfitLoss() / invested) * 100.0;
    }

    public Investment getBestPerformer() {
        return investments.stream()
                          .max((a, b) -> Double.compare(a.getReturnPercent(), b.getReturnPercent()))
                          .orElse(null);
    }

    public Investment getWorstPerformer() {
        return investments.stream()
                          .min((a, b) -> Double.compare(a.getReturnPercent(), b.getReturnPercent()))
                          .orElse(null);
    }

    public int count() { return investments.size(); }

    public void clear() {
        investments.clear();
        nextId = 1;
    }

    /** Replace full investment list (used on load/import) */
    public void replaceAll(List<Investment> list, int nextId) {
        this.investments = new ArrayList<>(list);
        this.nextId      = nextId;
    }

    public int getNextId() { return nextId; }

    // ── JSON ─────────────────────────────────────────────────
    public String summaryToJson() {
        Investment best  = getBestPerformer();
        Investment worst = getWorstPerformer();
        return String.format(
            "{\"totalInvested\":%.2f,\"totalCurrentValue\":%.2f," +
            "\"totalProfitLoss\":%.2f,\"totalProfit\":%.2f,\"totalLoss\":%.2f," +
            "\"overallReturnPercent\":%.2f,\"count\":%d," +
            "\"bestPerformer\":%s,\"worstPerformer\":%s}",
            getTotalInvested(), getTotalCurrentValue(),
            getTotalProfitLoss(), getTotalProfit(), getTotalLoss(),
            getOverallReturnPercent(), count(),
            best  != null ? best.toJson()  : "null",
            worst != null ? worst.toJson() : "null"
        );
    }
}
