package com.ramonnnarokomo.padel.exception;

/**
 * A club rule was broken (wrong time, too many bookings, too late to cancel...).
 * Mapped to 422; the message is in Spanish because the UI shows it as is.
 */
public class BookingRuleException extends RuntimeException {

    public BookingRuleException(String message) {
        super(message);
    }
}
