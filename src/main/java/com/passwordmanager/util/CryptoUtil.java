package com.passwordmanager.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
    
    // 🔑 Clave de 32 bytes (256 bits) para AES-256
    // En producción: usar variables de entorno o KeyStore
    private static final String SECRET_KEY = "0123456789abcdef0123456789abcdef";
    
    // Parámetros para GCM
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // 96 bits
    private static final int TAG_LENGTH = 16; // 128 bits

    /**
     * Encripta un texto usando AES-256-GCM
     * @param plainText Texto en claro
     * @return Texto encriptado en Base64 (IV + ciphertext)
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        
        // Generar IV aleatorio
        byte[] iv = new byte[IV_LENGTH];
        new java.security.SecureRandom().nextBytes(iv);
        
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        
        // Combinar IV + ciphertext
        byte[] combined = new byte[IV_LENGTH + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(cipherText, 0, combined, IV_LENGTH, cipherText.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Desencripta un texto usando AES-256-GCM
     * @param encryptedText Texto encriptado en Base64
     * @return Texto original en claro
     */
    public static String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        
        // Separar IV y ciphertext
        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherText = new byte[combined.length - IV_LENGTH];
        
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }
}