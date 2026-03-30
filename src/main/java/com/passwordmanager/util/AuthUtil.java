package com.passwordmanager.util;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Base64;

public class AuthUtil {
    
    /**
     * Autentica al usuario usando Basic Auth header.
     * Devuelve el User si las credenciales son válidas, null si no.
     */
    public static User authenticate(HttpServletRequest req) {
        try {
            // 1. Obtener header Authorization
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return null;
            }
            
            // 2. Decodificar credenciales Base64
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            
            if (parts.length != 2) {
                return null;
            }
            
            String email = parts[0];
            String password = parts[1];  // ← contraseña en texto plano
            
            // 3. Buscar usuario en BD
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(email);
            
            if (user == null) {
                return null;
            }
            
            // ✅ CRÍTICO: Usar BCrypt.checkpw para comparar contraseña con hash
            // Esto es lo que probablemente faltaba en tu código anterior
            if (!BCrypt.checkpw(password, user.getPassword())) {
                return null;
            }
            
            return user;
            
        } catch (Exception e) {
            // Cualquier error = no autenticado
            return null;
        }
    }
    
    /**
     * Hashea una contraseña usando BCrypt.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}