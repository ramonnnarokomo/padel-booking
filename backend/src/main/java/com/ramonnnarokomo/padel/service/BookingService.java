package com.ramonnnarokomo.padel.service;

import com.ramonnnarokomo.padel.domain.Booking;
import com.ramonnnarokomo.padel.domain.BookingRules;
import com.ramonnnarokomo.padel.domain.BookingStatus;
import com.ramonnnarokomo.padel.domain.Court;
import com.ramonnnarokomo.padel.dto.BookingRequest;
import com.ramonnnarokomo.padel.dto.BookingResponse;
import com.ramonnnarokomo.padel.dto.QuoteResponse;
import com.ramonnnarokomo.padel.dto.ScheduleResponse;
import com.ramonnnarokomo.padel.exception.BookingRuleException;
import com.ramonnnarokomo.padel.exception.NotFoundException;
import com.ramonnnarokomo.padel.exception.SlotTakenException;
import com.ramonnnarokomo.padel.repository.BookingRepository;
import com.ramonnnarokomo.padel.repository.CourtRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BookingService {

    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public BookingService(CourtRepository courtRepository, BookingRepository bookingRepository, Clock clock) {
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        // The lock makes concurrent bookings of the same court wait for each other.
        Court court = courtRepository.findByIdForUpdate(request.courtId())
                .orElseThrow(() -> courtNotFound(request.courtId()));
        LocalDateTime now = now();
        LocalDateTime start = request.start();
        int durationMinutes = request.durationMinutes();

        BookingRules.validateSlot(start, durationMinutes, now);

        LocalDateTime end = start.plusMinutes(durationMinutes);
        boolean slotTaken = bookingRepository.existsByCourtIdAndStatusAndStartBeforeAndEndAfter(
                court.getId(), BookingStatus.CONFIRMED, end, start);
        if (slotTaken) {
            throw new SlotTakenException("La pista ya está reservada en ese horario.");
        }

        String email = normalizeEmail(request.playerEmail());
        BookingRules.checkActiveBookingLimit(
                bookingRepository.countByPlayerEmailAndStatusAndEndAfter(email, BookingStatus.CONFIRMED, now));

        boolean peak = BookingRules.isPeak(start);
        BigDecimal price = BookingRules.price(court.getPricePerHour(), durationMinutes, peak);
        Booking booking = new Booking(court, request.playerName().trim(), email, start, durationMinutes,
                price, peak, now);
        return BookingResponse.from(bookingRepository.save(booking), now);
    }

    /** One booking of the given player, cancelled ones included, so the URL returned on creation always works. */
    @Transactional(readOnly = true)
    public BookingResponse get(Long id, String email) {
        return BookingResponse.from(findOwnBooking(id, email), now());
    }

    @Transactional
    public void cancel(Long id, String email) {
        Booking booking = findOwnBooking(id, email);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingRuleException("Esta reserva ya está cancelada.");
        }
        LocalDateTime now = now();
        BookingRules.checkCancellable(booking.getStart(), now);

        // No save() needed: the entity is managed, so the change is flushed on commit.
        booking.cancel(now);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> upcoming(String email) {
        LocalDateTime now = now();
        return bookingRepository
                .findByPlayerEmailAndStatusAndEndAfterOrderByStartAsc(normalizeEmail(email), BookingStatus.CONFIRMED, now)
                .stream()
                .map(booking -> BookingResponse.from(booking, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse schedule(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByStatusAndStartBetweenOrderByStartAsc(
                BookingStatus.CONFIRMED, date.atTime(BookingRules.OPENING_TIME), date.atTime(BookingRules.CLOSING_TIME));

        Map<Long, List<String>> bookedSlotsByCourt = new HashMap<>();
        for (Booking booking : bookings) {
            bookedSlotsByCourt
                    .computeIfAbsent(booking.getCourt().getId(), courtId -> new ArrayList<>())
                    .addAll(BookingRules.coveredSlots(booking.getStart().toLocalTime(), booking.getDurationMinutes()));
        }

        List<ScheduleResponse.CourtSchedule> courts = courtRepository.findAll(Sort.by("id")).stream()
                .map(court -> new ScheduleResponse.CourtSchedule(
                        court.getId(),
                        court.getName(),
                        court.isIndoor(),
                        court.getPricePerHour(),
                        bookedSlotsByCourt.getOrDefault(court.getId(), List.of())))
                .toList();

        return new ScheduleResponse(
                date,
                BookingRules.OPENING_TIME.format(BookingRules.SLOT_FORMAT),
                BookingRules.CLOSING_TIME.format(BookingRules.SLOT_FORMAT),
                BookingRules.SLOT_MINUTES,
                courts);
    }

    /** Same time rules as a real booking, but without checking availability or the per-email limit. */
    @Transactional(readOnly = true)
    public QuoteResponse quote(Long courtId, LocalDateTime start, int durationMinutes) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> courtNotFound(courtId));

        BookingRules.validateSlot(start, durationMinutes, now());

        boolean peak = BookingRules.isPeak(start);
        BigDecimal price = BookingRules.price(court.getPricePerHour(), durationMinutes, peak);
        return new QuoteResponse(court.getId(), start, start.plusMinutes(durationMinutes), durationMinutes, price, peak);
    }

    /**
     * A wrong email gets the same 404 as an unknown id, so nobody can find out which bookings exist.
     */
    private Booking findOwnBooking(Long id, String email) {
        return bookingRepository.findById(id)
                .filter(found -> found.getPlayerEmail().equals(normalizeEmail(email)))
                .orElseThrow(() -> new NotFoundException("No existe ninguna reserva " + id + " con ese email."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static NotFoundException courtNotFound(Long courtId) {
        return new NotFoundException("No existe la pista " + courtId + ".");
    }
}
