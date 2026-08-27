package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.DietaryRestriction;
import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.service.ReliefRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Loads database/sample/demo_data.sql and checks that every table arrives in the model. */
public class RegistryLoaderTest {
    private static ReliefRegistry registry;

    @BeforeClass
    public static void load() throws Exception {
        Database db = TestDatabase.require();
        TestDatabase.recreateSchema(db);
        TestDatabase.runFile(db, "database/sample/demo_data.sql");
        registry = RegistryLoader.load(db);
    }

    @Test
    public void locationsAndTheirStock() {
        assertEquals(3, registry.getLocations().size());
        Location hospital = registry.requireLocation("Maple Grove Hospital", "123 Main St");
        assertTrue(hospital.isShelter());
        assertEquals(50, hospital.getSupplies().quantity("Splints"));
        assertFalse(registry.requireLocation("Meadowview Retirement Village", "789 Oak Street").isShelter());
    }

    @Test
    public void victimsKeepTheirDatabaseIds() {
        assertEquals(4, registry.getVictims().size());
        DisasterVictim jane = registry.requireVictim(2);
        assertEquals("Jane Doe", jane.getFullName());
        assertEquals("woman", jane.getGender());
        assertEquals(3, jane.getBelongings().quantity("Splints"));
        assertEquals(Set.of(DietaryRestriction.DBML), jane.getDietaryRestrictions());
        assertEquals(1, jane.getMedicalRecords().size());
        assertEquals(5, registry.nextVictimId());
    }

    @Test
    public void bobIsMissingBecauseHisLocationIsNotAShelter() {
        assertEquals("Bob", registry.missingPeople().get(0).getFirstName());
        assertEquals(1, registry.missingPeople().size());
    }

    @Test
    public void relationshipsAndInquiries() {
        assertEquals(2, registry.relationshipsOf(registry.requireVictim(1)).size());
        assertEquals(6, registry.getInquirers().size());
        Inquirer dominik = registry.getInquirers().stream().filter(i -> i.getFirstName().equals("Dominik")).findFirst().orElseThrow();
        assertEquals(3, dominik.getInquiries().size());
    }
}
