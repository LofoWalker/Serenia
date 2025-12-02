package com.lofo.serenia.service.auth;

import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;

/**
 * Authentication service responsible for onboarding, login and account management.
 */
public interface AuthService {

    /**
     * Registers a new user and returns the created profile.
     */
    UserResponseDTO register(RegistrationRequestDTO dto);

    /**
     * Authenticates a user and provides their profile view.
     */
    UserResponseDTO login(LoginRequestDTO dto);

    /**
     * Fetches a user profile by email.
     */
    UserResponseDTO getByEmail(String email);

    /**
     * Deletes a user account and related data.
     */
    void deleteAccount(String email);
}
