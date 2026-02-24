package com.vedvix.syncledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for storing sensitive credentials at rest.
 * Used to encrypt Microsoft client secrets, API keys, etc.
 * 
 * Industry standard: AES-256-GCM provides both confidentiality and authenticity.
 * The IV is prepended to the ciphertext for proper decryption.
 * 
 * @author vedvix
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(
            @Value("${encryption.master-key:#{null}}") String masterKeyBase64) {
        
        if (masterKeyBase64 == null || masterKeyBase64.isBlank() || masterKeyBase64.equals("change-me-in-production")) {
            log.warn("⚠️  Using default encryption key. Set 'encryption.master-key' in production!");
            // Generate a deterministic key for development only
            byte[] defaultKey = new byte[32];
            new SecureRandom(new byte[]{1}).nextBytes(defaultKey);
            this.secretKey = new SecretKeySpec(defaultKey, "AES");
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("Encryption master key must be 256 bits (32 bytes) Base64-encoded");
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt a plaintext string using AES-256-GCM.
     * Returns Base64-encoded ciphertext with prepended IV.
     * 
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted string (IV + ciphertext + tag)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt AES-256-GCM encrypted string.
     * 
     * @param encryptedBase64 Base64-encoded encrypted string (IV + ciphertext + tag)
     * @return Decrypted plaintext string
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return null;
        }

        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // Extract ciphertext
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Mask a secret for display (e.g., "abc***xyz").
     */
    public static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 6) {
            return "****";
        }
        return secret.substring(0, 3) + "***" + secret.substring(secret.length() - 3);
    }
}
