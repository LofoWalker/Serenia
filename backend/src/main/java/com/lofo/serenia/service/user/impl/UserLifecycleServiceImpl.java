package com.lofo.serenia.service.user.impl;

import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.repository.*;
import com.lofo.serenia.service.user.UserLifecycleService;
import com.lofo.serenia.service.user.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
public class UserLifecycleServiceImpl implements UserLifecycleService {

    private static final Logger LOG = Logger.getLogger(UserLifecycleServiceImpl.class);

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserTokenQuotaRepository userTokenQuotaRepository;
    private final UserTokenUsageRepository userTokenUsageRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void deleteAccountAndAssociatedData(String email) {
        LOG.infof("Deleting account for email=%s", email);
        User user = userService.findByEmailOrThrow(email);
        UUID userId = user.getId();

        userTokenUsageRepository.deleteByUserId(userId);
        userTokenQuotaRepository.deleteByUserId(userId);
        messageRepository.deleteByUserId(userId);
        conversationRepository.deleteByUserId(userId);
        long deletedUsers = userRepository.deleteById(userId);

        checkIfSomethingWentWrong(email, deletedUsers);

        LOG.infof("User %s and related data deleted", email);
    }

    private static void checkIfSomethingWentWrong(String email, long deletedUsers) {
        if (deletedUsers != 1) {
            LOG.errorf("Unexpected delete result for user %s: %d rows", email, deletedUsers);
            throw new WebApplicationException("Unable to delete account", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
