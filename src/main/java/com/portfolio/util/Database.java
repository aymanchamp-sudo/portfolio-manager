package com.portfolio.util;

import java.sql.*;
import java.util.Properties;

public class Database {

    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String rawUrl = System.getenv("DATABASE_URL");
            if (rawUrl == null || rawUrl.isBlank())
                throw new SQLException("DATABASE_URL environment variable not set");
            try {
                connection = buildConnection(rawUrl);
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLException("DB connection failed: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Manually parses postgresql://user:password@host:port/db
     * Avoids java.net.URI which rejects special characters like [ ] @ in passwords.
     */
    private static Connection buildConnection(String rawUrl) throws SQLException {
        // Strip scheme
        String url = rawUrl;
        for (String scheme : new String[]{"jdbc:postgresql://", "postgresql://", "postgres://"}) {
            if (url.startsWith(scheme)) { url = url.substring(scheme.length()); break; }
        }

        // Split user:password@rest  — find LAST @ to handle @ in passwords
        int atIndex = url.lastIndexOf('@');
        if (atIndex < 0) throw new SQLException("Invalid DATABASE_URL: missing @");

        String userInfo = url.substring(0, atIndex);
        String hostPart = url.substring(atIndex + 1);

        // Split user:password — find FIRST colon
        int colonIdx = userInfo.indexOf(':');
        String user     = colonIdx >= 0 ? userInfo.substring(0, colonIdx) : userInfo;
        String password = colonIdx >= 0 ? userInfo.substring(colonIdx + 1) : "";

        // Split host:port/db
        String host = hostPart;
        String db   = "postgres";
        int slashIdx = hostPart.indexOf('/');
        if (slashIdx >= 0) {
            host = hostPart.substring(0, slashIdx);
            db   = hostPart.substring(slashIdx + 1);
            // strip any query string from db name
            int qIdx = db.indexOf('?');
            if (qIdx >= 0) db = db.substring(0, qIdx);
        }
        int portColon = host.lastIndexOf(':');
        String hostname = portColon >= 0 ? host.substring(0, portColon) : host;
        int    port     = portColon >= 0 ? Integer.parseInt(host.substring(portColon + 1)) : 5432;

        String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + db + "?sslmode=require";

        Properties props = new Properties();
        props.setProperty("user",     user);
        props.setProperty("password", password);

        System.out.println("[DB] Connecting to: " + hostname + ":" + port + "/" + db + " as " + user);
        return DriverManager.getConnection(jdbcUrl, props);
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
