package edu.ucalgary.oop.model;

import java.time.LocalDate;

/** One treatment. The location may be unknown (null) for records loaded from old data. */
public record MedicalRecord(Location location, String treatmentDetails, LocalDate dateOfTreatment) {
    public MedicalRecord {
        if (treatmentDetails == null || treatmentDetails.isBlank()) {
            throw new IllegalArgumentException("Treatment details are required");
        }
        Dates.notInFuture(dateOfTreatment, "Date of treatment");
    }
}
