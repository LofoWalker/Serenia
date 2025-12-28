package com.lofo.serenia.service.user.authentication;

import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.UnactivatedAccountException;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.rest.dto.in.LoginRequestDTO;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service for user authentication and login operations.
 * Handles credentials validation and account activation checks.
 */
@Slf4j
@ApplicationScoped
public class AuthenticationService {

    private static final String ERROR_INVALID_CREDENTIALS = "Invalid credentials";

    private final UserFinder userFinder;
    private final UserMapper userMapper;

    @Inject
    public AuthenticationService(UserFinder userFinder, UserMapper userMapper) {
        this.userFinder = userFinder;
        this.userMapper = userMapper;
    }

    /**
     * Authenticates a user with provided credentials.
     * Validates email existence, password correctness, and account activation status.
     *
     * @param dto the login request containing email and password
     * @return user profile data transfer object
     * @throws NotFoundException if user with given email does not exist
     * @throws AuthenticationFailedException if password verification fails
     * @throws UnactivatedAccountException if user account is not activated
     */
    @Transactional
    public UserResponseDTO login(LoginRequestDTO dto) {
        log.info("Login attempt for email={}", dto.email());

        User user = userFinder.findByEmailOrThrow(dto.email());
        validatePassword(dto.password(), user.getPassword());
        validateAccountActivation(user);

        log.info("Login success for email={}", dto.email());
        return userMapper.toView(user);
    }


    /**
     * Validates the provided password against the stored hashed password.
     *
     * @param providedPassword the plaintext password provided by the user
     * @param storedPassword the bcrypt hash stored in database
     * @throws AuthenticationFailedException if passwords do not match
     */
    private void validatePassword(String providedPassword, String storedPassword) {
        if (!BCrypt.checkpw(providedPassword, storedPassword)) {
            throw new AuthenticationFailedException(ERROR_INVALID_CREDENTIALS);
        }
    }

    /**
     * Validates that the user's account is activated.
     *
     * @param user the user entity to validate
     * @throws UnactivatedAccountException if account is not activated
     */
    private void validateAccountActivation(User user) {
        if (!user.isAccountActivated()) {
            log.warn("Login failed for email={}: account not activated", user.getEmail());
            throw new UnactivatedAccountException(UnactivatedAccountException.USER_MESSAGE);
        }
    }
}

