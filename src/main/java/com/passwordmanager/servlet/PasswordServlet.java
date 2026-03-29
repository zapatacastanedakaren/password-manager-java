package com.passwordmanager.servlet;

import com.passwordmanager.dao.PasswordDAO;
import com.passwordmanager.model.Password;
import com.passwordmanager.model.User;
import com.passwordmanager.util.AuthUtil;
import com.passwordmanager.util.CryptoUtil;
import com.passwordmanager.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/**
 * CRUD completo de credenciales.
 * 
 * GET  /api/passwords          → listar (con filtros opcionales ?search=&category=)
 * GET  /api/passwords/{id}     → obtener una
 * GET  /api/passwords/{id}/reveal → descifrar y devolver contraseña
 * POST /api/passwords          → crear
 * PUT  /api/passwords/{id}     → editar
 * DELETE /api/passwords/{id}   → eliminar
 * 
 * Todos los métodos validan autenticación Basic Auth antes de operar.
 */
@WebServlet("/api/passwords/*")
public class PasswordServlet extends HttpServlet {
    private final PasswordDAO passwordDAO = new PasswordDAO();

    // GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = AuthUtil.authenticate(req);
        if (user == null) {
            JsonUtil.error(res, 401, "No autenticado.");
            return;
        }

        String path = req.getPathInfo(); // null, "/", "/{id}", "/{id}/reveal"

        try {
            if (path == null || path.equals("/")) {
                handleList(req, res, user);
            } else {
                String[] segments = path.split("/"); // ["", "id"] o ["", "id", "reveal"]
                
                int id = parseId(segments[1]);
                if (id < 0) {
                    JsonUtil.error(res, 400, "ID inválido.");
                    return;
                }

                if (segments.length == 3 && "reveal".equals(segments[2])) {
                    handleReveal(res, id, user);
                } else {
                    handleGetOne(res, id, user);
                }
            }
        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error en el servidor.");
        }
    }

    // POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = AuthUtil.authenticate(req);
        if (user == null) {
            JsonUtil.error(res, 401, "No autenticado.");
            return;
        }

        try {
            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            String siteName = body.optString("siteName", "").trim();
            String password = body.optString("password", "");

            if (siteName.isBlank() || password.isBlank()) {
                JsonUtil.error(res, 400, "Nombre del sitio y contraseña son obligatorios.");
                return;
            }

            Password record = new Password();
            record.setSiteName(siteName);
            record.setSiteUrl(body.optString("siteUrl", "").trim());
            record.setUsername(body.optString("username", "").trim());
            record.setEncryptedPassword(CryptoUtil.encrypt(password));
            record.setCategory(body.optString("category", "General").trim());
            record.setNotes(body.optString("notes", "").trim());
            record.setUserId(user.getId());

            int newId = passwordDAO.create(record);

            JsonUtil.created(res, new JSONObject()
                .put("message", "Credencial guardada.")
                .put("data", new JSONObject().put("id", newId)));

        } catch(Exception e){
         e.printStackTrace(); // ← Agrega esta línea
          System.err.println("ERROR DETALLADO: " + e.getMessage());
          JsonUtil.error(res, 500, "Error: " + e.getMessage());
        }
    }

    // PUT
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = AuthUtil.authenticate(req);
        if (user == null) {
            JsonUtil.error(res, 401, "No autenticado.");
            return;
        }

        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                JsonUtil.error(res, 400, "ID requerido.");
                return;
            }

            int id = parseId(path.substring(1));
            if (id < 0) {
                JsonUtil.error(res, 400, "ID inválido.");
                return;
            }

            // Verificar que existe y pertenece al usuario
            Password existing = passwordDAO.findByIdAndUser(id, user.getId());
            if (existing == null) {
                JsonUtil.error(res, 404, "Credencial no encontrada.");
                return;
            }

            JSONObject body = new JSONObject(JsonUtil.readBody(req));
            String siteName = body.optString("siteName", "").trim();
            String password = body.optString("password", "");

            // Actualizar solo los campos que llegaron
            existing.setSiteName(!siteName.isBlank() ? siteName : existing.getSiteName());
            existing.setSiteUrl(body.has("siteUrl") 
                ? body.optString("siteUrl").trim() : existing.getSiteUrl());
            existing.setUsername(body.has("username") 
                ? body.optString("username").trim() : existing.getUsername());
            existing.setCategory(body.has("category") 
                ? body.optString("category").trim() : existing.getCategory());
            existing.setNotes(body.has("notes") 
                ? body.optString("notes").trim() : existing.getNotes());

            // Solo re-cifrar si mandaron nueva contraseña
            if (!password.isBlank()) {
                existing.setEncryptedPassword(CryptoUtil.encrypt(password));
            }

            passwordDAO.update(existing);

            JsonUtil.ok(res, "Credencial actualizada.");

        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error en el servidor.");
        }
    }

    // DELETE
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = AuthUtil.authenticate(req);
        if (user == null) {
            JsonUtil.error(res, 401, "No autenticado.");
            return;
        }

        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                JsonUtil.error(res, 400, "ID requerido.");
                return;
            }

            int id = parseId(path.substring(1));
            if (id < 0) {
                JsonUtil.error(res, 400, "ID inválido.");
                return;
            }

            boolean deleted = passwordDAO.delete(id, user.getId());

            if (!deleted) {
                JsonUtil.error(res, 404, "Credencial no encontrada.");
                return;
            }

            JsonUtil.ok(res, "Credencial eliminada.");

        } catch (Exception e) {
            JsonUtil.error(res, 500, "Error en el servidor.");
        }
    }

    // Handlers internos
    private void handleList(HttpServletRequest req, HttpServletResponse res, User user)
            throws Exception {
        String search = req.getParameter("search");
        String category = req.getParameter("category");

        var list = passwordDAO.findAllByUser(user.getId(), search, category);
        var array = new JSONArray();

        for (Password p : list) {
            array.put(new JSONObject()
                .put("id", p.getId())
                .put("siteName", p.getSiteName())
                .put("siteUrl", p.getSiteUrl())
                .put("username", p.getUsername())
                .put("category", p.getCategory())
                .put("notes", p.getNotes())
                .put("createdAt", p.getCreatedAt()));
            // encryptedPassword nunca se incluye en el listado
        }

        JsonUtil.ok(res, new JSONObject().put("data", array));
    }

    private void handleGetOne(HttpServletResponse res, int id, User user)
            throws Exception {
        Password p = passwordDAO.findByIdAndUser(id, user.getId());

        if (p == null) {
            JsonUtil.error(res, 404, "Credencial no encontrada.");
            return;
        }

        JsonUtil.ok(res, new JSONObject()
            .put("data", new JSONObject()
                .put("id", p.getId())
                .put("siteName", p.getSiteName())
                .put("siteUrl", p.getSiteUrl())
                .put("username", p.getUsername())
                .put("category", p.getCategory())
                .put("notes", p.getNotes())
                .put("createdAt", p.getCreatedAt())));
    }

    private void handleReveal(HttpServletResponse res, int id, User user)
            throws Exception {
        Password p = passwordDAO.findByIdAndUser(id, user.getId());

        if (p == null) {
            JsonUtil.error(res, 404, "Credencial no encontrada.");
            return;
        }

        String plainPassword = CryptoUtil.decrypt(p.getEncryptedPassword());

        JsonUtil.ok(res, new JSONObject()
            .put("data", new JSONObject()
                .put("password", plainPassword)));
    }

    // Helper
    /**
     * Parsea un segmento de URL a entero.
     * Retorna -1 si no es un número válido.
     */
    private int parseId(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}