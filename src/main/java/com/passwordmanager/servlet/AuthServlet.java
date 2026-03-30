package com.passwordmanager.servlet;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import com.passwordmanager.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo();
        if ("/register".equals(path)) {
            handleRegister(req, res);
        } else if ("/login".equals(path)) {
            handleLogin(req, res);
        } else {
            JsonUtil.error(res, 404, "Ruta no encontrada.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo();
        if ("/me".equals(path)) {
            handleMe(req, res);
        } else {
            JsonUtil.error(res, 404, "Ruta no encontrada.");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            String username = body.optString("username", "").trim();
            String email = body.optString("email", "").trim();
            String password = body.optString("password", "");
            String confirmPassword = body.optString("confirmPassword", "");

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                JsonUtil.error(res, 400, "Todos los campos son obligatorios.");
                return;
            }
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                JsonUtil.error(res, 400, "Email inválido.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JsonUtil.error(res, 400, "Las contraseñas no coinciden.");
                return;
            }
            if (userDAO.emailExists(email)) {
                JsonUtil.error(res, 400, "El email ya está registrado.");
                return;
            }

            // ⚠️ DEV ONLY: Guardar password en texto plano (NO usar en producción)
            int newId = userDAO.create(username, email, password);

            JsonUtil.created(res, new JSONObject()
                .put("message", "Usuario registrado exitosamente.")
                .put("user", new JSONObject()
                    .put("id", newId)
                    .put("username", username)
                    .put("email", email)));

        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error en el servidor.");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            String email = body.optString("email", "").trim();
            String pass = body.optString("password", "");

            if (email.isBlank() || pass.isBlank()) {
                JsonUtil.error(res, 400, "Email y contraseña son obligatorios.");
                return;
            }

            User user = userDAO.findByEmail(email);
            
            // ⚠️ DEV ONLY: Comparación simple (NO usar en producción)
            if (user == null || !pass.equals(user.getPassword())) {
                JsonUtil.error(res, 401, "Credenciales incorrectas.");
                return;
            }

            JsonUtil.ok(res, new JSONObject()
                .put("message", "Login exitoso.")
                .put("user", new JSONObject()
                    .put("id", user.getId())
                    .put("username", user.getUsername())
                    .put("email", user.getEmail())));

        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error en el servidor.");
        }
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                JsonUtil.error(res, 401, "No autenticado.");
                return;
            }

            String base64 = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(base64));
            String[] parts = decoded.split(":", 2);
            
            if (parts.length != 2) {
                JsonUtil.error(res, 401, "Credenciales inválidas.");
                return;
            }
            
            String email = parts[0].trim();
            String password = parts[1];
            
            User user = userDAO.findByEmail(email);
            
            // ⚠️ DEV ONLY: Misma validación simple que handleLogin
            if (user == null || !password.equals(user.getPassword())) {
                JsonUtil.error(res, 401, "Credenciales inválidas.");
                return;
            }
            
            JsonUtil.ok(res, new JSONObject()
                .put("user", new JSONObject()
                    .put("id", user.getId())
                    .put("username", user.getUsername())
                    .put("email", user.getEmail())));
                    
        } catch (Exception e) {
            JsonUtil.error(res, 401, "Error de autenticación.");
        }
    }
}