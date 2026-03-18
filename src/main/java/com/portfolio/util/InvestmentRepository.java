package com.portfolio.util;

import com.portfolio.model.Investment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestmentRepository {

    // ── CREATE ────────────────────────────────────────────────

    public static Investment insert(int userId, Investment inv) throws SQLException {
        String sql = "INSERT INTO investments (user_id,name,type,sector,amount,buy_price,sell_price,quantity,purchase_date,notes) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, inv.getName());
            ps.setString(3, inv.getType());
            ps.setString(4, inv.getSector());
            ps.setDouble(5, inv.getAmount());
            ps.setDouble(6, inv.getBuyPrice());
            ps.setDouble(7, inv.getSellPrice());
            ps.setInt(8, inv.getQuantity());
            ps.setString(9, inv.getPurchaseDate());
            ps.setString(10, inv.getNotes());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) inv.setId(rs.getInt("id"));
        }
        return inv;
    }

    // ── READ ──────────────────────────────────────────────────

    public static List<Investment> findAll(int userId) throws SQLException {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM investments WHERE user_id = ? ORDER BY id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static Investment findById(int userId, int id) throws SQLException {
        String sql = "SELECT * FROM investments WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    // ── UPDATE ────────────────────────────────────────────────

    public static boolean update(int userId, Investment inv) throws SQLException {
        String sql = "UPDATE investments SET name=?,type=?,sector=?,amount=?,buy_price=?,sell_price=?,quantity=?,purchase_date=?,notes=? " +
                     "WHERE id=? AND user_id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inv.getName());
            ps.setString(2, inv.getType());
            ps.setString(3, inv.getSector());
            ps.setDouble(4, inv.getAmount());
            ps.setDouble(5, inv.getBuyPrice());
            ps.setDouble(6, inv.getSellPrice());
            ps.setInt(7, inv.getQuantity());
            ps.setString(8, inv.getPurchaseDate());
            ps.setString(9, inv.getNotes());
            ps.setInt(10, inv.getId());
            ps.setInt(11, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    public static boolean delete(int userId, int id) throws SQLException {
        String sql = "DELETE FROM investments WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── MAP ResultSet → Investment ────────────────────────────

    private static Investment map(ResultSet rs) throws SQLException {
        Investment inv = new Investment();
        inv.setId(rs.getInt("id"));
        inv.setName(rs.getString("name"));
        inv.setType(rs.getString("type"));
        inv.setSector(rs.getString("sector"));
        inv.setAmount(rs.getDouble("amount"));
        inv.setBuyPrice(rs.getDouble("buy_price"));
        inv.setSellPrice(rs.getDouble("sell_price"));
        inv.setQuantity(rs.getInt("quantity"));
        inv.setPurchaseDate(rs.getString("purchase_date"));
        inv.setNotes(rs.getString("notes"));
        return inv;
    }
}
