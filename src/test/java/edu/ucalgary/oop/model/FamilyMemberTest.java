package edu.ucalgary.oop.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class FamilyMemberTest {

    private static FamilyMember person(long id) {
        return new FamilyMember(id, "p" + id);
    }

    private static Map<String, Integer> byName(Map<FamilyMember, Integer> found) {
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<FamilyMember, Integer> e : found.entrySet()) {
            out.put(e.getKey().getName(), e.getValue());
        }
        return out;
    }

    @Test
    public void linkAndUnlinkUpdateBothSides() {
        FamilyMember parent = person(1);
        FamilyMember child = person(2);
        FamilyMember.link(parent, child, ParentKind.ADOPTIVE);
        assertEquals(ParentKind.ADOPTIVE, parent.getChildren().get(child));
        assertEquals(ParentKind.ADOPTIVE, child.getParents().get(parent));
        FamilyMember.unlink(parent, child);
        assertTrue(parent.getChildren().isEmpty());
        assertTrue(child.getParents().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.unlink(parent, child));
    }

    @Test
    public void rejectsSelfDuplicateAndThirdBiologicalParent() {
        FamilyMember a = person(1);
        FamilyMember b = person(2);
        FamilyMember c = person(3);
        FamilyMember d = person(4);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(a, a, ParentKind.STEP));

        FamilyMember.link(a, d, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(a, d, ParentKind.ADOPTIVE));

        FamilyMember.link(b, d, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(c, d, ParentKind.BIOLOGICAL));
        FamilyMember.link(c, d, ParentKind.STEP);
        assertEquals(3, d.getParents().size());
    }

    @Test
    public void rejectsDirectAndTransitiveCycles() {
        FamilyMember a = person(1);
        FamilyMember b = person(2);
        FamilyMember c = person(3);
        FamilyMember.link(a, b, ParentKind.BIOLOGICAL);
        FamilyMember.link(b, c, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(b, a, ParentKind.STEP));
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(c, a, ParentKind.STEP));
        assertTrue(c.getChildren().isEmpty());
        assertTrue(a.getParents().isEmpty());
    }

    @Test
    public void sharedAncestorsAppearOnceWithShortestDistance() {
        // g is grandparent of c through two routes
        FamilyMember g = person(1);
        FamilyMember p1 = person(2);
        FamilyMember p2 = person(3);
        FamilyMember c = person(4);
        FamilyMember.link(g, p1, ParentKind.BIOLOGICAL);
        FamilyMember.link(g, p2, ParentKind.ADOPTIVE);
        FamilyMember.link(p1, c, ParentKind.BIOLOGICAL);
        FamilyMember.link(p2, c, ParentKind.STEP);
        Map<String, Integer> up = byName(c.ancestors());
        assertEquals(3, up.size());
        assertEquals(Integer.valueOf(1), up.get("p2"));
        assertEquals(Integer.valueOf(2), up.get("p1"));
        Map<String, Integer> down = byName(g.descendants());
        assertEquals(3, down.size());
        assertEquals(Integer.valueOf(2), down.get("p4"));
    }

    @Test
    public void bothSidesStayConsistentAfterRandomAddAndRemove() {
        Random random = new Random(7);
        List<FamilyMember> all = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            all.add(person(i));
        }
        ParentKind[] kinds = ParentKind.values();
        for (int step = 0; step < 2000; step++) {
            FamilyMember x = all.get(random.nextInt(all.size()));
            FamilyMember y = all.get(random.nextInt(all.size()));
            try {
                if (x.getChildren().containsKey(y) && random.nextBoolean()) {
                    FamilyMember.unlink(x, y);
                } else {
                    FamilyMember.link(x, y, kinds[random.nextInt(kinds.length)]);
                }
            } catch (IllegalArgumentException expected) {
                // rejected operations must leave no trace, checked below
            }
            assertConsistent(all);
        }
    }

    private static void assertConsistent(List<FamilyMember> all) {
        for (FamilyMember m : all) {
            int biological = 0;
            for (Map.Entry<FamilyMember, ParentKind> p : m.getParents().entrySet()) {
                assertEquals(p.getValue(), p.getKey().getChildren().get(m));
                if (p.getValue() == ParentKind.BIOLOGICAL) {
                    biological++;
                }
            }
            for (Map.Entry<FamilyMember, ParentKind> c : m.getChildren().entrySet()) {
                assertEquals(c.getValue(), c.getKey().getParents().get(m));
            }
            assertTrue(biological <= 2);
            assertFalse(m.ancestors().containsKey(m));
        }
    }
}
