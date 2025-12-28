package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import io.quarkus.arc.Unremovable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Unremovable
    default UserResponseDTO toView(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponseDTO(user.getId(), user.getLastName(), user.getFirstName(), user.getEmail(), user.getRole().name());
    }
}
