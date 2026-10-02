package com.ramonnnarokomo.padel.config;

import com.ramonnnarokomo.padel.domain.Court;
import com.ramonnnarokomo.padel.dto.BookingRequest;
import com.ramonnnarokomo.padel.repository.CourtRepository;
import com.ramonnnarokomo.padel.service.BookingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Fills an empty database with the club's four courts and a few bookings for tomorrow,
 * so the app has something to show on first start. Not active in tests.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final CourtRepository courtRepository;
    private final BookingService bookingService;
    private final Clock clock;

    public DataSeeder(CourtRepository courtRepository, BookingService bookingService, Clock clock) {
        this.courtRepository = courtRepository;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (courtRepository.count() > 0) {
            return;
        }

        Court court1 = courtRepository.save(new Court("Pista 1", true, new BigDecimal("18.00")));
        Court court2 = courtRepository.save(new Court("Pista 2", true, new BigDecimal("18.00")));
        courtRepository.save(new Court("Pista 3", false, new BigDecimal("14.00")));
        courtRepository.save(new Court("Pista 4", false, new BigDecimal("14.00")));

        // Going through the service keeps prices and rules consistent with real bookings.
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        bookingService.create(new BookingRequest(
                court1.getId(), "Lucía Navarro", "lucia.navarro@example.com", tomorrow.atTime(10, 0), 90));
        bookingService.create(new BookingRequest(
                court2.getId(), "Diego Romero", "diego.romero@example.com", tomorrow.atTime(18, 0), 60));
        bookingService.create(new BookingRequest(
                court1.getId(), "Sara Ortega", "sara.ortega@example.com", tomorrow.atTime(19, 30), 90));
    }
}
