package com.passwordmanager.model;

/**
 * Representa una fila de la tabla users.
 * Sin lógica de negocio — solo transporta datos entre capas.
 */
public class User {
    private int id;
    private String username;
    private String email;
    private String password; // siempre hasheada con bcrypt

    public User() {}

    public User(int id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String u) { this.username = u; }
    public void setEmail(String e) { this.email = e; }
    public void setPassword(String p) { this.password = p; }
}