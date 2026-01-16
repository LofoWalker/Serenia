package com.lofo.serenia.service.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HkdfUtils Tests")
class HkdfUtilsTest {

    private static final byte[] MASTER_KEY = Base64.getDecoder()
            .decode("qNJYJqH7CdLF0X3R5wZ+bVmK9pL2mN3oQ4rS6tU7vW8=");

    @Nested
    @DisplayName("Key Derivation Consistency Tests")
    class KeyDerivationConsistencyTests {

        @Test
        @DisplayName("Should derive consistent key for same user")
        void shouldDeriveConsistentKeyForSameUser() {
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");
            byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");

            assertArrayEquals(key1, key2, "Same inputs should produce same output");
        }

        @Test
        @DisplayName("Should derive different keys for different users")
        void shouldDeriveDifferentKeysForDifferentUsers() {
            UUID user1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID user2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

            byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, user1, "test-context");
            byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, user2, "test-context");

            assertFalse(Arrays.equals(key1, key2), "Different users should have different keys");
        }

        @Test
        @DisplayName("Should derive 32-byte key for AES-256")
        void shouldDerive32ByteKey() {
            UUID userId = UUID.randomUUID();

            byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");

            assertEquals(32, key.length, "Key should be 32 bytes for AES-256");
        }

        @Test
        @DisplayName("Should derive different keys for different contexts")
        void shouldDeriveDifferentKeysForDifferentContexts() {
            UUID userId = UUID.randomUUID();

            byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "context-v1");
            byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "context-v2");

            assertFalse(Arrays.equals(key1, key2), "Different contexts should produce different keys");
        }

        @Test
        @DisplayName("Should derive different keys for different master keys")
        void shouldDeriveDifferentKeysForDifferentMasterKeys() {
            UUID userId = UUID.randomUUID();
            byte[] masterKey2 = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

            byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");
            byte[] key2 = HkdfUtils.deriveUserKey(masterKey2, userId, "test-context");

            assertFalse(Arrays.equals(key1, key2), "Different master keys should produce different derived keys");
        }
    }

    @Nested
    @DisplayName("HKDF Derive Function Tests")
    class HkdfDeriveFunctionTests {

        @Test
        @DisplayName("Should derive key with arbitrary output length")
        void shouldDeriveKeyWithArbitraryOutputLength() {
            byte[] ikm = MASTER_KEY;
            byte[] salt = new byte[16];
            byte[] info = "test".getBytes();

            byte[] key16 = HkdfUtils.derive(ikm, salt, info, 16);
            byte[] key64 = HkdfUtils.derive(ikm, salt, info, 64);
            byte[] key128 = HkdfUtils.derive(ikm, salt, info, 128);

            assertEquals(16, key16.length);
            assertEquals(64, key64.length);
            assertEquals(128, key128.length);
        }

        @Test
        @DisplayName("Should throw exception for output length exceeding maximum")
        void shouldThrowExceptionForExcessiveOutputLength() {
            byte[] ikm = MASTER_KEY;
            byte[] salt = new byte[16];
            byte[] info = "test".getBytes();

            int excessiveLength = 256 * 32;

            assertThrows(IllegalArgumentException.class,
                    () -> HkdfUtils.derive(ikm, salt, info, excessiveLength));
        }

        @Test
        @DisplayName("Should derive consistent output for maximum allowed length")
        void shouldDeriveConsistentOutputForMaxLength() {
            byte[] ikm = MASTER_KEY;
            byte[] salt = new byte[16];
            byte[] info = "test".getBytes();

            int maxLength = 255 * 32;

            byte[] key1 = HkdfUtils.derive(ikm, salt, info, maxLength);
            byte[] key2 = HkdfUtils.derive(ikm, salt, info, maxLength);

            assertArrayEquals(key1, key2);
            assertEquals(maxLength, key1.length);
        }
    }

    @Nested
    @DisplayName("Entropy Quality Tests")
    class EntropyQualityTests {

        @Test
        @DisplayName("Should produce cryptographically strong output")
        void shouldProduceCryptographicallyStrongOutput() {
            UUID userId = UUID.randomUUID();
            byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test");

            int maxConsecutive = 0;
            int current = 1;
            for (int i = 1; i < key.length; i++) {
                if (key[i] == key[i - 1]) {
                    current++;
                    maxConsecutive = Math.max(maxConsecutive, current);
                } else {
                    current = 1;
                }
            }

            assertTrue(maxConsecutive < 5, "Key should have good entropy distribution");
        }

        @Test
        @DisplayName("Should have reasonable byte distribution")
        void shouldHaveReasonableByteDistribution() {
            UUID userId = UUID.randomUUID();
            byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test");

            int[] byteCounts = new int[256];
            for (byte b : key) {
                byteCounts[b & 0xFF]++;
            }

            int maxCount = 0;
            for (int count : byteCounts) {
                maxCount = Math.max(maxCount, count);
            }

            assertTrue(maxCount <= key.length / 4 + 2, "No single byte value should dominate");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty info parameter")
        void shouldHandleEmptyInfoParameter() {
            byte[] key = HkdfUtils.derive(MASTER_KEY, new byte[16], new byte[0], 32);

            assertNotNull(key);
            assertEquals(32, key.length);
        }

        @Test
        @DisplayName("Should handle minimum output length")
        void shouldHandleMinimumOutputLength() {
            byte[] key = HkdfUtils.derive(MASTER_KEY, new byte[16], "test".getBytes(), 1);

            assertEquals(1, key.length);
        }

        @Test
        @DisplayName("Should handle various UUID formats")
        void shouldHandleVariousUuidFormats() {
            UUID[] uuids = {
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                    UUID.randomUUID()
            };

            for (UUID uuid : uuids) {
                byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, uuid, "test");
                assertNotNull(key);
                assertEquals(32, key.length);
            }
        }
    }
}
