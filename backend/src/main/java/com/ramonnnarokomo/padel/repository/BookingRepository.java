package com.ramonnnarokomo.padel.repository;

import com.ramonnnarokomo.padel.domain.Booking;
import com.ramonnnarokomo.padel.domain.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Is there a booking on this court that overlaps the requested range?
     * Two ranges overlap when each one starts before the other one ends:
     * {@code b.start < requestedEnd and b.end > requestedStart}.
     */
    boolean existsByCourtIdAndStatusAndStartBeforeAndEndAfter(Long courtId, BookingStatus status,
                                                              LocalDateTime requestedEnd,
                                                              LocalDateTime requestedStart);

    long countByPlayerEmailAndStatusAndEndAfter(String playerEmail, BookingStatus status, LocalDateTime now);

    @EntityGraph(attributePaths = "court")
    List<Booking> findByPlayerEmailAndStatusAndEndAfterOrderByStartAsc(String playerEmail, BookingStatus status,
                                                                      LocalDateTime now);

    List<Booking> findByStatusAndStartBetweenOrderByStartAsc(BookingStatus status, LocalDateTime from,
                                                            LocalDateTime to);
}
