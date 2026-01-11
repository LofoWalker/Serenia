package com.lofo.serenia.rest.dto.out.admin;

import java.time.LocalDate;

public record TimelineDataPointDTO(
        LocalDate date,
        long value
) {}

