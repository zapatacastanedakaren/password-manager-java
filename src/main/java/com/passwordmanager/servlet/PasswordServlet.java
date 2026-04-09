package com.passwordmanager.servlet;

import com.passwordmanager.dao.PasswordDAO;
import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.Password;
import com.passwordmanager.model.User;
import com.passwordmanager.util.CryptoUtil;
import com.passwordmanager.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

@WebServlet("/api/passwords/*")
public class PasswordServlet extends HttpServlet {

    private final PasswordDAO passwordDAO = new PasswordDAO();
    private final UserDAO userDAO = new UserDAO();

    // ✅ Autenticación con soporte BCrypt + texto plano
    private User authenticate(HttpServletRequest req) {
        try {
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) return null;
            
            String base64 = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(base64));
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) return null;
            
            String email = parts[0].trim();
            String password = parts[1];
            
            User user = userDAO.findByEmail(email);
            if (user == null) return null;
            
            String dbPassword = user.getPassword();
            boolean valid;
            
            if (dbPassword.startsWith("$2a$")) {
                valid = BCrypt.checkpw(password, dbPassword);
            } else {
                valid = password.equals(dbPassword);
            }
            
            return valid ? user : null;
            
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = authenticate(req);
        if (user == null) { JsonUtil.error(res, 401, "No autenticado."); return; }
        
        String path = req.getPathInfo();
        
        if (path == null || path.equals("/") || path.isEmpty()) {
            try {
                List<Password> list = passwordDAO.findAllByUser(user.getId(), null, null);
                JSONArray arr = new JSONArray();
                for (Password p : list) { arr.put(toJson(p, false)); }
                JsonUtil.ok(res, new JSONObject().put("data", arr));
            } catch (SQLException e) { JsonUtil.error(res, 500, "Error DB"); }
            catch (Exception e) { JsonUtil.error(res, 500, "Error"); }
            return;
        }
        
        if (path != null && path.endsWith("/reveal")) {
            try {
                int id = Integer.parseInt(path.replace("/reveal", "").replace("/", "").trim());
                Password p = passwordDAO.findByIdAndUser(id, user.getId());
                if (p == null) { JsonUtil.error(res, 404, "No encontrada."); return; }
                
                // ✅ DESENCRIPTAR con AES-256-GCM antes de revelar
                String decryptedPassword = CryptoUtil.decrypt(p.getEncryptedPassword());
                JsonUtil.ok(res, new JSONObject().put("password", decryptedPassword));
            } catch (Exception e) { 
                e.printStackTrace();
                JsonUtil.error(res, 400, "ID inválido o error al descifrar."); 
            }
            return;
        }
        
        if (path != null) {
            try {
                int id = Integer.parseInt(path.replace("/", "").trim());
                Password p = passwordDAO.findByIdAndUser(id, user.getId());
                if (p == null) { JsonUtil.error(res, 404, "No encontrada."); return; }
                JsonUtil.ok(res, new JSONObject().put("data", toJson(p, false)));
            } catch (Exception e) { JsonUtil.error(res, 400, "ID inválido."); }
            return;
        }
        JsonUtil.error(res, 404, "Ruta no encontrada.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = authenticate(req);
        if (user == null) { JsonUtil.error(res, 401, "No autenticado."); return; }
        try {
            JSONObject b = new JSONObject(JsonUtil.readBody(req));
            Password p = new Password();
            p.setUserId(user.getId());
            p.setSiteName(b.optString("siteName", "").trim());
            p.setSiteUrl(b.optString("siteUrl", "").trim());
            p.setUsername(b.optString("username", "").trim());
            p.setCategory(b.optString("category", "General").trim());
            p.setNotes(b.optString("notes", "").trim());
            String plain = b.optString("password", "");
            
            if (p.getSiteName().isEmpty() || plain.isEmpty()) { 
                JsonUtil.error(res, 400, "Faltan campos."); 
                return; 
            }
            
            // ✅ ENCRIPTAR con AES-256-GCM antes de guardar
            p.setEncryptedPassword(CryptoUtil.encrypt(plain));
            
            int id = passwordDAO.create(p);
            JsonUtil.created(res, new JSONObject().put("id", id).put("message", "Creada."));
        } catch (SQLException e) { JsonUtil.error(res, 500, "Error DB"); }
        catch (Exception e) { 
            e.printStackTrace();
            JsonUtil.error(res, 500, "Error: " + e.getMessage()); 
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = authenticate(req);
        if (user == null) { JsonUtil.error(res, 401, "No autenticado."); return; }
        String path = req.getPathInfo();
        if (path == null) { JsonUtil.error(res, 400, "ID requerido."); return; }
        try {
            int id = Integer.parseInt(path.replace("/", "").trim());
            Password ex = passwordDAO.findByIdAndUser(id, user.getId());
            if (ex == null) { JsonUtil.error(res, 404, "No encontrada."); return; }
            
            JSONObject b = new JSONObject(JsonUtil.readBody(req));
            ex.setSiteName(b.optString("siteName", ex.getSiteName()).trim());
            ex.setSiteUrl(b.optString("siteUrl", ex.getSiteUrl()).trim());
            ex.setUsername(b.optString("username", ex.getUsername()).trim());
            ex.setCategory(b.optString("category", ex.getCategory()).trim());
            ex.setNotes(b.optString("notes", ex.getNotes()).trim());
            
            // ✅ Si hay nueva contraseña, encriptarla con AES-256-GCM
            if (b.has("password") && !b.getString("password").isEmpty()) {
                ex.setEncryptedPassword(CryptoUtil.encrypt(b.getString("password")));
            }
            
            passwordDAO.update(ex);
            JsonUtil.ok(res, new JSONObject().put("message", "Actualizada."));
        } catch (SQLException e) { JsonUtil.error(res, 500, "Error DB"); }
        catch (Exception e) { JsonUtil.error(res, 500, "Error: " + e.getMessage()); }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = authenticate(req);
        if (user == null) { JsonUtil.error(res, 401, "No autenticado."); return; }
        String path = req.getPathInfo();
        if (path == null) { JsonUtil.error(res, 400, "ID requerido."); return; }
        try {
            int id = Integer.parseInt(path.replace("/", "").trim());
            boolean ok = passwordDAO.delete(id, user.getId());
            if (!ok) { JsonUtil.error(res, 404, "No encontrada."); return; }
            JsonUtil.ok(res, new JSONObject().put("message", "Eliminada."));
        } catch (SQLException e) { JsonUtil.error(res, 500, "Error DB"); }
        catch (Exception e) { JsonUtil.error(res, 500, "Error: " + e.getMessage()); }
    }

    private JSONObject toJson(Password p, boolean reveal) {
        JSONObject j = new JSONObject()
            .put("id", p.getId())
            .put("siteName", p.getSiteName())
            .put("siteUrl", p.getSiteUrl())
            .put("username", p.getUsername())
            .put("category", p.getCategory())
            .put("notes", p.getNotes())
            .put("createdAt", p.getCreatedAt());
        if (reveal) { j.put("password", p.getEncryptedPassword()); }
        return j;
    }
}