package com.ramonnnarokomo.padel.exception;

/**
 * The requested court or booking does not exist (or does not belong to the caller). Mapped to 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
