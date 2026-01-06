package com.example.schemaregistry.domain.value;

import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Value object representing an MD5 hash of a schema.
 * Used for schema deduplication - schemas with identical content share the same hash.
 */
@Value
public class Md5Hash {
    String value;

    private Md5Hash(String value) {
        if (value == null || value.length() != 32) {
            throw new IllegalArgumentException(
                "MD5 hash must be exactly 32 hexadecimal characters, got: " + value
            );
        }
        if (!value.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException(
                "MD5 hash must contain only lowercase hexadecimal characters, got: " + value
            );
        }
        this.value = value;
    }

    public static Md5Hash of(String value) {
        return new Md5Hash(value);
    }

    /**
     * Compute MD5 hash of a schema string.
     * Uses UTF-8 encoding to match Confluent's implementation.
     */
    public static Md5Hash compute(String schemaText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(schemaText.getBytes(StandardCharsets.UTF_8));
            return of(bytesToHex(hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}