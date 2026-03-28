package com.passwordmanager.model;

/**
 * Representa una fila de la tabla passwords.
 * encryptedPassword nunca se envía al frontend en texto plano.
 */
public class Password {
    private int id;
    private String siteName;
    private String siteUrl;
    private String username;
    private String encryptedPassword;
    private String category;
    private String notes;
    private int userId;
    private String createdAt;

    public Password() {}

    public int getId() { return id; }
    public String getSiteName() { return siteName; }
    public String getSiteUrl() { return siteUrl; }
    public String getUsername() { return username; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public String getCategory() { return category; }
    public String getNotes() { return notes; }
    public int getUserId() { return userId; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setSiteName(String s) { this.siteName = s; }
    public void setSiteUrl(String s) { this.siteUrl = s; }
    public void setUsername(String u) { this.username = u; }
    public void setEncryptedPassword(String p) { this.encryptedPassword = p; }
    public void setCategory(String c) { this.category = c; }
    public void setNotes(String n) { this.notes = n; }
    public void setUserId(int uid) { this.userId = uid; }
    public void setCreatedAt(String c) { this.createdAt = c; }
}