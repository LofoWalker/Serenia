package com.lofo.serenia.rest.dto.out.admin;

import java.util.List;

public record TimelineDTO(
        String metric,
        List<TimelineDataPointDTO> data
) {}

