package com.passwordmanager.util;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Base64;

/**
 * Maneja la autenticación stateless via Basic Auth.
 *
 * El frontend envía en cada request:
 *   Authorization: Basic base64(email:password)
 *
 * Este util decodifica el header, busca el usuario en BD
 * y verifica el hash bcrypt.
 */
public class AuthUtil {
    private AuthUtil() {}

    /**
     * Valida las credenciales del header Authorization.
     * Retorna el User si son válidas, null si no.
     */
    public static User authenticate(HttpServletRequest req) {
        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Basic")) return null;

        try {
            // Decodificar base64
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)));
            int colonIndex = decoded.indexOf(':');
            if (colonIndex < 0) return null;

            String email = decoded.substring(0, colonIndex);
            String password = decoded.substring(colonIndex + 1);

            if (email.isBlank() || password.isBlank()) return null;

            // Buscar usuario en BD
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(email);

            if (user == null) return null;

            // Verificar bcrypt
            if (!BCrypt.checkpw(password, user.getPassword())) return null;

            return user;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Hashea una contraseña con bcrypt.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}