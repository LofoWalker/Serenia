package com.lofo.serenia.service.user;

import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.account.AccountManagementService;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountManagementService Unit Tests")
class AccountManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFinder userFinder;

    private AccountManagementService accountManagementService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "test@example.com";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @BeforeEach
    void setUp() {
        accountManagementService = new AccountManagementService(
                userRepository,
                conversationRepository,
                messageRepository,
                userMapper,
                userFinder
        );
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("should return user profile dto")
        void should_return_user_profile_dto() {
            User user = createUser();
            UserResponseDTO expectedDto = new UserResponseDTO(USER_ID, LAST_NAME, FIRST_NAME, USER_EMAIL, "USER");

            when(userFinder.findByEmailOrThrow(USER_EMAIL)).thenReturn(user);
            when(userMapper.toView(user)).thenReturn(expectedDto);

            UserResponseDTO result = accountManagementService.getUserProfile(USER_EMAIL);

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo(USER_EMAIL);
            assertThat(result.firstName()).isEqualTo(FIRST_NAME);
            assertThat(result.lastName()).isEqualTo(LAST_NAME);
            assertThat(result.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should throw when user not found")
        void should_throw_when_user_not_found() {
            when(userFinder.findByEmailOrThrow(USER_EMAIL)).thenThrow(new NotFoundException("User not found"));

            assertThatThrownBy(() -> accountManagementService.getUserProfile(USER_EMAIL))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("deleteAccountAndAssociatedData")
    class DeleteAccountAndAssociatedData {

        @Test
        @DisplayName("should delete user and all related data")
        void should_delete_user_and_all_related_data() {
            User user = createUser();

            when(userFinder.findByEmailOrThrow(USER_EMAIL)).thenReturn(user);
            when(userRepository.deleteById(USER_ID)).thenReturn(1L);

            accountManagementService.deleteAccountAndAssociatedData(USER_EMAIL);

            verify(messageRepository).deleteByUserId(USER_ID);
            verify(conversationRepository).deleteByUserId(USER_ID);
            verify(userRepository).deleteById(USER_ID);
        }

        @Test
        @DisplayName("should throw when deletion fails")
        void should_throw_when_deletion_fails() {
            User user = createUser();

            when(userFinder.findByEmailOrThrow(USER_EMAIL)).thenReturn(user);
            when(userRepository.deleteById(USER_ID)).thenReturn(0L);

            assertThatThrownBy(() -> accountManagementService.deleteAccountAndAssociatedData(USER_EMAIL))
                    .isInstanceOf(WebApplicationException.class)
                    .hasMessageContaining("Unable to delete account");
        }

        @Test
        @DisplayName("should delete messages before conversations")
        void should_delete_messages_before_conversations() {
            User user = createUser();

            when(userFinder.findByEmailOrThrow(USER_EMAIL)).thenReturn(user);
            when(userRepository.deleteById(USER_ID)).thenReturn(1L);

            accountManagementService.deleteAccountAndAssociatedData(USER_EMAIL);

            InOrder inOrder = inOrder(messageRepository, conversationRepository, userRepository);
            inOrder.verify(messageRepository).deleteByUserId(USER_ID);
            inOrder.verify(conversationRepository).deleteByUserId(USER_ID);
            inOrder.verify(userRepository).deleteById(USER_ID);
        }
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setPassword("hashedPassword");
        user.setRole(Role.USER);
        user.setAccountActivated(true);
        return user;
    }
}
