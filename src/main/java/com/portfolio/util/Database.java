package com.portfolio.util;

import java.sql.*;
import java.util.Properties;

public class Database {

    private static String jdbcUrl;
    private static Properties jdbcProps;

    public static void init() throws SQLException {
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank())
            throw new SQLException("DATABASE_URL environment variable not set");
        parseUrl(rawUrl);
        // Test connection on startup
        try (Connection c = getConnection()) {
            System.out.println("[DB] Connection test successful.");
        }
    }

    /** Returns a fresh connection every time — safe for use with Session Pooler */
    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null)
            throw new SQLException("Database not initialised — call Database.init() first");
        return DriverManager.getConnection(jdbcUrl, jdbcProps);
    }

    private static void parseUrl(String rawUrl) throws SQLException {
        String url = rawUrl;
        for (String scheme : new String[]{"jdbc:postgresql://", "postgresql://", "postgres://"}) {
            if (url.startsWith(scheme)) { url = url.substring(scheme.length()); break; }
        }

        int atIndex = url.lastIndexOf('@');
        if (atIndex < 0) throw new SQLException("Invalid DATABASE_URL: missing @");

        String userInfo = url.substring(0, atIndex);
        String hostPart = url.substring(atIndex + 1);

        int colonIdx = userInfo.indexOf(':');
        String user     = colonIdx >= 0 ? userInfo.substring(0, colonIdx) : userInfo;
        String password = colonIdx >= 0 ? userInfo.substring(colonIdx + 1) : "";

        String host = hostPart;
        String db   = "postgres";
        int slashIdx = hostPart.indexOf('/');
        if (slashIdx >= 0) {
            host = hostPart.substring(0, slashIdx);
            db   = hostPart.substring(slashIdx + 1);
            int qIdx = db.indexOf('?');
            if (qIdx >= 0) db = db.substring(0, qIdx);
        }
        int portColon = host.lastIndexOf(':');
        String hostname = portColon >= 0 ? host.substring(0, portColon) : host;
        int    port     = portColon >= 0 ? Integer.parseInt(host.substring(portColon + 1)) : 5432;

        jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + db + "?sslmode=require";
        jdbcProps = new Properties();
        jdbcProps.setProperty("user",     user);
        jdbcProps.setProperty("password", password);

        System.out.println("[DB] Configured: " + hostname + ":" + port + "/" + db + " as " + user);
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
            e.printStackTrace();
        }
    }
}
