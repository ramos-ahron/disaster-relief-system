package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.Relationship;
import edu.ucalgary.oop.model.RelationshipType;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Each test gets its own registry: no shared state between tests. */
public class ReliefServicesTest {
    private ReliefRegistry registry;
    private CentralReliefService central;
    private LocationReliefService local;
    private Location hospital;
    private Location village;

    @Before
    public void setUp() {
        registry = new ReliefRegistry();
        central = new CentralReliefService(registry);
        local = new LocationReliefService(registry);
        hospital = central.addLocation("Maple Grove Hospital", "123 Main St", true);
        village = central.addLocation("Meadowview Village", "789 Oak St", false);
    }

    private DisasterVictim register(String first, Location where) {
        return local.register(first, "Doe", LocalDate.now(), where);
    }

    @Test
    public void idsAreHandedOutInOrderAfterLoadedOnes() {
        registry.addVictim(new DisasterVictim(41, "Loaded", null, LocalDate.now()));
        assertEquals(42, register("John", hospital).getId());
        assertEquals(43, register("Jane", hospital).getId());
        assertThrows(IllegalArgumentException.class,
                () -> registry.addVictim(new DisasterVictim(42, "Dup", null, LocalDate.now())));
    }

    @Test
    public void duplicateLocationsAreRejectedIgnoringCase() {
        assertThrows(IllegalArgumentException.class, () -> central.addLocation("maple grove hospital", "123 MAIN ST", false));
        assertSame(hospital, registry.requireLocation("Maple Grove Hospital", "123 Main St"));
    }

    @Test
    public void missingFollowsTheCurrentLocation() {
        DisasterVictim john = register("John", village);
        assertEquals(List.of(john), registry.missingPeople());
        local.move(john, hospital);
        assertTrue(registry.missingPeople().isEmpty());
        assertEquals(List.of(john), registry.occupantsOf(hospital));
        assertThrows(IllegalArgumentException.class, () -> local.move(john, hospital));
    }

    @Test
    public void suppliesMoveFromShelterToVictim() {
        central.addSupplies(hospital, "Water", 10);
        assertThrows(IllegalArgumentException.class, () -> central.addSupplies(village, "Water", 1));
        DisasterVictim john = register("John", hospital);
        local.giveSupplies(john, "Water", 4);
        assertEquals(6, hospital.getSupplies().quantity("Water"));
        assertEquals(4, john.getBelongings().quantity("Water"));
        assertThrows(IllegalArgumentException.class, () -> local.giveSupplies(john, "Water", 7));
        DisasterVictim jane = register("Jane", village);
        assertThrows(IllegalArgumentException.class, () -> local.giveSupplies(jane, "Water", 1));
    }

    @Test
    public void dischargeRemovesTheVictimAndAllTheirRelationships() {
        // The old code threw ConcurrentModificationException here once a victim had relations.
        DisasterVictim john = register("John", hospital);
        DisasterVictim jane = register("Jane", hospital);
        DisasterVictim bob = register("Bob", hospital);
        assertTrue(registry.relate(new Relationship(john, jane, RelationshipType.COUPLE)));
        assertFalse(registry.relate(new Relationship(jane, john, RelationshipType.COUPLE)));
        registry.relate(new Relationship(john, bob, RelationshipType.FRIEND));
        registry.relate(new Relationship(jane, bob, RelationshipType.SIBLING));
        local.discharge(john);
        assertFalse(registry.findVictim(john.getId()).isPresent());
        assertEquals(1, registry.relationshipsOf(jane).size());
        assertTrue(registry.relationshipsOf(john).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> local.discharge(john));
    }

    @Test
    public void treatmentIsRecordedAtTheCurrentLocation() {
        DisasterVictim john = register("John", hospital);
        local.treat(john, "Bandage");
        assertSame(hospital, john.getMedicalRecords().get(0).location());
        assertThrows(IllegalArgumentException.class, () -> local.treat(john, "Bandage"));
    }

    @Test
    public void searchMatchesAnyPartOfTheName() {
        register("John", hospital);
        register("Alice", village);
        assertEquals(2, central.search("doe").size());
        assertEquals(1, central.search("LIC").size());
        assertTrue(central.search("zzz").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> central.search("  "));
    }

    @Test
    public void repeatCallersShareOneLog() {
        Inquirer first = central.logInquiry("Yaa", "Odei", "123-456-8913", "Looking for Kofi");
        Inquirer again = central.logInquiry("yaa", "", "123-456-8913", "Looking for Ama");
        assertSame(first, again);
        assertEquals(2, first.getInquiries().size());
        central.logInquiry("Yaa", "Odei", "123-456-8913", "Looking for Ama");
        assertEquals(2, first.getInquiries().size());
        assertEquals(1, registry.getInquirers().size());
    }
}
