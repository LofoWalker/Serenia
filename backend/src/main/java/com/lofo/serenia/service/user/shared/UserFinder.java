package com.lofo.serenia.service.user.shared;

import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Centralized service for finding users.
 * Provides consistent user lookup across all services.
 */
@Slf4j
@ApplicationScoped
public class UserFinder {

    private static final String ERROR_USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;

    @Inject
    public UserFinder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds a user by email or throws NotFoundException.
     *
     * @param email the user's email address
     * @return the user entity
     * @throws NotFoundException if user with given email does not exist
     */
    public User findByEmailOrThrow(String email) {
        log.debug("Fetching user by email={}", email);
        return userRepository.find("email", email)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException(ERROR_USER_NOT_FOUND));
    }

    /**
     * Finds a user by email, returning an Optional.
     *
     * @param email the user's email address
     * @return Optional containing the user if found, empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Fetching user by email={}", email);
        return userRepository.find("email", email).firstResultOptional();
    }

    /**
     * Checks if a user exists with the given email.
     *
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.find("email", email).firstResultOptional().isPresent();
    }
}

