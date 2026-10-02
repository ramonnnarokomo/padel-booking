package com.ramonnnarokomo.padel.domain;

import com.ramonnnarokomo.padel.exception.BookingRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests: no Spring context, "now" is always Monday 5 October 2026 at 10:00. */
class BookingRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    // --- validateSlot ---

    @Test
    void acceptsAValidSlot() {
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 18, 0), 90, NOW));
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 18, 30), 60, NOW));
    }

    @Test
    void rejectsDurationsOutsideTheAllowedOnes() {
        for (int duration : new int[] {0, 30, 45, 75, 150}) {
            assertRejected(LocalDateTime.of(2026, 10, 6, 18, 0), duration,
                    "Duración no válida: elige 60, 90 o 120 minutos.");
        }
    }

    @Test
    void acceptsEveryAllowedDuration() {
        for (int duration : new int[] {60, 90, 120}) {
            assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 18, 0), duration, NOW));
        }
    }

    @Test
    void rejectsStartsThatAreNotOnTheHourOrHalfHour() {
        String message = "Las reservas empiezan en punto o y media.";
        assertRejected(LocalDateTime.of(2026, 10, 6, 18, 15), 60, message);
        assertRejected(LocalDateTime.of(2026, 10, 6, 18, 0, 30), 60, message);
        assertRejected(LocalDateTime.of(2026, 10, 6, 18, 0, 0, 1), 60, message);
    }

    @Test
    void rejectsStartsBeforeOpening() {
        assertRejected(LocalDateTime.of(2026, 10, 6, 8, 30), 60, "El club abre de 09:00 a 23:00.");
    }

    @Test
    void acceptsTheFirstSlotOfTheDay() {
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 9, 0), 60, NOW));
    }

    @Test
    void rejectsBookingsThatEndAfterClosing() {
        assertRejected(LocalDateTime.of(2026, 10, 6, 22, 30), 60, "El club abre de 09:00 a 23:00.");
        assertRejected(LocalDateTime.of(2026, 10, 6, 21, 30), 120, "El club abre de 09:00 a 23:00.");
    }

    @Test
    void rejectsLateBookingsThatWouldEndAfterMidnight() {
        // 22:30 + 120 min ends at 00:30 the next day, which is "before 23:00" if you only compare times.
        assertRejected(LocalDateTime.of(2026, 10, 6, 22, 30), 120, "El club abre de 09:00 a 23:00.");
    }

    @Test
    void acceptsBookingsEndingExactlyAtClosing() {
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 22, 0), 60, NOW));
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 6, 21, 0), 120, NOW));
    }

    @Test
    void rejectsStartsInThePast() {
        String message = "No se puede reservar en el pasado.";
        assertRejected(LocalDateTime.of(2026, 10, 5, 9, 30), 60, message);
        assertRejected(NOW, 60, message);
        assertRejected(LocalDateTime.of(2026, 10, 4, 18, 0), 60, message);
    }

    @Test
    void acceptsLaterTodayAndUpToFourteenDaysAhead() {
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 5, 10, 30), 60, NOW));
        assertDoesNotThrow(() -> BookingRules.validateSlot(LocalDateTime.of(2026, 10, 19, 22, 0), 60, NOW));
    }

    @Test
    void rejectsStartsMoreThanFourteenDaysAhead() {
        assertRejected(LocalDateTime.of(2026, 10, 20, 9, 0), 60,
                "Solo se puede reservar con 14 días de antelación como máximo.");
    }

    // --- peak hours and price ---

    @Test
    void weekdayEveningsArePeak() {
        assertTrue(BookingRules.isPeak(LocalDateTime.of(2026, 10, 5, 18, 0)));   // Monday
        assertTrue(BookingRules.isPeak(LocalDateTime.of(2026, 10, 9, 21, 30)));  // Friday
    }

    @Test
    void weekdayDaytimeAndWeekendsAreNotPeak() {
        assertFalse(BookingRules.isPeak(LocalDateTime.of(2026, 10, 6, 17, 30)));  // Tuesday, ends after 18:00
        assertFalse(BookingRules.isPeak(LocalDateTime.of(2026, 10, 10, 18, 0)));  // Saturday
        assertFalse(BookingRules.isPeak(LocalDateTime.of(2026, 10, 11, 20, 0)));  // Sunday
    }

    @Test
    void pricesAPeakWeekdayBooking() {
        LocalDateTime tuesdayEvening = LocalDateTime.of(2026, 10, 6, 18, 0);
        boolean peak = BookingRules.isPeak(tuesdayEvening);

        assertTrue(peak);
        assertEquals(new BigDecimal("33.75"), BookingRules.price(new BigDecimal("18.00"), 90, peak));
    }

    @Test
    void pricesASaturdayEveningWithoutSurcharge() {
        LocalDateTime saturdayEvening = LocalDateTime.of(2026, 10, 10, 18, 0);
        boolean peak = BookingRules.isPeak(saturdayEvening);

        assertFalse(peak);
        assertEquals(new BigDecimal("21.00"), BookingRules.price(new BigDecimal("14.00"), 90, peak));
    }

    @Test
    void roundsPricesHalfUpToCents() {
        // 13.33 * 90 / 60 = 19.995
        assertEquals(new BigDecimal("20.00"), BookingRules.price(new BigDecimal("13.33"), 90, false));
        // 10.01 * 60 / 60 * 1.25 = 12.5125
        assertEquals(new BigDecimal("12.51"), BookingRules.price(new BigDecimal("10.01"), 60, true));
    }

    // --- per-email limit ---

    @Test
    void allowsUpToTwoActiveBookingsPerEmail() {
        assertDoesNotThrow(() -> BookingRules.checkActiveBookingLimit(0));
        assertDoesNotThrow(() -> BookingRules.checkActiveBookingLimit(1));
    }

    @Test
    void rejectsAThirdActiveBooking() {
        BookingRuleException ex = assertThrows(BookingRuleException.class,
                () -> BookingRules.checkActiveBookingLimit(2));
        assertEquals("Ya tienes 2 reservas activas; cancela una para reservar otra.", ex.getMessage());
    }

    // --- cancellation ---

    @Test
    void canCancelWithExactlyTwentyFourHoursNotice() {
        LocalDateTime start = NOW.plusHours(24);

        assertTrue(BookingRules.canCancel(start, NOW));
        assertDoesNotThrow(() -> BookingRules.checkCancellable(start, NOW));
    }

    @Test
    void cannotCancelWithLessThanTwentyFourHoursNotice() {
        LocalDateTime start = NOW.plusHours(23).plusMinutes(59);

        assertFalse(BookingRules.canCancel(start, NOW));
        BookingRuleException ex = assertThrows(BookingRuleException.class,
                () -> BookingRules.checkCancellable(start, NOW));
        assertEquals("Solo se puede cancelar hasta 24 horas antes.", ex.getMessage());
    }

    // --- slots ---

    @Test
    void listsTheSlotsCoveredByABooking() {
        assertEquals(List.of("18:00", "18:30", "19:00"), BookingRules.coveredSlots(LocalTime.of(18, 0), 90));
        assertEquals(List.of("09:00", "09:30"), BookingRules.coveredSlots(LocalTime.of(9, 0), 60));
        assertEquals(List.of("21:00", "21:30", "22:00", "22:30"), BookingRules.coveredSlots(LocalTime.of(21, 0), 120));
    }

    @Test
    void listsEverySlotOfTheDay() {
        List<String> slots = BookingRules.allSlots();

        assertEquals(28, slots.size());
        assertEquals("09:00", slots.get(0));
        assertEquals("09:30", slots.get(1));
        assertEquals("22:30", slots.get(slots.size() - 1));
    }

    private static void assertRejected(LocalDateTime start, int durationMinutes, String expectedMessage) {
        BookingRuleException ex = assertThrows(BookingRuleException.class,
                () -> BookingRules.validateSlot(start, durationMinutes, NOW));
        assertEquals(expectedMessage, ex.getMessage());
    }
}
