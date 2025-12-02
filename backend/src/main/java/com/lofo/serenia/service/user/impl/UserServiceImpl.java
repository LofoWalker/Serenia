package com.lofo.serenia.service.user.impl;

import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.user.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

@AllArgsConstructor
@ApplicationScoped
public class UserServiceImpl implements UserService {

    private static final Logger LOG = Logger.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    @Override
    public User findByEmailOrThrow(String email) {
        LOG.debugf("Fetching user by email=%s", email);
        return userRepository.find("email", email)
                .firstResultOptional()
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));
    }
}

