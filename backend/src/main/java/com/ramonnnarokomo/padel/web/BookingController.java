package com.ramonnnarokomo.padel.web;

import com.ramonnnarokomo.padel.dto.BookingRequest;
import com.ramonnnarokomo.padel.dto.BookingResponse;
import com.ramonnnarokomo.padel.dto.QuoteResponse;
import com.ramonnnarokomo.padel.dto.ScheduleResponse;
import com.ramonnnarokomo.padel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/schedule")
    public ScheduleResponse schedule(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.schedule(date);
    }

    @GetMapping("/quote")
    public QuoteResponse quote(@RequestParam Long courtId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                               @RequestParam int durationMinutes) {
        return bookingService.quote(courtId, start, durationMinutes);
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(booking.id())
                .toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> upcoming(@RequestParam String email) {
        return bookingService.upcoming(email);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@PathVariable Long id, @RequestParam String email) {
        return bookingService.get(id, email);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @RequestParam String email) {
        bookingService.cancel(id, email);
        return ResponseEntity.noContent().build();
    }
}
