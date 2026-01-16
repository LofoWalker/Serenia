package com.lofo.serenia.service.chat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * HKDF (HMAC-based Key Derivation Function) implementation per RFC 5869.
 * Used to derive unique encryption keys for each user from a master key.
 */
public final class HkdfUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int HASH_LENGTH = 32;

    private HkdfUtils() {
    }

    /**
     * Derives a key using HKDF-SHA256.
     *
     * @param ikm    Input Key Material (master key bytes)
     * @param salt   Salt value (user ID bytes)
     * @param info   Context and application specific information
     * @param length Desired output key length in bytes
     * @return Derived key bytes
     */
    public static byte[] derive(byte[] ikm, byte[] salt, byte[] info, int length) {
        if (length > 255 * HASH_LENGTH) {
            throw new IllegalArgumentException("Output length exceeds maximum allowed");
        }

        byte[] prk = hmacSha256(salt, ikm);

        return expand(prk, info, length);
    }

    /**
     * Convenience method to derive a user-specific key from master key and user ID.
     *
     * @param masterKeyBytes Master key bytes (AES-256 = 32 bytes)
     * @param userId         User UUID
     * @param context        Application context string
     * @return 32-byte derived key suitable for AES-256
     */
    public static byte[] deriveUserKey(byte[] masterKeyBytes, UUID userId, String context) {
        byte[] salt = uuidToBytes(userId);
        byte[] info = context.getBytes(StandardCharsets.UTF_8);
        return derive(masterKeyBytes, salt, info, 32);
    }

    private static byte[] expand(byte[] prk, byte[] info, int length) {
        int iterations = (int) Math.ceil((double) length / HASH_LENGTH);
        byte[] okm = new byte[length];
        byte[] previousBlock = new byte[0];

        int offset = 0;
        for (int i = 1; i <= iterations; i++) {
            byte[] input = ByteBuffer.allocate(previousBlock.length + info.length + 1)
                    .put(previousBlock)
                    .put(info)
                    .put((byte) i)
                    .array();

            previousBlock = hmacSha256(prk, input);

            int copyLength = Math.min(HASH_LENGTH, length - offset);
            System.arraycopy(previousBlock, 0, okm, offset, copyLength);
            offset += copyLength;
        }

        return okm;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
