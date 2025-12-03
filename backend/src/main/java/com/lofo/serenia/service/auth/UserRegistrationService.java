package com.lofo.serenia.service.auth;

import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;

public interface UserRegistrationService {
    UserResponseDTO register(RegistrationRequestDTO dto);
}
