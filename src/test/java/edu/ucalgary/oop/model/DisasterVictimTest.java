package edu.ucalgary.oop.model;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DisasterVictimTest {
    private static final LocalDate TODAY = LocalDate.now();

    private static DisasterVictim victim() {
        return new DisasterVictim(1, "Freda", "Gonzalez", TODAY);
    }

    @Test
    public void namesAreTrimmedAndLastNameIsOptional() {
        DisasterVictim v = new DisasterVictim(2, "  Ana ", "  ", TODAY);
        assertEquals("Ana", v.getFirstName());
        assertNull(v.getLastName());
        assertEquals("Ana", v.getFullName());
        assertThrows(IllegalArgumentException.class, () -> new DisasterVictim(3, " ", "X", TODAY));
    }

    @Test
    public void rejectsFutureEntryDateAndBadId() {
        assertThrows(IllegalArgumentException.class, () -> new DisasterVictim(1, "A", null, TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> new DisasterVictim(0, "A", null, TODAY));
    }

    @Test
    public void ageFollowsDateOfBirth() {
        DisasterVictim v = victim();
        assertEquals(-1, v.getAge());
        v.setDateOfBirth(TODAY.minusYears(30));
        assertEquals(30, v.getAge());
        v.setDateOfBirth(TODAY.minusYears(30).plusDays(1));
        assertEquals(29, v.getAge());
        assertThrows(IllegalArgumentException.class, () -> v.setDateOfBirth(TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> v.setDateOfBirth(TODAY.minusYears(151)));
    }

    @Test
    public void genderMustBeAKnownOption() {
        DisasterVictim v = victim();
        v.setGender("woman");
        assertEquals("woman", v.getGender());
        v.setGender(null);
        assertNull(v.getGender());
        assertThrows(IllegalArgumentException.class, () -> v.setGender("robot"));
        assertEquals(7, GenderOptions.all().size());
    }

    @Test
    public void missingUnlessAtAShelter() {
        DisasterVictim v = victim();
        assertTrue(v.isMissing());
        v.setLocation(new Location("Retirement village", "1 Oak St", false));
        assertTrue(v.isMissing());
        v.setLocation(new Location("Hospital", "2 Elm St", true));
        assertFalse(v.isMissing());
    }

    @Test
    public void duplicateRecordsAndRestrictionsAreRejected() {
        DisasterVictim v = victim();
        MedicalRecord record = new MedicalRecord(null, "Bandage", TODAY);
        v.addMedicalRecord(record);
        assertThrows(IllegalArgumentException.class, () -> v.addMedicalRecord(new MedicalRecord(null, "Bandage", TODAY)));
        v.addDietaryRestriction(DietaryRestriction.KSML);
        assertThrows(IllegalArgumentException.class, () -> v.addDietaryRestriction(DietaryRestriction.KSML));
        v.removeDietaryRestriction(DietaryRestriction.KSML);
        assertThrows(IllegalArgumentException.class, () -> v.removeDietaryRestriction(DietaryRestriction.KSML));
        assertThrows(UnsupportedOperationException.class, () -> v.getMedicalRecords().clear());
    }
}
