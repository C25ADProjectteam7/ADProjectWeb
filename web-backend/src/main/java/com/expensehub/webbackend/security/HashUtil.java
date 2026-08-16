package com.expensehub.webbackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared utility for computing SHA-256 hex digests.
 * Used for email hash lookups (the email column itself is AES-GCM encrypted).
 */
public final class HashUtil {

    private HashUtil() {
        // utility class
    }

    /**
     * Returns the lowercase SHA-256 hex digest of the given string.
     */
    public static String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}