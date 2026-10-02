package com.ramonnnarokomo.padel.exception;

/**
 * The court already has a confirmed booking that overlaps the requested time. Mapped to 409.
 */
public class SlotTakenException extends RuntimeException {

    public SlotTakenException(String message) {
        super(message);
    }
}
