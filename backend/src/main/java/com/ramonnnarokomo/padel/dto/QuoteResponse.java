package com.ramonnnarokomo.padel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteResponse(
        Long courtId,
        LocalDateTime start,
        LocalDateTime end,
        int durationMinutes,
        BigDecimal price,
        boolean peak) {
}
