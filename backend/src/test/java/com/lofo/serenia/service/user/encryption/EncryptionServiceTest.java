package com.lofo.serenia.service.user.encryption;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.exceptions.EncryptionException;
import com.lofo.serenia.service.chat.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for EncryptionService.
 * Covers all scenarios including encryption, decryption, edge cases, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionService Tests")
class EncryptionServiceTest {

    private static final String VALID_BASE64_KEY_256 = "qNJYJqH7CdLF0X3R5wZ+bVmK9pL2mN3oQ4rS6tU7vW8=";
    private static final String VALID_HEX_KEY_256 = "0xA8D3582A817B09D2C5D17DD1E7073E6D5989F6F2F8BBC4A37264F2DAE4FB5C6E";
    private static final String VALID_BASE64_KEY_128 = "qNJYJqH7CdLF0X3R5wZ+bQ==";
    private static final String VALID_BASE64_KEY_192 = "C5d5bP1jv8d9o5T6Zb+H1m9r5YpGZ9F0";

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_PLAINTEXT = "Hello, World!";
    private static final String EMPTY_PLAINTEXT = "";
    private static final String LONG_PLAINTEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100);

    @Mock
    private SereniaConfig sereniaConfig;

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_256);
        encryptionService = new EncryptionService(sereniaConfig);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize with valid Base64 key")
        void shouldInitializeWithBase64Key() {
            assertNotNull(encryptionService);
        }

        @Test
        @DisplayName("Should throw exception when config is null")
        void shouldThrowExceptionWhenConfigIsNull() {
            assertThrows(NullPointerException.class, () -> new EncryptionService(null));
        }

        @Test
        @DisplayName("Should throw exception when security key is null")
        void shouldThrowExceptionWhenSecurityKeyIsNull() {
            when(sereniaConfig.securityKey()).thenReturn(null);
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> new EncryptionService(sereniaConfig));
            assertEquals("Configuration property 'serenia.security.key' must not be null or blank",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when security key is blank")
        void shouldThrowExceptionWhenSecurityKeyIsBlank() {
            when(sereniaConfig.securityKey()).thenReturn("   ");
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> new EncryptionService(sereniaConfig));
            assertEquals("Configuration property 'serenia.security.key' must not be null or blank",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when key has invalid length")
        void shouldThrowExceptionWhenKeyHasInvalidLength() {
            // 15 bytes = invalid (must be 16, 24, or 32)
            when(sereniaConfig.securityKey()).thenReturn(Base64.getEncoder().encodeToString(new byte[15]));
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> new EncryptionService(sereniaConfig));
            assertTrue(exception.getMessage().contains("Invalid length"));
        }

        @Test
        @DisplayName("Should initialize with 128-bit key")
        void shouldInitializeWith128BitKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_128);
            assertDoesNotThrow(() -> new EncryptionService(sereniaConfig));
        }

        @Test
        @DisplayName("Should initialize with 192-bit key")
        void shouldInitializeWith192BitKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_192);
            assertDoesNotThrow(() -> new EncryptionService(sereniaConfig));
        }

        @Test
        @DisplayName("Should initialize with hex-prefixed key")
        void shouldInitializeWithHexKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_HEX_KEY_256);
            assertDoesNotThrow(() -> new EncryptionService(sereniaConfig));
        }

        @Test
        @DisplayName("Should throw exception when key format is invalid")
        void shouldThrowExceptionWhenKeyFormatIsInvalid() {
            when(sereniaConfig.securityKey()).thenReturn("!@#$%^&*()");
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> new EncryptionService(sereniaConfig));
            assertTrue(exception.getMessage().contains("Invalid format"));
        }
    }

    @Nested
    @DisplayName("Encryption Tests")
    class EncryptionTests {

        @Test
        @DisplayName("Should encrypt valid plaintext")
        void shouldEncryptValidPlaintext() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            assertNotNull(encrypted);
            assertTrue(encrypted.length > 0);
            // Encrypted data should be larger than plaintext (IV + ciphertext + tag)
            assertTrue(encrypted.length > TEST_PLAINTEXT.getBytes(StandardCharsets.UTF_8).length);
        }

        @Test
        @DisplayName("Should encrypt empty string")
        void shouldEncryptEmptyString() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, EMPTY_PLAINTEXT);
            assertNotNull(encrypted);
            // Even empty string should produce IV (12 bytes) + tag (16 bytes)
            assertTrue(encrypted.length >= 12);
        }

        @Test
        @DisplayName("Should encrypt long plaintext")
        void shouldEncryptLongPlaintext() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, LONG_PLAINTEXT);
            assertNotNull(encrypted);
            assertTrue(encrypted.length > 0);
        }

        @Test
        @DisplayName("Should encrypt same plaintext differently (random IV)")
        void shouldEncryptSamePlaintextDifferently() {
            byte[] encrypted1 = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            byte[] encrypted2 = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            assertNotEquals(
                    java.util.Arrays.toString(encrypted1),
                    java.util.Arrays.toString(encrypted2),
                    "Same plaintext should produce different ciphertext due to random IV"
            );
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            assertThrows(NullPointerException.class,
                    () -> encryptionService.encryptForUser(null, TEST_PLAINTEXT));
        }

        @Test
        @DisplayName("Should throw exception when plaintext is null")
        void shouldThrowExceptionWhenPlaintextIsNull() {
            assertThrows(NullPointerException.class,
                    () -> encryptionService.encryptForUser(TEST_USER_ID, null));
        }

        @Test
        @DisplayName("Should include IV in encrypted payload")
        void shouldIncludeIvInEncryptedPayload() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            // IV is 12 bytes, so encrypted data must be at least 12 bytes
            assertTrue(encrypted.length >= 12);
        }
    }

    @Nested
    @DisplayName("Decryption Tests")
    class DecryptionTests {

        @Test
        @DisplayName("Should decrypt encrypted data")
        void shouldDecryptEncryptedData() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(TEST_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should decrypt empty string")
        void shouldDecryptEmptyString() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, EMPTY_PLAINTEXT);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(EMPTY_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should decrypt long plaintext")
        void shouldDecryptLongPlaintext() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, LONG_PLAINTEXT);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(LONG_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should throw exception when encrypted bytes are null")
        void shouldThrowExceptionWhenEncryptedBytesAreNull() {
            assertThrows(NullPointerException.class,
                    () -> encryptionService.decryptForUser(TEST_USER_ID, null));
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            assertThrows(NullPointerException.class,
                    () -> encryptionService.decryptForUser(null, encrypted));
        }

        @Test
        @DisplayName("Should throw exception when encrypted data is too short")
        void shouldThrowExceptionWhenEncryptedDataIsTooShort() {
            byte[] tooShort = new byte[11]; // Less than IV length (12)
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> encryptionService.decryptForUser(TEST_USER_ID, tooShort));
            assertEquals("Invalid encrypted data: too short to contain IV", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when encrypted data is exactly IV length")
        void shouldThrowExceptionWhenEncryptedDataIsExactlyIvLength() {
            byte[] exactIvLength = new byte[12];
            EncryptionException exception = assertThrows(EncryptionException.class,
                    () -> encryptionService.decryptForUser(TEST_USER_ID, exactIvLength));
            assertEquals("Invalid encrypted data: too short to contain IV", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when data is modified (GCM auth tag verification)")
        void shouldThrowExceptionWhenDataIsModified() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            // Modify a byte in the ciphertext
            encrypted[encrypted.length - 1] = (byte) (encrypted[encrypted.length - 1] ^ 0xFF);
            assertThrows(EncryptionException.class,
                    () -> encryptionService.decryptForUser(TEST_USER_ID, encrypted));
        }

        @Test
        @DisplayName("Should throw exception when IV is modified")
        void shouldThrowExceptionWhenIvIsModified() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            // Modify a byte in the IV
            encrypted[0] = (byte) (encrypted[0] ^ 0xFF);
            assertThrows(EncryptionException.class,
                    () -> encryptionService.decryptForUser(TEST_USER_ID, encrypted));
        }
    }

    @Nested
    @DisplayName("Round-trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should successfully encrypt and decrypt")
        void shouldSuccessfullyEncryptAndDecrypt() {
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(TEST_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should preserve special characters")
        void shouldPreserveSpecialCharacters() {
            String specialText = "Test with special chars: éàù©™ñ™ 🔐";
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, specialText);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(specialText, decrypted);
        }

        @Test
        @DisplayName("Should preserve newlines and whitespace")
        void shouldPreserveNewlinesAndWhitespace() {
            String textWithWhitespace = "Line 1\nLine 2\n\tTabbed\n  Spaces  ";
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, textWithWhitespace);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(textWithWhitespace, decrypted);
        }

        @Test
        @DisplayName("Should handle multiple round-trips")
        void shouldHandleMultipleRoundTrips() {
            String original = "Test data";
            String current = original;

            for (int i = 0; i < 5; i++) {
                byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, current);
                current = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            }

            assertEquals(original, current);
        }
    }

    @Nested
    @DisplayName("Hex Key Format Tests")
    class HexKeyFormatTests {

        @Test
        @DisplayName("Should parse hex key with 0x prefix")
        void shouldParseHexKeyWith0xPrefix() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_HEX_KEY_256);
            EncryptionService service = new EncryptionService(sereniaConfig);
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should parse hex key with 0X prefix")
        void shouldParseHexKeyWith0XPrefix() {
            String upperCaseHex = VALID_HEX_KEY_256.replace("0x", "0X");
            when(sereniaConfig.securityKey()).thenReturn(upperCaseHex);
            EncryptionService service = new EncryptionService(sereniaConfig);
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should reject hex key with odd length")
        void shouldRejectHexKeyWithOddLength() {
            when(sereniaConfig.securityKey()).thenReturn("0xABC");
            assertThrows(EncryptionException.class, () -> new EncryptionService(sereniaConfig));
        }

        @Test
        @DisplayName("Should handle hex key with spaces")
        void shouldHandleHexKeyWithSpaces() {
            String hexWithSpaces = "0x A8D3582A 817B09D2 C5D17DD1 E7073E6D";
            when(sereniaConfig.securityKey()).thenReturn(hexWithSpaces);
            EncryptionService service = new EncryptionService(sereniaConfig);
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should reject invalid hex characters")
        void shouldRejectInvalidHexCharacters() {
            when(sereniaConfig.securityKey()).thenReturn("0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG");
            assertThrows(EncryptionException.class, () -> new EncryptionService(sereniaConfig));
        }
    }

    @Nested
    @DisplayName("Different User IDs Tests")
    class DifferentUserIdsTests {

        @Test
        @DisplayName("Different users should encrypt same plaintext to different ciphertexts")
        void differentUsersShouldProduceDifferentCiphertexts() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            byte[] encrypted1 = encryptionService.encryptForUser(userId1, TEST_PLAINTEXT);
            byte[] encrypted2 = encryptionService.encryptForUser(userId2, TEST_PLAINTEXT);

            // They should be different because of random IV (even though we use same master key)
            assertNotEquals(
                    java.util.Arrays.toString(encrypted1),
                    java.util.Arrays.toString(encrypted2)
            );
        }

        @Test
        @DisplayName("Should correctly decrypt data with different user IDs")
        void shouldCorrectlyDecryptWithDifferentUserIds() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            byte[] encrypted1 = encryptionService.encryptForUser(userId1, TEST_PLAINTEXT);
            byte[] encrypted2 = encryptionService.encryptForUser(userId2, TEST_PLAINTEXT);

            assertEquals(TEST_PLAINTEXT, encryptionService.decryptForUser(userId1, encrypted1));
            assertEquals(TEST_PLAINTEXT, encryptionService.decryptForUser(userId2, encrypted2));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large plaintext")
        void shouldHandleVeryLargePlaintext() {
            String largePlaintext = "x".repeat(1_000_000); // 1 MB
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, largePlaintext);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(largePlaintext, decrypted);
        }

        @Test
        @DisplayName("Should handle single character")
        void shouldHandleSingleCharacter() {
            String singleChar = "A";
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, singleChar);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(singleChar, decrypted);
        }

        @Test
        @DisplayName("Should handle all ASCII characters")
        void shouldHandleAllAsciiCharacters() {
            StringBuilder ascii = new StringBuilder();
            for (int i = 32; i < 127; i++) {
                ascii.append((char) i);
            }
            String asciiText = ascii.toString();
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, asciiText);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(asciiText, decrypted);
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String unicode = "🔐🔒🔓 Ελληνικά العربية 中文";
            byte[] encrypted = encryptionService.encryptForUser(TEST_USER_ID, unicode);
            String decrypted = encryptionService.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(unicode, decrypted);
        }

        @Test
        @DisplayName("Should produce deterministic size for same plaintext length")
        void shouldProduceDeterministicSizeForSamePlaintextLength() {
            String plaintext = "A".repeat(100);
            byte[] encrypted1 = encryptionService.encryptForUser(TEST_USER_ID, plaintext);
            byte[] encrypted2 = encryptionService.encryptForUser(TEST_USER_ID, plaintext);

            // Size should be the same (IV 12 + tag 16 + plaintext length)
            assertEquals(encrypted1.length, encrypted2.length);
        }
    }

    @Nested
    @DisplayName("Key Size Tests")
    class KeySizeTests {

        @Test
        @DisplayName("Should work with 128-bit key")
        void shouldWorkWith128BitKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_128);
            EncryptionService service = new EncryptionService(sereniaConfig);
            byte[] encrypted = service.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            String decrypted = service.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(TEST_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should work with 192-bit key")
        void shouldWorkWith192BitKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_192);
            EncryptionService service = new EncryptionService(sereniaConfig);
            byte[] encrypted = service.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            String decrypted = service.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(TEST_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Should work with 256-bit key")
        void shouldWorkWith256BitKey() {
            when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_256);
            EncryptionService service = new EncryptionService(sereniaConfig);
            byte[] encrypted = service.encryptForUser(TEST_USER_ID, TEST_PLAINTEXT);
            String decrypted = service.decryptForUser(TEST_USER_ID, encrypted);
            assertEquals(TEST_PLAINTEXT, decrypted);
        }
    }

}

