CREATE DATABASE IF NOT EXISTS password_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE password_manager;

CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS passwords (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    site_name            VARCHAR(100) NOT NULL,
    site_url             VARCHAR(255),
    username             VARCHAR(100),
    encrypted_password   TEXT NOT NULL,
    category             VARCHAR(50) DEFAULT 'General',
    notes                TEXT,
    user_id              INT NOT NULL,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);