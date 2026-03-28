package com.passwordmanager.dao;

import com.passwordmanager.db.DatabasePool;
import com.passwordmanager.model.Password;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla passwords.
 * Todos los métodos filtran siempre por userId — nunca devuelven
 * datos de otro usuario.
 */
public class PasswordDAO {

    /**
     * Lista todas las credenciales de un usuario.
     * Acepta filtros opcionales (pueden ser null o vacíos).
     */
    public List<Password> findAllByUser(int userId, String search, String category) throws SQLException {
        // Construir query dinámico según filtros presentes
        StringBuilder sql = new StringBuilder(
            "SELECT id, site_name, site_url, username, category, notes, created_at " +
            "FROM passwords WHERE user_id=?"
        );

        if (search != null && !search.isBlank()) sql.append(" AND site_name LIKE ?");
        if (category != null && !category.isBlank()) sql.append(" AND category=?");
        sql.append(" ORDER BY site_name ASC");

        List<Password> list = new ArrayList<>();

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            stmt.setInt(idx++, userId);
            if (search != null && !search.isBlank()) stmt.setString(idx++, "%" + search + "%");
            if (category != null && !category.isBlank()) stmt.setString(idx, category);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Password p = new Password();
                    p.setId(rs.getInt("id"));
                    p.setSiteName(rs.getString("site_name"));
                    p.setSiteUrl(rs.getString("site_url"));
                    p.setUsername(rs.getString("username"));
                    p.setCategory(rs.getString("category"));
                    p.setNotes(rs.getString("notes"));
                    p.setCreatedAt(rs.getString("created_at"));
                    p.setUserId(userId);
                    list.add(p);
                }
            }
        }
        return list;
    }

    /**
     * Busca una credencial por id.
     * Incluye encryptedPassword — solo usar cuando se necesita descifrar.
     * Retorna null si no existe o no pertenece al usuario.
     */
    public Password findByIdAndUser(int id, int userId) throws SQLException {
        String sql = "SELECT id, site_name, site_url, username, encrypted_password, " +
                     "category, notes, created_at FROM passwords WHERE id=? AND user_id=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Password p = new Password();
                    p.setId(rs.getInt("id"));
                    p.setSiteName(rs.getString("site_name"));
                    p.setSiteUrl(rs.getString("site_url"));
                    p.setUsername(rs.getString("username"));
                    p.setEncryptedPassword(rs.getString("encrypted_password"));
                    p.setCategory(rs.getString("category"));
                    p.setNotes(rs.getString("notes"));
                    p.setCreatedAt(rs.getString("created_at"));
                    p.setUserId(userId);
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Inserta una nueva credencial.
     * Retorna el id generado.
     */
    public int create(Password p) throws SQLException {
        String sql = "INSERT INTO passwords(site_name, site_url, username, encrypted_password, category, notes, user_id) " +
                     "VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, p.getSiteName());
            stmt.setString(2, p.getSiteUrl());
            stmt.setString(3, p.getUsername());
            stmt.setString(4, p.getEncryptedPassword());
            stmt.setString(5, p.getCategory() != null ? p.getCategory() : "General");
            stmt.setString(6, p.getNotes());
            stmt.setInt(7, p.getUserId());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el id de la credencial creada.");
    }

    /**
     * Actualiza una credencial existente.
     * Solo actualiza si pertenece al usuario — ownership garantizado en SQL.
     */
    public boolean update(Password p) throws SQLException {
        String sql = "UPDATE passwords SET site_name=?, site_url=?, username=?, " +
                     "encrypted_password=?, category=?, notes=? WHERE id=? AND user_id=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getSiteName());
            stmt.setString(2, p.getSiteUrl());
            stmt.setString(3, p.getUsername());
            stmt.setString(4, p.getEncryptedPassword());
            stmt.setString(5, p.getCategory());
            stmt.setString(6, p.getNotes());
            stmt.setInt(7, p.getId());
            stmt.setInt(8, p.getUserId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Elimina una credencial.
     * El AND user_id impide borrar registros ajenos.
     */
    public boolean delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM passwords WHERE id=? AND user_id=?";

        try (Connection conn = DatabasePool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }
}