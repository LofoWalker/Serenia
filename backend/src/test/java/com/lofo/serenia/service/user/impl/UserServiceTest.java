package com.lofo.serenia.service.user.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.UserRegistrationService;
import com.lofo.serenia.service.user.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("UserService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class UserServiceTest {

    @Inject
    UserService userService;

    @Inject
    UserRegistrationService userRegistrationService;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @BeforeEach
    @Transactional
    void resetDatabase() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.persist(Role.builder().name("USER").build());
    }

    @Test
    @DisplayName("should_return_user_when_email_exists")
    void findByEmailOrThrowShouldReturnUserWhenEmailExists() {
        String email = uniqueEmail();
        userRegistrationService.register(new RegistrationRequestDTO("Doe", "John", email, "Password123!"));

        User foundUser = userService.findByEmailOrThrow(email);

        assertNotNull(foundUser);
        assertEquals(email, foundUser.getEmail());
        assertEquals("John", foundUser.getFirstName());
        assertEquals("Doe", foundUser.getLastName());
    }

    @Test
    @DisplayName("should_throw_not_found_when_email_does_not_exist")
    void findByEmailOrThrowShouldThrowNotFoundWhenEmailDoesNotExist() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> userService.findByEmailOrThrow(uniqueEmail()));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}

