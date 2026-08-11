package edu.ucalgary.oop.model;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** The small immutable types and enums. */
public class ValueTypesTest {
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    public void relationshipIsSymmetricAndNeverSelf() {
        DisasterVictim a = new DisasterVictim(1, "A", null, TODAY);
        DisasterVictim b = new DisasterVictim(2, "B", null, TODAY);
        Relationship ab = new Relationship(a, b, RelationshipType.SIBLING);
        Relationship ba = new Relationship(b, a, RelationshipType.SIBLING);
        assertEquals(ab, ba);
        assertEquals(ab.hashCode(), ba.hashCode());
        assertSame(b, ab.otherThan(a));
        assertSame(a, ba.otherThan(b));
        assertThrows(IllegalArgumentException.class, () -> new Relationship(a, a, RelationshipType.FRIEND));
    }

    @Test
    public void inquirerPhoneFormatAndDuplicateCalls() {
        assertThrows(IllegalArgumentException.class, () -> new Inquirer("A", null, "1234567890"));
        assertThrows(IllegalArgumentException.class, () -> new Inquirer("A", null, "123-45a-7890"));
        Inquirer caller = new Inquirer("Dominik", "Pflug", "123-456-9831");
        assertTrue(caller.log(new Inquiry(TODAY, "Theresa Pflug")));
        assertFalse(caller.log(new Inquiry(TODAY, "Theresa Pflug")));
        assertEquals(1, caller.getInquiries().size());
        assertTrue(caller.isSameCaller("dominik", "123-456-9831"));
    }

    @Test
    public void datedRecordsRejectTheFuture() {
        assertThrows(IllegalArgumentException.class, () -> new Inquiry(TODAY.plusDays(1), "x"));
        assertThrows(IllegalArgumentException.class, () -> new MedicalRecord(null, "x", TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> new MedicalRecord(null, " ", TODAY));
        assertThrows(IllegalArgumentException.class, () -> Dates.parse("2024-13-01", "Date"));
        assertEquals(LocalDate.of(2024, 2, 29), Dates.parse(" 2024-02-29 ", "Date"));
    }

    @Test
    public void enumsParseStoredText() {
        assertEquals(DietaryRestriction.AVML, DietaryRestriction.parse("Asian vegetarian meal"));
        assertEquals(DietaryRestriction.AVML, DietaryRestriction.parse("avml"));
        assertEquals(RelationshipType.GUARDIAN_WARD, RelationshipType.parse("Guardian-Ward"));
        assertEquals(RelationshipType.COUPLE, RelationshipType.parse("COUPLE"));
        assertThrows(IllegalArgumentException.class, () -> RelationshipType.parse("Enemy"));
    }
}
