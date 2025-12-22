package com.lofo.serenia.service.user;

import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Service for user account management and profile operations.
 * Handles account deletion, profile retrieval, and user data lifecycle.
 */
@Slf4j
@ApplicationScoped
public class AccountManagementService {

    private static final String ERROR_ACCOUNT_DELETION_FAILED = "Unable to delete account";

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserMapper userMapper;
    private final UserFinder userFinder;

    @Inject
    public AccountManagementService(UserRepository userRepository, ConversationRepository conversationRepository,
                                    MessageRepository messageRepository, UserMapper userMapper, UserFinder userFinder) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userMapper = userMapper;
        this.userFinder = userFinder;
    }

    /**
     * Retrieves user profile information by email.
     *
     * @param email the user's email address
     * @return user profile data transfer object
     * @throws NotFoundException if user does not exist
     */
    public UserResponseDTO getUserProfile(String email) {
        log.debug("Fetching user profile by email=%s", email);
        return userMapper.toView(userFinder.findByEmailOrThrow(email));
    }

    /**
     * Deletes a user account and all associated data.
     * Performs a cascading delete of messages, conversations, and user records.
     *
     * @param email the email of the user to delete
     * @throws NotFoundException if the user does not exist
     * @throws WebApplicationException if deletion fails
     */
    @Transactional
    public void deleteAccountAndAssociatedData(String email) {
        log.info("Deleting account for email=%s", email);
        User user = userFinder.findByEmailOrThrow(email);
        UUID userId = user.getId();

        messageRepository.deleteByUserId(userId);
        conversationRepository.deleteByUserId(userId);
        long deletedUsers = userRepository.deleteById(userId);

        if (deletedUsers != 1) {
            log.error("Unexpected delete result for user %s: %d rows", email, deletedUsers);
            throw new WebApplicationException(ERROR_ACCOUNT_DELETION_FAILED, Response.Status.INTERNAL_SERVER_ERROR);
        }

        log.info("User %s and related data deleted", email);
    }
}

