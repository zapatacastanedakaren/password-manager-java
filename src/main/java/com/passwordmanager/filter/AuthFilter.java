package com.passwordmanager.filter;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.Base64;

@WebFilter(urlPatterns = {"/api/*"})
public class AuthFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        
        // ✅ FIX: Obtener la ruta relativa al contexto
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        // Endpoints públicos (NO requieren autenticación)
        if (path.startsWith("/api/auth/register") || 
            path.startsWith("/api/auth/login")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Verificar header Authorization
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendError(res, 401, "No autenticado.");
            return;
        }
        
        // Decodificar credenciales Basic Auth
        try {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            
            if (parts.length != 2) {
                sendError(res, 401, "Credenciales inválidas.");
                return;
            }
            
            String email = parts[0];
            String password = parts[1];
            
            // Verificar en base de datos
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(email);
            
            if (user == null || !user.getPassword().equals(password)) {
                sendError(res, 401, "Credenciales inválidas.");
                return;
            }
            
            // Guardar usuario en request para que los servlets lo usen
            req.setAttribute("user", user);
            
        } catch (IllegalArgumentException e) {
            sendError(res, 401, "Credenciales inválidas.");
            return;
        } catch (Exception e) {
            sendError(res, 500, "Error de autenticación.");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":\"error\",\"message\":\"" + message + "\"}");
    }
}