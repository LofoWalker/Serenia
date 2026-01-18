package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.user.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public long deleteById(UUID userId) {
        return delete("id", userId);
    }
}
