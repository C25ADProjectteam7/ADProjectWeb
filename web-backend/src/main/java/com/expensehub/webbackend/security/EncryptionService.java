package com.expensehub.webbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption service.
 * <p>
 * Each encryption generates a random 12-byte IV, which is prepended to the
 * ciphertext before Base64-encoding, making each encrypted value self-contained
 * and non-deterministic (identical plaintexts produce different ciphertexts).
 */
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits
    private static final int GCM_TAG_LENGTH = 128;  // bits

    private final SecretKey key;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.encryption.secret}") String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Encryption key must be 32 bytes (256 bits), Base64-encoded");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts plaintext. Returns Base64-encoded ciphertext with a random IV
     * prepended (IV length = 12 bytes). Format: Base64(IV + ciphertext + GCM tag).
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded payload produced by {@link #encrypt(String)}.
     */
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            int ciphertextLen = combined.length - GCM_IV_LENGTH;
            byte[] ciphertext = new byte[ciphertextLen];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertextLen);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}