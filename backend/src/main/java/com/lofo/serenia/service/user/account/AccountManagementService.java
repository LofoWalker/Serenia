package com.lofo.serenia.service.user.account;

import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.chat.ConversationService;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Service for user account management and profile operations.
 * Handles account deletion, profile retrieval, and user data lifecycle.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AccountManagementService {

    private static final String ERROR_ACCOUNT_DELETION_FAILED = "Unable to delete account";

    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final UserMapper userMapper;
    private final UserFinder userFinder;

    /**
     * Retrieves user profile information by email.
     *
     * @param email the user's email address
     * @return user profile data transfer object
     * @throws NotFoundException if user does not exist
     */
    public UserResponseDTO getUserProfile(String email) {
        log.debug("Fetching user profile by email={}", email);
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
        log.info("Deleting account for email={}", email);
        User user = userFinder.findByEmailOrThrow(email);
        UUID userId = user.getId();

        conversationService.deleteUserConversations(userId);
        long deletedUsers = userRepository.deleteById(userId);

        if (deletedUsers != 1) {
            log.error("Unexpected delete result for user {}: {} rows", email, deletedUsers);
            throw new WebApplicationException(ERROR_ACCOUNT_DELETION_FAILED, Response.Status.INTERNAL_SERVER_ERROR);
        }

        log.info("User {} and related data deleted", email);
    }
}
