package com.passwordmanager.servlet;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Base64;

@WebServlet("/api/auth/me")
public class MeServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        
        res.setContentType("application/json;charset=UTF-8");
        
        // Verificar header Authorization
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendJson(res, 401, "error", "No autenticado.");
            return;
        }
        
        try {
            // Decodificar credenciales
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            
            if (parts.length != 2) {
                sendJson(res, 401, "error", "Credenciales inválidas.");
                return;
            }
            
            String email = parts[0];
            String password = parts[1];
            
            // Verificar en base de datos
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(email);
            
            if (user == null || !user.getPassword().equals(password)) {
                sendJson(res, 401, "error", "Credenciales inválidas.");
                return;
            }
            
            // Éxito - devolver usuario (JSON manual, sin Gson)
            String json = String.format(
                "{\"status\":\"success\",\"user\":{\"id\":%d,\"email\":\"%s\",\"username\":\"%s\"}}",
                user.getId(),
                escapeJson(user.getEmail()),
                escapeJson(user.getUsername())
            );
            
            res.setStatus(200);
            res.getWriter().write(json);
            
        } catch (IllegalArgumentException e) {
            sendJson(res, 401, "error", "Credenciales inválidas.");
        } catch (Exception e) {
            sendJson(res, 401, "error", "Error de autenticación.");
        }
    }
    
    private void sendJson(HttpServletResponse res, int status, String statusVal, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(String.format("{\"status\":\"%s\",\"message\":\"%s\"}", statusVal, message));
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}