package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.UnactivatedAccountException;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.service.auth.UserAuthenticationService;
import com.lofo.serenia.service.user.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

@AllArgsConstructor
@ApplicationScoped
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

    private static final Logger LOG = Logger.getLogger(UserAuthenticationServiceImpl.class);

    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    public UserResponseDTO login(LoginRequestDTO dto) {
        LOG.infof("Login attempt for email=%s", dto.email());

        User user = findUserByEmailOrThrow(dto);
        ensurePasswordEquality(dto, user);
        ensureAccountActivation(dto, user);

        LOG.infof("Login success for email=%s", dto.email());
        return userMapper.toView(user);
    }

    @Override
    public UserResponseDTO getByEmail(String email) {
        LOG.debugf("Fetching user by email=%s", email);
        return userMapper.toView(userService.findByEmailOrThrow(email));
    }

    private User findUserByEmailOrThrow(LoginRequestDTO dto) {
        try {
            return userService.findByEmailOrThrow(dto.email());
        } catch (WebApplicationException e) {
            throw invalidCredentials(dto.email());
        }
    }

    private static void ensureAccountActivation(LoginRequestDTO dto, User user) {
        if (!user.isAccountActivated()) {
            LOG.warnf("Login failed for email=%s: account not activated", dto.email());
            throw new UnactivatedAccountException(UnactivatedAccountException.USER_MESSAGE);
        }
    }

    private void ensurePasswordEquality(LoginRequestDTO dto, User user) {
        if (!BCrypt.checkpw(dto.password(), user.getPassword())) {
            throw invalidCredentials(dto.email());
        }
    }

    private AuthenticationFailedException invalidCredentials(String email) {
        LOG.warnf("Login failed for email=%s", email);
        return new AuthenticationFailedException("Invalid credentials");
    }
}
