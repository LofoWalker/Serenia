package com.lofo.serenia.service.encryption;

import java.util.UUID;


public interface EncryptionService {

    byte[] encryptForUser(UUID userId, String plaintext);

    String decryptForUser(UUID userId, byte[] ciphertext);

    void createUserKeyIfAbsent(UUID userId);
}
