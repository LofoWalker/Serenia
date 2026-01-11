package com.lofo.serenia.rest.dto.out.admin;

import java.util.List;

public record UserListDTO(
        List<UserDetailDTO> users,
        long totalCount,
        int page,
        int size
) {}

