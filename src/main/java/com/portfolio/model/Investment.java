package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Investment — core data object.
 * Implements Serializable for binary file persistence.
 */
public class Investment implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Fields ──────────────────────────────────────────────
    private int    id;
    private String name;
    private String type;       // Stock, Mutual Fund, Gold, Bonds, Crypto, FD
    private String sector;     // IT, Banking, Energy, etc.
    private double amount;     // total invested amount
    private double buyPrice;
    private double sellPrice;
    private int    quantity;
    private String purchaseDate; // stored as ISO string yyyy-MM-dd
    private String notes;

    // ── Constructors ─────────────────────────────────────────
    public Investment() {}

    public Investment(int id, String name, String type, String sector,
                      double amount, double buyPrice, double sellPrice,
                      int quantity, String purchaseDate, String notes) {
        this.id           = id;
        this.name         = name;
        this.type         = type;
        this.sector       = sector;
        this.amount       = amount;
        this.buyPrice     = buyPrice;
        this.sellPrice    = sellPrice;
        this.quantity     = quantity;
        this.purchaseDate = purchaseDate;
        this.notes        = notes;
    }

    // ── Business Logic ────────────────────────────────────────

    /** P&L = (sellPrice - buyPrice) × quantity */
    public double getProfitLoss() {
        return (sellPrice - buyPrice) * quantity;
    }

    /** Return percentage relative to total invested */
    public double getReturnPercent() {
        if (amount == 0) return 0;
        return (getProfitLoss() / amount) * 100.0;
    }

    /** Current value = buyPrice × quantity + P&L */
    public double getCurrentValue() {
        return buyPrice * quantity + getProfitLoss();
    }

    public boolean isProfit() {
        return getProfitLoss() >= 0;
    }

    // ── Serialisation helper (CSV line) ──────────────────────
    public String toCsvLine() {
        return String.join(",",
                String.valueOf(id),
                escapeCsv(name),
                escapeCsv(type),
                escapeCsv(sector),
                String.valueOf(amount),
                String.valueOf(buyPrice),
                String.valueOf(sellPrice),
                String.valueOf(quantity),
                escapeCsv(purchaseDate != null ? purchaseDate : ""),
                escapeCsv(notes != null ? notes : "")
        );
    }

    public static String csvHeader() {
        return "id,name,type,sector,amount,buyPrice,sellPrice,quantity,purchaseDate,notes";
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ── JSON serialisation (manual, no external library) ─────
    public String toJson() {
        return String.format(
            "{\"id\":%d,\"name\":%s,\"type\":%s,\"sector\":%s," +
            "\"amount\":%.2f,\"buyPrice\":%.2f,\"sellPrice\":%.2f," +
            "\"quantity\":%d,\"purchaseDate\":%s,\"notes\":%s," +
            "\"profitLoss\":%.2f,\"returnPercent\":%.2f,\"currentValue\":%.2f,\"isProfit\":%b}",
            id, jsonStr(name), jsonStr(type), jsonStr(sector),
            amount, buyPrice, sellPrice,
            quantity, jsonStr(purchaseDate), jsonStr(notes),
            getProfitLoss(), getReturnPercent(), getCurrentValue(), isProfit()
        );
    }

    private String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ── Getters & Setters ─────────────────────────────────────
    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getType()              { return type; }
    public void   setType(String type)   { this.type = type; }

    public String getSector()                { return sector; }
    public void   setSector(String sector)   { this.sector = sector; }

    public double getAmount()                { return amount; }
    public void   setAmount(double amount)   { this.amount = amount; }

    public double getBuyPrice()                  { return buyPrice; }
    public void   setBuyPrice(double buyPrice)   { this.buyPrice = buyPrice; }

    public double getSellPrice()                   { return sellPrice; }
    public void   setSellPrice(double sellPrice)   { this.sellPrice = sellPrice; }

    public int  getQuantity()                  { return quantity; }
    public void setQuantity(int quantity)      { this.quantity = quantity; }

    public String getPurchaseDate()                      { return purchaseDate; }
    public void   setPurchaseDate(String purchaseDate)   { this.purchaseDate = purchaseDate; }

    public String getNotes()               { return notes; }
    public void   setNotes(String notes)   { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("Investment{id=%d, name='%s', type='%s', P&L=%.2f}",
                             id, name, type, getProfitLoss());
    }
}
