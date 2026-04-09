package com.passwordmanager.servlet;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import com.passwordmanager.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.Base64;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo();
        
        if (path == null) {
            JsonUtil.error(res, 404, "Ruta no encontrada.");
            return;
        }
        
        switch (path) {
            case "/register":
                handleRegister(req, res);
                break;
            case "/login":
                handleLogin(req, res);
                break;
            default:
                JsonUtil.error(res, 404, "Ruta no encontrada.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo();
        
        if ("/me".equals(path)) {
            handleGetMe(req, res);
            return;
        }
        
        JsonUtil.error(res, 404, "Ruta no encontrada.");
    }

    /**
     * POST /api/auth/register
     * Registra un nuevo usuario con contraseña hasheada (BCrypt)
     */
    private void handleRegister(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            
            String username = body.optString("username", "").trim();
            String email = body.optString("email", "").trim();
            String password = body.optString("password", "");
            String confirmPassword = body.optString("confirmPassword", "");
            
            // Validaciones
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                JsonUtil.error(res, 400, "Todos los campos son obligatorios.");
                return;
            }
            
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                JsonUtil.error(res, 400, "Email inválido.");
                return;
            }
            
            if (password.length() < 8) {
                JsonUtil.error(res, 400, "Contraseña debe tener al menos 8 caracteres.");
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JsonUtil.error(res, 400, "Las contraseñas no coinciden.");
                return;
            }
            
            // Verificar si el email ya existe
            if (userDAO.emailExists(email)) {
                JsonUtil.error(res, 409, "El email ya está registrado.");
                return;
            }
            
            // ✅ HASHEAR CONTRASEÑA CON BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Crear usuario
            int userId = userDAO.create(username, email, hashedPassword);
            
            JsonUtil.created(res, new JSONObject()
                .put("id", userId)
                .put("message", "Usuario registrado exitosamente."));
            
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.error(res, 500, "Error al registrar usuario.");
        }
    }

    /**
     * POST /api/auth/login
     * Valida credenciales con BCrypt (soporta hash y texto plano)
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            
            String email = body.optString("email", "").trim();
            String password = body.optString("password", "");
            
            System.out.println("🔍 [LOGIN] Email recibido: " + email);
            System.out.println("🔍 [LOGIN] Password recibida: [" + password + "]");
            
            if (email.isBlank() || password.isBlank()) {
                JsonUtil.error(res, 400, "Email y contraseña son obligatorios.");
                return;
            }
            
            User user = userDAO.findByEmail(email);
            
            System.out.println("🔍 [LOGIN] Usuario encontrado: " + (user != null));
            
            if (user == null) {
                JsonUtil.error(res, 401, "Credenciales incorrectas.");
                return;
            }
            
            String dbPassword = user.getPassword();
            System.out.println("🔍 [LOGIN] Password en BD: " + dbPassword);
            System.out.println("🔍 [LOGIN] Empieza con $2a$?: " + dbPassword.startsWith("$2a$"));
            
            boolean valid;
            
            if (dbPassword.startsWith("$2a$")) {
                System.out.println("🔍 [LOGIN] Usando BCrypt.checkpw...");
                valid = BCrypt.checkpw(password, dbPassword);
                System.out.println("🔍 [LOGIN] Resultado BCrypt: " + valid);
            } else {
                System.out.println("🔍 [LOGIN] Usando equals (texto plano)...");
                valid = password.equals(dbPassword);
                System.out.println("🔍 [LOGIN] Resultado equals: " + valid);
            }
            
            if (!valid) {
                System.out.println("❌ [LOGIN] Credenciales incorrectas - valid=false");
                JsonUtil.error(res, 401, "Credenciales incorrectas.");
                return;
            }
            
            System.out.println("✅ [LOGIN] Login exitoso para: " + email);
            
            JSONObject userData = new JSONObject()
                .put("id", user.getId())
                .put("username", user.getUsername())
                .put("email", user.getEmail());
            
            JsonUtil.ok(res, new JSONObject()
                .put("user", userData)
                .put("message", "Login exitoso."));
            
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.error(res, 500, "Error al iniciar sesión.");
        }
    }

    /**
     * GET /api/auth/me
     * Obtiene el usuario actual desde Basic Auth header
     */
    private void handleGetMe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            User user = authenticate(req);
            
            if (user == null) {
                JsonUtil.error(res, 401, "No autenticado.");
                return;
            }
            
            JSONObject userData = new JSONObject()
                .put("id", user.getId())
                .put("username", user.getUsername())
                .put("email", user.getEmail());
            
            JsonUtil.ok(res, new JSONObject().put("user", userData));
            
        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error al obtener usuario.");
        }
    }

    /**
     * Autentica usuario desde Basic Auth header
     */
    private User authenticate(HttpServletRequest req) {
        try {
            String authHeader = req.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return null;
            }
            
            String base64 = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(base64));
            String[] parts = decoded.split(":", 2);
            
            if (parts.length != 2) {
                return null;
            }
            
            String email = parts[0].trim();
            String password = parts[1];
            
            User user = userDAO.findByEmail(email);
            
            if (user == null) {
                return null;
            }
            
            // ✅ ACEPTA TANTO BCrypt COMO TEXTO PLANO
            String dbPassword = user.getPassword();
            boolean valid;
            
            if (dbPassword.startsWith("$2a$")) {
                valid = BCrypt.checkpw(password, dbPassword);
            } else {
                valid = password.equals(dbPassword);
            }
            
            if (!valid) {
                return null;
            }
            
            return user;
            
        } catch (Exception e) {
            return null;
        }
    }
}