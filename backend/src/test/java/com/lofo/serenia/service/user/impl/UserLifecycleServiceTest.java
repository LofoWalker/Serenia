package com.lofo.serenia.service.user.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.auth.UserRegistrationService;
import com.lofo.serenia.service.user.UserLifecycleService;
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
@DisplayName("UserLifecycleService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class UserLifecycleServiceTest {

    @Inject
    UserLifecycleService userLifecycleService;

    @Inject
    UserRegistrationService userRegistrationService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserTokenQuotaRepository userTokenQuotaRepository;

    @Inject
    UserTokenUsageRepository userTokenUsageRepository;

    @Inject
    RoleRepository roleRepository;

    @BeforeEach
    @Transactional
    void resetDatabase() {
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.persist(Role.builder().name("USER").build());
    }

    @Test
    @DisplayName("should_delete_user_and_related_data")
    void deleteAccountShouldDeleteUserAndRelatedDataAndAssociatedData() {
        String email = uniqueEmail();
        userRegistrationService.register(new RegistrationRequestDTO("Doe", "John", email, "Password123!"));

        User user = userRepository.find("email", email).firstResult();
        assertNotNull(user);

        userLifecycleService.deleteAccountAndAssociatedData(email);

        User deletedUser = userRepository.find("email", email).firstResult();
        assertNull(deletedUser);
    }

    @Test
    @DisplayName("should_throw_not_found_when_deleting_unknown_user")
    void deleteAccountAndAssociatedDataShouldThrowNotFoundWhenDeletingUnknownUser() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> userLifecycleService.deleteAccountAndAssociatedData(uniqueEmail()));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
