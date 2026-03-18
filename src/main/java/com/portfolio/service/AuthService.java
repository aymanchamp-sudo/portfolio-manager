package com.portfolio.service;

import com.portfolio.util.Database;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;

public class AuthService {

    private static AuthService instance;
    private AuthService() {}
    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    // ── Register ──────────────────────────────────────────────

    public int register(String username, String password) throws Exception {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username is required");
        if (password == null || password.length() < 4)
            throw new IllegalArgumentException("Password must be at least 4 characters");

        String hash = hashPassword(password);
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
            throw new Exception("Registration failed");
        } catch (SQLException e) {
            if (e.getMessage().contains("unique") || e.getMessage().contains("duplicate"))
                throw new IllegalArgumentException("Username already taken");
            throw e;
        }
    }

    // ── Login ─────────────────────────────────────────────────

    public String login(String username, String password) throws Exception {
        if (username == null || password == null)
            throw new IllegalArgumentException("Username and password are required");

        String hash = hashPassword(password);
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id FROM users WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                throw new IllegalArgumentException("Invalid username or password");
            int userId = rs.getInt("id");
            return createSession(userId);
        }
    }

    // ── Session ───────────────────────────────────────────────

    private String createSession(int userId) throws SQLException {
        String token = generateToken();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO sessions (token, user_id) VALUES (?, ?)")) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        return token;
    }

    public int getUserIdFromToken(String token) throws SQLException {
        if (token == null || token.isBlank()) return -1;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT user_id FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        }
        return -1;
    }

    public void logout(String token) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String generateToken() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
