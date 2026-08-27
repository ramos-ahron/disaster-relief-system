package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.ParentKind;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
}
