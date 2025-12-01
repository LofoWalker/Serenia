package com.lofo.serenia.service.encryption.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.EncryptionException;
import com.lofo.serenia.service.encryption.EncryptionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class EncryptionServiceImpl implements EncryptionService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public EncryptionServiceImpl(SereniaConfig sereniaConfig) {
        Objects.requireNonNull(sereniaConfig, "sereniaConfig must not be null");
        this.masterKey = initMasterKey(sereniaConfig.securityKey());
    }

    @Override
    public void createUserKeyIfAbsent(UUID userId) {
        // Clé unique globale : aucune action nécessaire, on garde la méthode pour compatibilité API
        Objects.requireNonNull(userId, "userId must not be null");
    }

    @Override
    public byte[] encryptForUser(UUID userId, String plaintext) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(plaintext, "plaintext must not be null");

        SecretKey key = getMasterKey();

        try {
            byte[] iv = generateIv();
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return encodePayload(iv, ciphertext);

        } catch (Exception e) {
            throw new EncryptionException("Encryption failed for user " + userId, e);
        }
    }

    @Override
    public String decryptForUser(UUID userId, byte[] encryptedBytes) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(encryptedBytes, "encryptedBytes must not be null");

        if (encryptedBytes.length <= GCM_IV_LENGTH_BYTES) {
            throw new EncryptionException("Invalid encrypted data: too short to contain IV");
        }

        SecretKey key = getMasterKey();

        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key, iv);
            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed for user " + userId, e);
        }
    }

    private SecretKey getMasterKey() {
        if (masterKey == null) {
            throw new EncryptionException("Master encryption key is not initialized");
        }
        return masterKey;
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private Cipher createCipher(int mode, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(mode, key, spec);
            return cipher;
        } catch (Exception e) {
            throw new EncryptionException("Failed to initialize cipher", e);
        }
    }

    private byte[] encodePayload(byte[] iv, byte[] ciphertext) {
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private SecretKey initMasterKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new EncryptionException("Configuration property 'serenia.security.key' must not be null or blank");
        }

        String trimmed = configuredKey.trim();
        byte[] keyBytes;
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                // Hex string without the 0x prefix
                keyBytes = hexStringToBytes(trimmed.substring(2));
            } else {
                // Assume Base64 by default
                keyBytes = java.util.Base64.getDecoder().decode(trimmed);
            }
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Invalid format for 'serenia.security.key' (expected Base64 or 0x-prefixed hex)", e);
        }

        int length = keyBytes.length;
        if (length != 16 && length != 24 && length != 32) {
            throw new EncryptionException("Invalid length for 'serenia.security.key': expected 16, 24 or 32 bytes but got " + length);
        }

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private byte[] hexStringToBytes(String hex) {
        String clean = hex.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        int len = clean.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(clean.charAt(i), 16);
            int lo = Character.digit(clean.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character in security key");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
