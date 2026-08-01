package edu.ucalgary.oop.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Date parsing and the "not in the future" rule shared by every dated record. */
public final class Dates {
    private Dates() {
    }

    /** Parses an ISO date (yyyy-mm-dd). */
    public static LocalDate parse(String text, String what) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(what + " must be a date in the form yyyy-mm-dd");
        }
    }

    public static LocalDate notInFuture(LocalDate date, String what) {
        if (date == null) {
            throw new IllegalArgumentException(what + " is required");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(what + " cannot be in the future");
        }
        return date;
    }
}
