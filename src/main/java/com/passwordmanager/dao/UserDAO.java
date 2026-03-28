package com.passwordmanager.dao;

import com.passwordmanager.db.DatabasePool;
import com.passwordmanager.model.User;

import java.sql.*;

/**
 * Acceso a datos de la tabla users.
 * Cada método abre una conexión del pool, ejecuta el query y la devuelve.
 */
public class UserDAO {

    /**
     * Busca un usuario por email.
     * Retorna null si no existe.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, password FROM users WHERE email=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * Busca un usuario por id.
     * Retorna null si no existe.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, username, email, password FROM users WHERE id=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * Inserta un nuevo usuario.
     * Retorna el id generado por MySQL.
     */
    public int create(String username, String email, String hashedPassword) throws SQLException {
        String sql = "INSERT INTO users(username, email, password) VALUES(?,?,?)";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el id del usuario creado.");
    }

    /**
     * Verifica si un email ya está registrado.
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}