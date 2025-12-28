package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.mapper.UserMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public long deleteById(UUID userId) {
        return delete("id", userId);
    }

    public UserResponseDTO findViewByEmail(String email, UserMapper mapper) {
        return find("email", email).project(User.class)
                .firstResultOptional()
                .map(mapper::toView)
                .orElse(null);
    }
}
