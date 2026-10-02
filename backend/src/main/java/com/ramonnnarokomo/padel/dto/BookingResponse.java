package com.ramonnnarokomo.padel.dto;

import com.ramonnnarokomo.padel.domain.Booking;
import com.ramonnnarokomo.padel.domain.BookingRules;
import com.ramonnnarokomo.padel.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long courtId,
        String courtName,
        String playerName,
        String playerEmail,
        LocalDateTime start,
        LocalDateTime end,
        int durationMinutes,
        BigDecimal price,
        boolean peak,
        BookingStatus status,
        boolean cancellable) {

    /** Reads the booking's court, so call it inside a transaction (or with the court already loaded). */
    public static BookingResponse from(Booking booking, LocalDateTime now) {
        boolean cancellable = booking.getStatus() == BookingStatus.CONFIRMED
                && BookingRules.canCancel(booking.getStart(), now);
        return new BookingResponse(
                booking.getId(),
                booking.getCourt().getId(),
                booking.getCourt().getName(),
                booking.getPlayerName(),
                booking.getPlayerEmail(),
                booking.getStart(),
                booking.getEnd(),
                booking.getDurationMinutes(),
                booking.getPrice(),
                booking.isPeak(),
                booking.getStatus(),
                cancellable);
    }
}
