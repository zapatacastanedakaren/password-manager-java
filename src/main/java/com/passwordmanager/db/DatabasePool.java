package com.passwordmanager.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabasePool {
    private static HikariDataSource dataSource;

    static {
        try {
            // ✅ FIX CRÍTICO: Forzar registro explícito del driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            HikariConfig config = new HikariConfig();
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");
            
            if (dbUrl == null || dbUrl.isBlank()) {
                // Fallback a hikari.properties para desarrollo local
                Properties props = new Properties();
                InputStream input = DatabasePool.class
                    .getClassLoader()
                    .getResourceAsStream("hikari.properties");
                
                if (input == null) {
                    throw new RuntimeException("No se encontró hikari.properties ni variables de entorno DB_*.");
                }
                props.load(input);
                config = new HikariConfig(props);
            } else {
                // Usar variables de entorno (Docker)
                config.setJdbcUrl(dbUrl);
                config.setUsername(dbUser);
                config.setPassword(dbPass);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(30000);
                config.setPoolName("PasswordManagerPool");
            }
            
            dataSource = new HikariDataSource(config);
            System.out.println("✅ Pool iniciado: " + config.getPoolName());
            
        } catch (Exception e) {
            System.err.println("❌ ERROR EN DATABASEPOOL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al iniciar el pool: " + e.getMessage(), e);
        }
    }

    private DatabasePool() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔒 Pool cerrado.");
        }
    }
}