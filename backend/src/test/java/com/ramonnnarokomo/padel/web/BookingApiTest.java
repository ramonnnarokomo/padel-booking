package com.ramonnnarokomo.padel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramonnnarokomo.padel.domain.Court;
import com.ramonnnarokomo.padel.repository.BookingRepository;
import com.ramonnnarokomo.padel.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the REST API against an in-memory H2 database.
 * The clock is fixed at Monday 5 October 2026, 10:00 (Madrid).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingApiTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.atZone(MADRID).toInstant(), MADRID);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Long indoorCourtId;
    private Long outdoorCourtId;

    @BeforeEach
    void resetDatabase() {
        bookingRepository.deleteAll();
        courtRepository.deleteAll();
        indoorCourtId = courtRepository.save(new Court("Pista 1", true, new BigDecimal("18.00"))).getId();
        outdoorCourtId = courtRepository.save(new Court("Pista 3", false, new BigDecimal("14.00"))).getId();
    }

    @Test
    void createsABookingAndShowsItInTheSchedule() throws Exception {
        postBooking(indoorCourtId, "Ramon@Example.com", "2026-10-07T10:00", 90)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/bookings/")))
                .andExpect(jsonPath("$.courtId").value(indoorCourtId))
                .andExpect(jsonPath("$.courtName").value("Pista 1"))
                .andExpect(jsonPath("$.playerEmail").value("ramon@example.com"))
                .andExpect(jsonPath("$.start").value("2026-10-07T10:00:00"))
                .andExpect(jsonPath("$.end").value("2026-10-07T11:30:00"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.price").value(27.00))
                .andExpect(jsonPath("$.peak").value(false))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.cancellable").value(true));

        mockMvc.perform(get("/api/schedule").param("date", "2026-10-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-07"))
                .andExpect(jsonPath("$.openingTime").value("09:00"))
                .andExpect(jsonPath("$.closingTime").value("23:00"))
                .andExpect(jsonPath("$.slotMinutes").value(30))
                .andExpect(jsonPath("$.courts", hasSize(2)))
                .andExpect(jsonPath("$.courts[0].courtName").value("Pista 1"))
                .andExpect(jsonPath("$.courts[0].bookedSlots", contains("10:00", "10:30", "11:00")))
                .andExpect(jsonPath("$.courts[1].bookedSlots", empty()));
    }

    @Test
    void theLocationOfANewBookingReturnsItToItsOwnerOnly() throws Exception {
        String location = postBooking(indoorCourtId, "ramon@example.com", "2026-10-08T19:00", 60)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).param("email", "Ramon@Example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courtName").value("Pista 1"))
                .andExpect(jsonPath("$.start").value("2026-10-08T19:00:00"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.cancellable").value(true));

        mockMvc.perform(get(location).param("email", "someone-else@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/bookings/{id}", 999_999).param("email", "ramon@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnOverlappingBookingOnTheSameCourt() throws Exception {
        createBooking(indoorCourtId, "ramon@example.com", "2026-10-07T10:00", 90);

        postBooking(indoorCourtId, "lucia@example.com", "2026-10-07T11:00", 60)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("reservada en ese horario")));

        // Same time on another court is fine.
        postBooking(outdoorCourtId, "lucia@example.com", "2026-10-07T11:00", 60)
                .andExpect(status().isCreated());
    }

    @Test
    void returnsFieldErrorsForAnInvalidRequest() throws Exception {
        String body = """
                {"courtId": %d, "playerName": "", "playerEmail": "not-an-email", "durationMinutes": 90}
                """.formatted(indoorCourtId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.playerName").exists())
                .andExpect(jsonPath("$.errors.playerEmail").exists())
                .andExpect(jsonPath("$.errors.start").exists())
                .andExpect(jsonPath("$.errors.courtId").doesNotExist());
    }

    @Test
    void quotesWeekdayEveningsWithThePeakSurcharge() throws Exception {
        mockMvc.perform(get("/api/quote")
                        .param("courtId", indoorCourtId.toString())
                        .param("start", "2026-10-06T18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courtId").value(indoorCourtId))
                .andExpect(jsonPath("$.start").value("2026-10-06T18:00:00"))
                .andExpect(jsonPath("$.end").value("2026-10-06T19:30:00"))
                .andExpect(jsonPath("$.price").value(33.75))
                .andExpect(jsonPath("$.peak").value(true));

        postBooking(indoorCourtId, "ramon@example.com", "2026-10-06T18:00", 90)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(33.75))
                .andExpect(jsonPath("$.peak").value(true));
    }

    @Test
    void quoteRejectsBrokenRulesAndUnknownCourts() throws Exception {
        mockMvc.perform(get("/api/quote")
                        .param("courtId", indoorCourtId.toString())
                        .param("start", "2026-10-06T18:15")
                        .param("durationMinutes", "90"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/quote")
                        .param("courtId", "999")
                        .param("start", "2026-10-06T18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isNotFound());
    }

    @Test
    void limitsEachEmailToTwoUpcomingBookings() throws Exception {
        createBooking(indoorCourtId, "ramon@example.com", "2026-10-07T10:00", 60);
        createBooking(indoorCourtId, "ramon@example.com", "2026-10-08T10:00", 60);

        // Different case, same player.
        postBooking(outdoorCourtId, "RAMON@example.com", "2026-10-09T10:00", 60)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Ya tienes 2 reservas activas; cancela una para reservar otra."));

        mockMvc.perform(get("/api/bookings").param("email", "ramon@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].start").value("2026-10-07T10:00:00"))
                .andExpect(jsonPath("$[1].start").value("2026-10-08T10:00:00"));
    }

    @Test
    void cancelsABookingWithEnoughNotice() throws Exception {
        long id = createBooking(indoorCourtId, "ramon@example.com", "2026-10-08T19:00", 60);

        mockMvc.perform(delete("/api/bookings/{id}", id).param("email", "ramon@example.com"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bookings").param("email", "ramon@example.com"))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/schedule").param("date", "2026-10-08"))
                .andExpect(jsonPath("$.courts[0].bookedSlots", empty()));
        // It is still reachable by id, now as cancelled.
        mockMvc.perform(get("/api/bookings/{id}", id).param("email", "ramon@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellable").value(false));
    }

    @Test
    void refusesToCancelLessThanTwentyFourHoursBefore() throws Exception {
        // Today at 20:00 is only 10 hours away.
        long id = createBooking(indoorCourtId, "ramon@example.com", "2026-10-05T20:00", 60);

        mockMvc.perform(get("/api/bookings").param("email", "ramon@example.com"))
                .andExpect(jsonPath("$[0].cancellable").value(false));

        mockMvc.perform(delete("/api/bookings/{id}", id).param("email", "ramon@example.com"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Solo se puede cancelar hasta 24 horas antes."));
    }

    @Test
    void hidesBookingsFromOtherEmails() throws Exception {
        long id = createBooking(indoorCourtId, "ramon@example.com", "2026-10-08T19:00", 60);

        mockMvc.perform(delete("/api/bookings/{id}", id).param("email", "someone-else@example.com"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postBooking(Long courtId, String email, String start, int durationMinutes) throws Exception {
        String body = """
                {"courtId": %d, "playerName": "Ramón", "playerEmail": "%s", "start": "%s", "durationMinutes": %d}
                """.formatted(courtId, email, start, durationMinutes);
        return mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createBooking(Long courtId, String email, String start, int durationMinutes) throws Exception {
        String json = postBooking(courtId, email, start, durationMinutes)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get("id").asLong();
    }
}
