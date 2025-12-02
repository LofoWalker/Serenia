package com.lofo.serenia.mapper;

import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.out.UserResponseDTO;
import io.quarkus.arc.Unremovable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Unremovable
    default UserResponseDTO toView(User user) {
        if (user == null) {
            return null;
        }
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponseDTO(user.getId(), user.getLastName(), user.getFirstName(), user.getEmail(), roles);
    }
}
