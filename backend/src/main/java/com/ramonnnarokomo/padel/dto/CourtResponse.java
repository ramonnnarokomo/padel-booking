package com.ramonnnarokomo.padel.dto;

import com.ramonnnarokomo.padel.domain.Court;

import java.math.BigDecimal;

public record CourtResponse(Long id, String name, boolean indoor, BigDecimal pricePerHour) {

    public static CourtResponse from(Court court) {
        return new CourtResponse(court.getId(), court.getName(), court.isIndoor(), court.getPricePerHour());
    }
}
