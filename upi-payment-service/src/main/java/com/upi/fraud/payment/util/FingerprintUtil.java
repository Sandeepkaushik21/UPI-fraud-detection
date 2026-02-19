package com.upi.fraud.payment.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 hashing for device fingerprint (hardware "DNA").
 * Used to detect device spoofing when deviceId matches but fingerprint differs.
 */
public final class FingerprintUtil {

    private static final String SHA256 = "SHA-256";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private FingerprintUtil() {}

    public static String sha256Hex(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance(SHA256);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0xF]).append(HEX_CHARS[b & 0xF]);
        }
        return sb.toString();
    }
}
