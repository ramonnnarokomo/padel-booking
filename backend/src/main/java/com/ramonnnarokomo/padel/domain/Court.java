package com.ramonnnarokomo.padel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean indoor;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pricePerHour;

    /** Required by JPA. */
    protected Court() {
    }

    public Court(String name, boolean indoor, BigDecimal pricePerHour) {
        this.name = name;
        this.indoor = indoor;
        this.pricePerHour = pricePerHour;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }
}
