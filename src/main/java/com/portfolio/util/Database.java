package com.portfolio.util;

import java.sql.*;

public class Database {

    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = System.getenv("DATABASE_URL");
            if (url == null || url.isBlank())
                throw new SQLException("DATABASE_URL environment variable not set");
            if (url.startsWith("postgres://"))
                url = url.replaceFirst("postgres://", "jdbc:postgresql://");
            else if (url.startsWith("postgresql://"))
                url = url.replaceFirst("postgresql://", "jdbc:postgresql://");
            // Append SSL params if not already present
            if (!url.contains("sslmode")) {
                url += (url.contains("?") ? "&" : "?") + "sslmode=require";
            }
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    public static void initSchema() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    token VARCHAR(64) PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS investments (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                    name VARCHAR(255) NOT NULL,
                    type VARCHAR(50),
                    sector VARCHAR(100),
                    amount DOUBLE PRECISION DEFAULT 0,
                    buy_price DOUBLE PRECISION DEFAULT 0,
                    sell_price DOUBLE PRECISION DEFAULT 0,
                    quantity INTEGER DEFAULT 0,
                    purchase_date VARCHAR(20),
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """);
            System.out.println("[DB] Schema initialised successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Schema init failed: " + e.getMessage());
        }
    }
}
