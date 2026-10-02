package com.ramonnnarokomo.padel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull(message = "Elige una pista.")
        Long courtId,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
        String playerName,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no es válido.")
        String playerEmail,

        @NotNull(message = "Indica la hora de inicio.")
        LocalDateTime start,

        @NotNull(message = "Indica la duración.")
        Integer durationMinutes) {
}
