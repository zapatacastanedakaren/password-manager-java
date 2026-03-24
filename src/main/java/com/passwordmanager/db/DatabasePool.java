package com.passwordmanager.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton que mantiene un único pool de conexiones durante
 * toda la vida de la aplicación.
 *
 * ¿Por qué un pool?
 * Crear una conexión a MySQL cuesta ~50-150ms.
 * Con un pool, las conexiones se reutilizan: el costo se paga
 * una sola vez al arrancar, no en cada request.
 */
public class DatabasePool {
    private static HikariDataSource dataSource;

    // Bloque estático: se ejecuta una sola vez cuando la clase se carga
    static {
        try {
            // Leer configuración desde hikari.properties
            Properties props = new Properties();
            InputStream input = DatabasePool.class
                    .getClassLoader()
                    .getResourceAsStream("hikari.properties");

            if (input == null) {
                throw new RuntimeException("No se encontró hikari.properties en el classpath.");
            }

            props.load(input);

            HikariConfig config = new HikariConfig(props);
            dataSource = new HikariDataSource(config);

            System.out.println("✅ Pool de conexiones iniciado: " + config.getPoolName());
        } catch (Exception e) {
            throw new RuntimeException("Error al iniciar el pool de conexiones: " + e.getMessage(), e);
        }
    }

    // Constructor privado: nadie puede instanciar esta clase
    private DatabasePool() {}

    /**
     * Retorna una conexión del pool.
     * IMPORTANTE: siempre usar en try-with-resources para devolverla al pool.
     *
     * Ejemplo:
     *  try(Connection conn = DatabasePool.getConnection()) {
     *      // usar conn
     *  }
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool al apagar la aplicación.
     * Se llama desde el ServletContextListener.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔒 Pool de conexiones cerrado.");
        }
    }
}