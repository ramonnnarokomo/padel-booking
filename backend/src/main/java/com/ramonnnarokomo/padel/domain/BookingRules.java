package com.ramonnnarokomo.padel.domain;

import com.ramonnnarokomo.padel.exception.BookingRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The club's booking rules.
 * <p>
 * Plain Java on purpose (no Spring, no database): every method receives "now" as a parameter,
 * so the rules can be unit-tested with fixed dates.
 */
public final class BookingRules {

    public static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);
    public static final int SLOT_MINUTES = 30;
    public static final List<Integer> ALLOWED_DURATIONS = List.of(60, 90, 120);
    public static final int MAX_DAYS_AHEAD = 14;
    public static final int MAX_ACTIVE_BOOKINGS_PER_EMAIL = 2;
    public static final Duration MIN_NOTICE_TO_CANCEL = Duration.ofHours(24);
    public static final LocalTime PEAK_FROM = LocalTime.of(18, 0);
    public static final BigDecimal PEAK_SURCHARGE = new BigDecimal("1.25");
    public static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private BookingRules() {
    }

    /**
     * Checks the requested time slot: duration, half-hour start, opening hours,
     * not in the past and at most 14 days ahead.
     */
    public static void validateSlot(LocalDateTime start, int durationMinutes, LocalDateTime now) {
        if (!ALLOWED_DURATIONS.contains(durationMinutes)) {
            throw new BookingRuleException("Duración no válida: elige 60, 90 o 120 minutos.");
        }
        if (start.getMinute() % SLOT_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new BookingRuleException("Las reservas empiezan en punto o y media.");
        }

        // Compare full date-times: 22:30 + 120 min ends at 00:30 of the next day.
        LocalDateTime opening = start.toLocalDate().atTime(OPENING_TIME);
        LocalDateTime closing = start.toLocalDate().atTime(CLOSING_TIME);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        if (start.isBefore(opening) || end.isAfter(closing)) {
            throw new BookingRuleException("El club abre de 09:00 a 23:00.");
        }

        if (!start.isAfter(now)) {
            throw new BookingRuleException("No se puede reservar en el pasado.");
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            throw new BookingRuleException("Solo se puede reservar con 14 días de antelación como máximo.");
        }
    }

    /** Peak hours: Monday to Friday, starting at 18:00 or later. */
    public static boolean isPeak(LocalDateTime start) {
        DayOfWeek day = start.getDayOfWeek();
        boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        return weekday && !start.toLocalTime().isBefore(PEAK_FROM);
    }

    /** pricePerHour × minutes / 60 (× 1.25 at peak hours), rounded half-up to cents. */
    public static BigDecimal price(BigDecimal pricePerHour, int durationMinutes, boolean peak) {
        BigDecimal price = pricePerHour.multiply(BigDecimal.valueOf(durationMinutes));
        if (peak) {
            price = price.multiply(PEAK_SURCHARGE);
        }
        return price.divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    /** @param activeBookings upcoming confirmed bookings the player already has */
    public static void checkActiveBookingLimit(long activeBookings) {
        if (activeBookings >= MAX_ACTIVE_BOOKINGS_PER_EMAIL) {
            throw new BookingRuleException("Ya tienes 2 reservas activas; cancela una para reservar otra.");
        }
    }

    /** A booking can be cancelled while its start is at least 24 hours away. */
    public static boolean canCancel(LocalDateTime start, LocalDateTime now) {
        return !start.isBefore(now.plus(MIN_NOTICE_TO_CANCEL));
    }

    public static void checkCancellable(LocalDateTime start, LocalDateTime now) {
        if (!canCancel(start, now)) {
            throw new BookingRuleException("Solo se puede cancelar hasta 24 horas antes.");
        }
    }

    /** The 30-minute slots a booking occupies, e.g. 18:00 + 90 min gives ["18:00", "18:30", "19:00"]. */
    public static List<String> coveredSlots(LocalTime start, int durationMinutes) {
        List<String> slots = new ArrayList<>();
        for (int offset = 0; offset < durationMinutes; offset += SLOT_MINUTES) {
            slots.add(start.plusMinutes(offset).format(SLOT_FORMAT));
        }
        return slots;
    }

    /** Every slot of the day: "09:00", "09:30", ..., "22:30". */
    public static List<String> allSlots() {
        int minutesOpen = (int) Duration.between(OPENING_TIME, CLOSING_TIME).toMinutes();
        return coveredSlots(OPENING_TIME, minutesOpen);
    }
}
