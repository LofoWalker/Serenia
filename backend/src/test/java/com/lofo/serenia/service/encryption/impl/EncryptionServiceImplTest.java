package com.lofo.serenia.service.encryption.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.EncryptionException;
import com.lofo.serenia.service.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionServiceImpl tests")
class EncryptionServiceImplTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String TEST_PLAINTEXT = "Hello, World!";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final String DEFAULT_HEX_KEY = "0x7956703D86068BA71F75486CFBEE87AC5978E60AE57501573B7C09A49F7F6734";

    private final SereniaConfig sereniaConfig;
    private EncryptionService encryptionService;

    EncryptionServiceImplTest(@Mock SereniaConfig sereniaConfig) {
        this.sereniaConfig = sereniaConfig;
    }

    @BeforeEach
    void setup() {
        when(sereniaConfig.securityKey()).thenReturn(DEFAULT_HEX_KEY);
        encryptionService = new EncryptionServiceImpl(sereniaConfig);
    }

    private byte[] encrypt(UUID userId, String plaintext) {
        return encryptionService.encryptForUser(userId, plaintext);
    }

    @Nested
    @DisplayName("Configuration and initialization")
    class InitializationTests {

        @Test
        @DisplayName("Doit refuser une configuration nulle")
        void constructor_should_reject_null_config() {
            assertThrows(NullPointerException.class, () -> new EncryptionServiceImpl(null));
        }

        @Test
        @DisplayName("Doit accepter une clé Base64 valide de 16 octets")
        void constructor_should_accept_base64_key() {
            SereniaConfig localConfig = mock(SereniaConfig.class);
            byte[] keyBytes = new byte[16];
            Arrays.fill(keyBytes, (byte) 0x01);
            when(localConfig.securityKey()).thenReturn(Base64.getEncoder().encodeToString(keyBytes));

            assertDoesNotThrow(() -> new EncryptionServiceImpl(localConfig));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "0xABC", "0x1"})
        @DisplayName("Doit signaler une clé mal formée")
        void constructor_should_throw_for_invalid_key(String invalidKey) {
            SereniaConfig localConfig = mock(SereniaConfig.class);
            when(localConfig.securityKey()).thenReturn(invalidKey);

            assertThrows(EncryptionException.class, () -> new EncryptionServiceImpl(localConfig));
        }
    }

    @Nested
    @DisplayName("Gestion des clés utilisateurs")
    class KeyManagementTests {

        @Test
        @DisplayName("Doit créer une clé utilisateur")
        void should_create_user_key() {
            assertDoesNotThrow(() -> encryptionService.createUserKeyIfAbsent(FIXED_USER_ID));
        }

        @Test
        @DisplayName("Doit réutiliser la même clé sans erreur")
        void should_handle_duplicate_key_creation() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            assertDoesNotThrow(() -> encryptionService.createUserKeyIfAbsent(FIXED_USER_ID));
        }

        @Test
        @DisplayName("Doit refuser un userId null")
        void should_reject_null_user_id() {
            assertThrows(NullPointerException.class, () -> encryptionService.createUserKeyIfAbsent(null));
        }

        @Test
        @DisplayName("Doit fournir des contextes indépendants pour plusieurs utilisateurs")
        void should_isolate_multiple_users() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            encryptionService.createUserKeyIfAbsent(ANOTHER_USER_ID);

            byte[] encryptedForFirst = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);
            byte[] encryptedForSecond = encrypt(ANOTHER_USER_ID, TEST_PLAINTEXT);

            assertFalse(Arrays.equals(encryptedForFirst, encryptedForSecond));
        }
    }

    @Nested
    @DisplayName("Chiffrement")
    class EncryptionTests {

        @ParameterizedTest
        @DisplayName("Doit assurer un aller-retour pour plusieurs textes")
        @ValueSource(strings = {"", "Short", "Line 1\nLine 2", "Emoji: 😊", "日本語テキスト"})
        void should_round_trip_various_plaintexts(String plaintext) {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);

            byte[] encrypted = encrypt(FIXED_USER_ID, plaintext);
            assertNotNull(encrypted);
            assertTrue(encrypted.length > GCM_IV_LENGTH_BYTES);

            String decrypted = encryptionService.decryptForUser(FIXED_USER_ID, encrypted);
            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("Doit inclure un IV de 12 octets dans le payload")
        void should_prefix_iv() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);

            byte[] encrypted = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);

            assertEquals(GCM_IV_LENGTH_BYTES, Arrays.copyOfRange(encrypted, 0, GCM_IV_LENGTH_BYTES).length);
            assertTrue(encrypted.length > GCM_IV_LENGTH_BYTES);
        }

        @Test
        @DisplayName("Doit générer des payloads différents pour un même plaintext")
        void should_generate_unique_payloads_per_encrypt() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);

            byte[] first = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);
            byte[] second = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);

            assertFalse(Arrays.equals(first, second));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("Doit autoriser le chiffrement de textes vides ou blancs")
        void should_encrypt_empty_like_texts(String text) {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);

            byte[] encrypted = encrypt(FIXED_USER_ID, text);

            assertNotNull(encrypted);
            assertTrue(encrypted.length > GCM_IV_LENGTH_BYTES);
        }

        @Test
        @DisplayName("Doit signaler un userId null lors du chiffrement")
        void should_throw_when_encrypt_user_id_null() {
            assertThrows(NullPointerException.class, () -> encryptionService.encryptForUser(null, TEST_PLAINTEXT));
        }

        @Test
        @DisplayName("Doit signaler un plaintext null")
        void should_throw_when_encrypt_plaintext_null() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            assertThrows(NullPointerException.class, () -> encryptionService.encryptForUser(FIXED_USER_ID, null));
        }
    }

    @Nested
    @DisplayName("Déchiffrement")
    class DecryptionTests {

        @Test
        @DisplayName("Doit retrouver le texte original")
        void should_decrypt_valid_encryption() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            byte[] encrypted = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);

            String decrypted = encryptionService.decryptForUser(FIXED_USER_ID, encrypted);

            assertEquals(TEST_PLAINTEXT, decrypted);
        }

        @Test
        @DisplayName("Doit signaler un payload trop court")
        void should_throw_when_payload_too_short() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            byte[] triple = new byte[GCM_IV_LENGTH_BYTES - 1];

            assertThrows(EncryptionException.class, () -> encryptionService.decryptForUser(FIXED_USER_ID, triple));
        }

        @Test
        @DisplayName("Doit refuser un tableau exactement de la longueur de l'IV")
        void should_throw_when_payload_exact_iv_length() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            byte[] ivOnly = new byte[GCM_IV_LENGTH_BYTES];

            assertThrows(EncryptionException.class, () -> encryptionService.decryptForUser(FIXED_USER_ID, ivOnly));
        }

        @Test
        @DisplayName("Doit détecter un IV corrompu")
        void should_throw_when_iv_corrupted() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            byte[] encrypted = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);
            encrypted[0] ^= 0xFF;

            assertThrows(EncryptionException.class, () -> encryptionService.decryptForUser(FIXED_USER_ID, encrypted));
        }

        @Test
        @DisplayName("Doit détecter un ciphertext corrompu")
        void should_throw_when_ciphertext_corrupted() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            byte[] encrypted = encrypt(FIXED_USER_ID, TEST_PLAINTEXT);
            if (encrypted.length > GCM_IV_LENGTH_BYTES) {
                encrypted[GCM_IV_LENGTH_BYTES] ^= 0x0F;
            }

            assertThrows(EncryptionException.class, () -> encryptionService.decryptForUser(FIXED_USER_ID, encrypted));
        }

        @Test
        @DisplayName("Doit signaler un userId null lors du déchiffrement")
        void should_throw_when_decrypt_user_id_null() {
            byte[] encrypted = new byte[GCM_IV_LENGTH_BYTES + 16];
            assertThrows(NullPointerException.class, () -> encryptionService.decryptForUser(null, encrypted));
        }

        @Test
        @DisplayName("Doit refuser des octets null")
        void should_throw_when_encrypted_bytes_null() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            assertThrows(NullPointerException.class, () -> encryptionService.decryptForUser(FIXED_USER_ID, null));
        }
    }

    @Nested
    @DisplayName("Intégrité globale")
    class IntegrityTests {

        @Test
        @DisplayName("Doit gérer plusieurs cycles de chiffrement/déchiffrement")
        void should_handle_multiple_cycles() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            List<String> plaintexts = Stream.of("First", "Second", "Third", "Fourth", "Fifth").toList();
            List<byte[]> encrypted = new ArrayList<>();
            plaintexts.forEach(text -> encrypted.add(encrypt(FIXED_USER_ID, text)));

            for (int index = 0; index < plaintexts.size(); index++) {
                assertEquals(plaintexts.get(index), encryptionService.decryptForUser(FIXED_USER_ID, encrypted.get(index)));
            }
        }

        @Test
        @DisplayName("Doit conserver le texte long")
        void should_handle_long_text() {
            encryptionService.createUserKeyIfAbsent(FIXED_USER_ID);
            StringBuilder builder = new StringBuilder();
            builder.append("Long text ".repeat(1000));

            byte[] encrypted = encrypt(FIXED_USER_ID, builder.toString());
            assertEquals(builder.toString(), encryptionService.decryptForUser(FIXED_USER_ID, encrypted));
        }
    }

}
