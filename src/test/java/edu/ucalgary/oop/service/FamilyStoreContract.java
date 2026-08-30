package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Behaviour every FamilyStore must have. Subclasses supply a store holding the people
 * G, P1, P2, Other, C, S and X with no links between them.
 */
public abstract class FamilyStoreContract {
    protected FamilyStore store;
    protected final Map<String, Long> id = new HashMap<>();

    /** A store with exactly the seven people named above and no links. */
    protected abstract FamilyStore emptyStore() throws Exception;

    @Before
    public void createStore() throws Exception {
        store = emptyStore();
        id.clear();
        store.persons().forEach(m -> id.put(m.getName(), m.getId()));
        assertEquals(7, id.size());
    }

    protected void link(String parent, String child, ParentKind kind) {
        store.addParent(id.get(parent), id.get(child), kind);
    }

    private String error(Runnable action) {
        return assertThrows(IllegalArgumentException.class, action::run).getMessage();
    }

    private static Map<String, Integer> byName(Map<FamilyMember, Integer> found) {
        Map<String, Integer> out = new HashMap<>();
        found.forEach((m, depth) -> out.put(m.getName(), depth));
        return out;
    }

    @Test
    public void rejectsDuplicateSelfAndUnknownPerson() {
        link("P1", "C", ParentKind.BIOLOGICAL);
        assertEquals("Relation already exists", error(() -> link("P1", "C", ParentKind.ADOPTIVE)));
        assertEquals("A person cannot be their own parent", error(() -> link("C", "C", ParentKind.STEP)));
        assertEquals("Unknown person", error(() -> store.addParent(9999, id.get("C"), ParentKind.STEP)));
    }

    @Test
    public void allowsTwoBiologicalParentsOnly() {
        link("P1", "C", ParentKind.BIOLOGICAL);
        link("P2", "C", ParentKind.BIOLOGICAL);
        assertEquals("Child already has two biological parents", error(() -> link("Other", "C", ParentKind.BIOLOGICAL)));
        link("Other", "C", ParentKind.STEP);
        store.removeParent(id.get("P1"), id.get("C"));   // frees a slot
        link("G", "C", ParentKind.BIOLOGICAL);
        assertEquals("Relation not found", error(() -> store.removeParent(id.get("P1"), id.get("C"))));
    }

    @Test
    public void rejectsCycles() {
        link("G", "P1", ParentKind.BIOLOGICAL);
        link("P1", "C", ParentKind.BIOLOGICAL);
        assertEquals("Relation would create a cycle", error(() -> link("C", "G", ParentKind.STEP)));
        assertEquals("Relation would create a cycle", error(() -> link("P1", "G", ParentKind.STEP)));
    }

    @Test
    public void walksSharedAncestorsOnceWithShortestDistance() {
        // G is parent of P1 and P2; P1, P2 and Other are parents of C; P1 is also parent of S.
        link("G", "P1", ParentKind.BIOLOGICAL);
        link("G", "P2", ParentKind.ADOPTIVE);
        link("P1", "C", ParentKind.BIOLOGICAL);
        link("P2", "C", ParentKind.STEP);
        link("Other", "C", ParentKind.BIOLOGICAL);
        link("P1", "S", ParentKind.BIOLOGICAL);

        Map<String, Integer> up = byName(store.ancestry(id.get("C"), 50).ancestors());
        assertEquals(Map.of("P1", 1, "P2", 1, "Other", 1, "G", 2), up);
        Map<String, Integer> down = byName(store.descendancy(id.get("G"), 50).descendants());
        assertEquals(Map.of("P1", 1, "P2", 1, "C", 2, "S", 2), down);

        assertEquals(3, store.ancestry(id.get("C"), 1).ancestors().size());
        assertTrue(store.ancestry(id.get("X"), 50).ancestors().isEmpty());
        assertEquals("Unknown person", error(() -> store.ancestry(9999, 5)));
    }
}
