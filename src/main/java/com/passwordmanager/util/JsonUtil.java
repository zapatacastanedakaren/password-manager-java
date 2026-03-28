package com.passwordmanager.util;

import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Centraliza el envío de respuestas JSON.
 * Todos los servlets usan estos métodos — nunca escriben
 * directamente al response.
 */
public class JsonUtil {
    private JsonUtil() {}

    /**
     * Lee el body del request como String.
     */
    public static String readBody(jakarta.servlet.http.HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (var reader = req.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Envía una respuesta JSON con el status HTTP indicado.
     */
    public static void send(HttpServletResponse res, int status, JSONObject body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(body.toString());
    }

    /** Respuesta de éxito con mensaje */
    public static void ok(HttpServletResponse res, String message) throws IOException {
        send(res, 200, new JSONObject()
            .put("status", "success")
            .put("message", message));
    }

    /** Respuesta de éxito con datos */
    public static void ok(HttpServletResponse res, JSONObject data) throws IOException {
        data.put("status", "success");
        send(res, 200, data);
    }

    /** Respuesta 201 Created */
    public static void created(HttpServletResponse res, JSONObject data) throws IOException {
        data.put("status", "success");
        send(res, 201, data);
    }

    /** Respuesta de error */
    public static void error(HttpServletResponse res, int status, String message) throws IOException {
        send(res, status, new JSONObject()
            .put("status", "error")
            .put("message", message));
    }
}