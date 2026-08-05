package edu.ucalgary.oop.model;

import java.time.LocalDate;

/** One call from an inquirer. */
public record Inquiry(LocalDate date, String details) {
    public Inquiry {
        Dates.notInFuture(date, "Call date");
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Inquiry details are required");
        }
    }
}
