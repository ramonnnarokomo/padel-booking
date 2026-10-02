package com.ramonnnarokomo.padel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Which 30-minute slots of a given day are already booked on each court. */
public record ScheduleResponse(
        LocalDate date,
        String openingTime,
        String closingTime,
        int slotMinutes,
        List<CourtSchedule> courts) {

    public record CourtSchedule(
            Long courtId,
            String courtName,
            boolean indoor,
            BigDecimal pricePerHour,
            List<String> bookedSlots) {
    }
}
