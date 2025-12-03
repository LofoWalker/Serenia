package com.lofo.serenia.service.auth;

import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;

public interface UserAuthenticationService {
    UserResponseDTO login(LoginRequestDTO dto);

    UserResponseDTO getByEmail(String email);
}
