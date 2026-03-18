package com.portfolio.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
            } catch (Exception e) {
                throw new SQLException("DB connection failed: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    private static Connection buildConnection(String rawUrl) throws Exception {
        // Normalise scheme for URI parsing
        String uriStr = rawUrl
            .replaceFirst("^postgresql://", "postgres://")
            .replaceFirst("^jdbc:postgresql://", "postgres://");

        URI uri = new URI(uriStr);

        String host = uri.getHost();
        int    port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath();
        String db   = (path != null && path.startsWith("/")) ? path.substring(1) : "postgres";

        // Extract user:password — handles dots in username
        String userInfo = uri.getUserInfo();
        String user = "", password = "";
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                user     = URLDecoder.decode(userInfo.substring(0, colon), StandardCharsets.UTF_8);
                password = URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
            } else {
                user = userInfo;
            }
        }

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";

        Properties props = new Properties();
        props.setProperty("user",     user);
        props.setProperty("password", password);
        props.setProperty("ssl",      "true");

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
