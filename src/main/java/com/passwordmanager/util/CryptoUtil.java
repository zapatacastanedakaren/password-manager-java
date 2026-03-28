package com.passwordmanager.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado AES-256-GCM para las contraseñas almacenadas.
 *
 * Formato del texto cifrado: base64(iv) + ":" + base64(encrypted)
 * El GCM incluye el authTag al final del encrypted — no se maneja por separado.
 *
 * La clave se lee de la variable de entorno ENCRYPTION_KEY (32 bytes en base64).
 * Generar con: openssl rand -base64 32
 */
public class CryptoUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // bytes
    private static final int TAG_LENGTH = 128; // bits

    private static final SecretKey SECRET_KEY;

    static {
        String keyB64 = System.getenv("ENCRYPTION_KEY");
        if (keyB64 == null || keyB64.isBlank()) {
            throw new RuntimeException("Variable de entorno ENCRYPTION_KEY no definida.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyB64);
        if (keyBytes.length != 32) {
            throw new RuntimeException("ENCRYPTION_KEY debe ser de 32 bytes (base64 de 'openssl rand -base64 32').");
        }
        SECRET_KEY = new SecretKeySpec(keyBytes, "AES");
    }

    private CryptoUtil() {}

    /**
     * Cifra un texto plano.
     * @return "base64(iv):base64(encrypted+authTag)"
     */
    public static String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(iv) + ":" +
               Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Descifra un texto cifrado en formato "base64(iv):base64(encrypted+authTag)".
     * @return texto plano original
     */
    public static String decrypt(String encryptedData) throws Exception {
        String[] parts = encryptedData.split(":");
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encrypted = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_LENGTH, iv));

        return new String(cipher.doFinal(encrypted), "UTF-8");
    }
}