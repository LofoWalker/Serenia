package com.lofo.serenia.service.user;

import com.lofo.serenia.domain.user.User;

public interface UserService {
    User findByEmailOrThrow(String email);
}

