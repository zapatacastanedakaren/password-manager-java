package com.passwordmanager.db;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Este listener arranca y cierra el pool junto con Tomcat.
 *
 * Se ejecuta automáticamente cuando Tomcat inicia y cuando se detiene.
 * Garantiza que el pool se cierre limpiamente al apagar la app.
 */
@WebListener
public class AppLifecycle implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Forzar la inicialización del pool al arrancar
        // (el bloque static de DatabasePool se ejecuta aquí)
        try {
            DatabasePool.getConnection().close();
            System.out.println("✅ Aplicación iniciada correctamente.");
        } catch (Exception e) {
            System.err.println("❌ Error al verificar conexión inicial: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DatabasePool.close();
    }
}