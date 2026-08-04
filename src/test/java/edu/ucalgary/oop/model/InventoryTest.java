package edu.ucalgary.oop.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class InventoryTest {

    @Test
    public void addMergesSameType() {
        Inventory inv = new Inventory();
        inv.add("Water", 3);
        inv.add("Water", 2);
        assertEquals(5, inv.quantity("Water"));
        assertEquals(1, inv.asMap().size());
    }

    @Test
    public void removingEverythingDropsTheType() {
        Inventory inv = new Inventory();
        inv.add("Water", 3);
        inv.remove("Water", 3);
        assertTrue(inv.isEmpty());
        assertEquals(0, inv.quantity("Water"));
    }

    @Test
    public void removingTooMuchFailsAndChangesNothing() {
        Inventory inv = new Inventory();
        inv.add("Water", 3);
        assertThrows(IllegalArgumentException.class, () -> inv.remove("Water", 4));
        assertThrows(IllegalArgumentException.class, () -> inv.remove("Blankets", 1));
        assertEquals(3, inv.quantity("Water"));
    }

    @Test
    public void rejectsNonPositiveQuantityAndBlankType() {
        Inventory inv = new Inventory();
        assertThrows(IllegalArgumentException.class, () -> inv.add("Water", 0));
        assertThrows(IllegalArgumentException.class, () -> inv.add("Water", -1));
        assertThrows(IllegalArgumentException.class, () -> inv.add(" ", 1));
        assertTrue(inv.isEmpty());
    }

    @Test
    public void transferMovesStockOrNothing() {
        Inventory shelter = new Inventory();
        Inventory person = new Inventory();
        shelter.add("Splints", 5);
        shelter.transferTo(person, "Splints", 2);
        assertEquals(3, shelter.quantity("Splints"));
        assertEquals(2, person.quantity("Splints"));
        assertThrows(IllegalArgumentException.class, () -> shelter.transferTo(person, "Splints", 9));
        assertEquals(3, shelter.quantity("Splints"));
        assertEquals(2, person.quantity("Splints"));
    }
}
