package com.passwordmanager.servlet;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import com.passwordmanager.util.AuthUtil;
import com.passwordmanager.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Maneja registro y login de usuarios.
 *
 * POST /api/auth/register → crea un nuevo usuario
 * POST /api/auth/login → valida credenciales y devuelve datos del usuario
 * GET  /api/auth/me → verifica que las credenciales del header son válidas
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo(); // /register o /login

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

    // /register
    private void handleRegister(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));

            String username = body.optString("username", "").trim();
            String email = body.optString("email", "").trim();
            String password = body.optString("password", "");
            String confirmPassword = body.optString("confirmPassword", "");

            // Validaciones básicas
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                JsonUtil.error(res, 400, "Todos los campos son obligatorios.");
                return;
            }

            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                JsonUtil.error(res, 400, "Email inválido.");
                return;
            }

            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
                JsonUtil.error(res, 400, "Contraseña débil. Mínimo 8 caracteres, mayúscula, minúscula y número.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                JsonUtil.error(res, 400, "Las contraseñas no coinciden.");
                return;
            }

            if (!username.matches("^[a-zA-Z0-9_]{3,25}$")) {
                JsonUtil.error(res, 400, "Usuario inválido. 3-25 caracteres, letras, números y guiones bajos.");
                return;
            }

            if (userDAO.emailExists(email)) {
                JsonUtil.error(res, 400, "El email ya está registrado.");
                return;
            }

            String hashedPassword = AuthUtil.hashPassword(password);
            int newId = userDAO.create(username, email, hashedPassword);

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

    // /login
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

            // Mismo mensaje para email y password incorrectos — no revelar cuál falló
            if (user == null || !org.mindrot.jbcrypt.BCrypt.checkpw(pass, user.getPassword())) {
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

    // /me
    private void handleMe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = AuthUtil.authenticate(req);

        if (user == null) {
            JsonUtil.error(res, 401, "No autenticado.");
            return;
        }

        JsonUtil.ok(res, new JSONObject()
            .put("user", new JSONObject()
                .put("id", user.getId())
                .put("username", user.getUsername())
                .put("email", user.getEmail())));
    }
}