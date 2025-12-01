package com.lofo.serenia.service.token.impl;

import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.service.token.TokenService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenService unit tests")
@Tag("unit")
class TokenServiceTest {

    private static final String ISSUER = "serenia";
    private static final long EXPIRATION_SECONDS = 3600L;
    private static final AtomicLong ROLE_ID_GENERATOR = new AtomicLong(1);

    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        tokenService = new TokenServiceImpl();
        setField(tokenService, "issuer", ISSUER);
        setField(tokenService, "duration", EXPIRATION_SECONDS);
    }

    @Test
    void generateToken_shouldEmbedIssuerSubjectAndUpn_whenUserValid() {
        User user = buildUserWithRoles(Set.of("USER", "ADMIN"));

        String jwt = tokenService.generateToken(user);
        JsonObject payload = parsePayload(jwt);

        assertEquals(ISSUER, payload.getString("iss"));
        assertEquals(user.getEmail(), payload.getString("upn"));
        assertEquals(user.getId().toString(), payload.getString("sub"));
        assertTrue(payload.containsKey("groups"));
    }

    private static Stream<Set<String>> roleProvider() {
        return Stream.of(
                Set.of("USER"),
                Set.of("USER", "ADMIN"),
                Set.of("AUDITOR", "USER", "ADMIN")
        );
    }

    @ParameterizedTest
    @MethodSource("roleProvider")
    void generateToken_shouldIncludeAllRolesInGroups(Set<String> roles) {
        User user = buildUserWithRoles(roles);

        String jwt = tokenService.generateToken(user);
        JsonArray groups = parsePayload(jwt).getJsonArray("groups");

        assertNotNull(groups);
        assertEquals(roles.size(), groups.size());
        Set<String> groupSet = groups.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        assertEquals(roles, groupSet);
    }

    private static User buildUserWithRoles(Set<String> roles) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRoles(roles.stream()
                .map(role -> Role.builder()
                        .id(ROLE_ID_GENERATOR.getAndIncrement())
                        .name(role)
                        .build())
                .collect(Collectors.toSet()));
        return user;
    }

    private static JsonObject parsePayload(String jwt) {
        String[] chunks = jwt.split("\\.");
        assertEquals(3, chunks.length, "JWT must contain header, payload and signature");
        byte[] payloadBytes = Base64.getUrlDecoder().decode(chunks[1]);
        return new JsonObject(new String(payloadBytes, StandardCharsets.UTF_8));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TokenServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
