package com.ramonnnarokomo.padel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(name = "idx_booking_court_start", columnList = "court_id, start_time"))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false, length = 80)
    private String playerName;

    /** Always stored trimmed and in lower case, so lookups by email are exact matches. */
    @Column(nullable = false)
    private String playerEmail;

    // START and END are reserved words in SQL, hence the explicit column names.
    @Column(name = "start_time", nullable = false)
    private LocalDateTime start;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime end;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean peak;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    /** Required by JPA. */
    protected Booking() {
    }

    public Booking(Court court, String playerName, String playerEmail, LocalDateTime start,
                   int durationMinutes, BigDecimal price, boolean peak, LocalDateTime createdAt) {
        this.court = court;
        this.playerName = playerName;
        this.playerEmail = playerEmail;
        this.start = start;
        this.end = start.plusMinutes(durationMinutes);
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.peak = peak;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    public void cancel(LocalDateTime cancelledAt) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() {
        return id;
    }

    public Court getCourt() {
        return court;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerEmail() {
        return playerEmail;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isPeak() {
        return peak;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
